package org.mule.modules.hornetq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSession.QueueQuery;
import org.hornetq.api.core.client.MessageHandler;
import org.mule.api.MuleContext;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.api.context.MuleContextAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects to Hornetq via the Hornetq Core api
 * @author craig
 *
 */
@Module(name = "hornetq", schemaVersion = "1.0", poolable=false)
public class HornetqModule
{
    
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * The client session factory to use
     */
    @Configurable
    private ClientSession clientSession;
    
    private Map<String,Collection<ClientConsumer>> consumers;
    
    private ClientProducer producer;
    
    /**
     * Called by the container
     * @throws Exception
     */
    @PostConstruct
    public void connect() throws Exception
    {   
        producer = clientSession.createProducer();
        
        consumers = new ConcurrentHashMap<String, Collection<ClientConsumer>>();
    }
    
    public void setClientSession(ClientSession clientSession)
    {
        this.clientSession = clientSession;
    }
    
    /**
     * Consumes messages from a given queue, creating it and uses the given address if needed.
     * 
     * {@sample.xml ../../../doc/Hornetq-connector.xml.sample hornetq:consume}
     * 
     * @param address The address
     * @param queue the queue 
     * @param durable queue is durable
     * @param queueFilter the filter for the queue
     * @param filter The filter to use
     * @param callback The callback to use
     * @param consumerCount the count
     * @param temporary is the queue temp
     * @throws HornetQException
     */
    @Source
    public void consume(String address, @Optional() String queue, @Optional @Default(value="true") boolean durable, @Optional @Default(value="false") boolean temporary, @Optional String queueFilter, @Optional @Default(value="1") Integer consumerCount, @Optional @Default(value="") String filter, final SourceCallback callback) throws HornetQException
    //public void consume(String queue, @Optional @Default(value="1") Integer consumerCount, @Optional @Default(value="") String filter, final SourceCallback callback) throws HornetQException
    {
        Queue q = new Queue(address,queue,durable,queueFilter);
        QueueQuery qq = clientSession.queueQuery(SimpleString.toSimpleString(q.getQueue()));
        if (!qq.isExists())
        {
            logger.info("Creating {}",q);
            clientSession.createQueue(q.getAddress(), q.getQueue(), q.getFilter(),q.isDurable());
        }
        final Collection<ClientConsumer> localConsumers = new HashSet<ClientConsumer>(consumerCount);
        for (int i = 0; i < consumerCount; i++)
        {
            ClientConsumer consumer = createConsumer(q.getQueue(), callback);
            localConsumers.add(consumer);
        }
        
        this.consumers.put(q.getQueue(), localConsumers);
    }
    
    protected ClientConsumer createConsumer(String queue, final SourceCallback callback) throws HornetQException
    {
        ClientConsumer consumer = clientSession.createConsumer(queue);
        consumer.setMessageHandler(new MessageHandler()
        {

            @Override
            public void onMessage(ClientMessage message)
            {
                try
                {
                    Object r = callback.process(message.getBodyBuffer().readString(),message.toMap());
                    message.acknowledge();
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
        
        return consumer;
    }
    
    //@Processor
    public Collection<String> pop(String queue, @Optional String address, @Optional @Default(value="true") boolean create, @Optional @Default(value="true") boolean durable, @Optional @Default(value="1") Integer count) throws HornetQException
    {
        if(!this.consumers.containsKey(queue))
        {
            final Collection<ClientConsumer> localConsumers = Collections.singleton(clientSession.createConsumer(queue));
            
            this.consumers.put(queue, localConsumers);
        }
        Collection<String> messages = new ArrayList<String>(count);
        for(ClientConsumer c: this.consumers.get(queue))
        {
            for(int i=0;i<count;i++)
            {
                ClientMessage msg = c.receive();
                msg.acknowledge();
                messages.add(msg.getBodyBuffer().readString());
            }
        }
        
        return messages;
    }
    
    /**
     * Sends a text message
     * 
     * {@sample.xml ../../../doc/Hornetq-connector.xml.sample hornetq:send}
     * 
     * @param address The address to send to
     * @param body The Text messages to send
     * @return The original body
     * @throws HornetQException
     */
    @Processor
    public String send(String address, @Optional @Default(value="#[payload:]") String body) throws HornetQException
    {
        logger.info("Sending {} to {}",body,address);
        ClientMessage msg = clientSession.createMessage(true);
        msg.getBodyBuffer().writeString(body);
        producer.send(address, msg);
        return body;
    }
}
