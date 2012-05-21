package org.mule.modules.hornetq;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSession.QueueQuery;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.MessageHandler;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
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
     * @param queue The name of the queue to consume from
     * @param address The addrss to use if creating a queue
     * @param create Boolean to not create a queue
     * @param durable Should the queue be durable
     * @param consumerCount The number of consumers to create
     * @param callback The callback to use
     * @throws HornetQException
     */
    @Source
    public void consume(String queue, @Optional String address, @Optional @Default(value="true") boolean create, @Optional @Default(value="true") boolean durable, @Optional @Default(value="1") Integer consumerCount, final SourceCallback callback) throws HornetQException
    {
        QueueQuery qq = clientSession.queueQuery(SimpleString.toSimpleString(queue));
        if (!qq.isExists())
        {
            if (create)
            {
                logger.info("Creating queue {} with address {}",queue,address);
                clientSession.createQueue(address, queue, durable);
            } else
            {
                throw new RuntimeException(
                        "queue doesn't exist and you don't want to make it");
            }
        }
        final Collection<ClientConsumer> localConsumers = new HashSet<ClientConsumer>(consumerCount);
        for (int i = 0; i < consumerCount; i++)
        {
            ClientConsumer consumer = createConsumer(queue, callback);
            localConsumers.add(consumer);
        }
        
        this.consumers.put(queue, localConsumers);
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
                    callback.process(message.getBodyBuffer().readString(),
                            message.toMap());
                    message.acknowledge();
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
        
        return consumer;
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
