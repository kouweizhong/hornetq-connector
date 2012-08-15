package org.mule.modules.hornetq;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.Message;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.MessageHandler;
import org.hornetq.api.core.client.ClientSession.QueueQuery;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.session.DefaultMuleSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HornetQConsumerSource implements MessageSource, FlowConstructAware, MuleContextAware, Startable, Stoppable
{
    private transient Logger logger = LoggerFactory.getLogger(getClass());

    private MessageProcessor listener;
    
    private Queue queue;
    private Integer consumerCount = 1;
    private String filter = "";
    private ClientSession clientSession;
    private FlowConstruct flowConstruct;
    private MuleContext muleContext;
    
    @Override
    public void start() throws MuleException
    {
        try
        {
            QueueQuery qq = clientSession.queueQuery(SimpleString.toSimpleString(queue.getName()));
            if (!qq.isExists())
            {
                logger.info("Creating {}",queue);
                clientSession.createQueue(queue.getAddress(), queue.getName(), queue.getFilter(),queue.isDurable());
            }
            for(int i=0;i<consumerCount;i++)
            {
                ClientConsumer consumer = createConsumer(queue.getName(), this.filter);
            }
        } catch (HornetQException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    protected ClientConsumer createConsumer(String queue, String filter) throws HornetQException
    {
        ClientConsumer consumer = clientSession.createConsumer(queue, filter);
        consumer.setMessageHandler(new MessageHandler()
        {

            @Override
            public void onMessage(ClientMessage message)
            {
                try
                {
                    logger.debug("Got a message");
                    message.acknowledge();
                    MuleMessage muleMessage;
                    Map<String,Object> props = cleanMapOfSimpleString(message.toMap());
                    muleMessage = new DefaultMuleMessage(readMessageBody(message),props, null, null, muleContext);
                    MuleSession muleSession;
                    muleSession = new DefaultMuleSession(flowConstruct, muleContext);
                    MuleEvent muleEvent;
                    muleEvent = new DefaultMuleEvent(muleMessage, MessageExchangePattern.ONE_WAY, muleSession);
                    logger.debug("Created muleEvent {}",muleEvent);
                    MuleEvent responseEvent;
                    responseEvent = listener.process(muleEvent);
                    clientSession.commit();
                } catch (Exception e)
                {
                    try
                    {
                        logger.info("Rolling back");
                        logger.error("SOmehing bad",e);
                        clientSession.rollback();
                        throw new RuntimeException(e);
                    } catch (HornetQException e1)
                    {
                        logger.error("Something bad happened while rollback {}",e1);
                    }
                }
            }
        });
        
        return consumer;
    }
    
    protected Object readMessageBody(ClientMessage message) throws IOException, ClassNotFoundException
    {
        switch(message.getType())
        {
            case Message.TEXT_TYPE:
                return message.getBodyBuffer().readString();
            case Message.OBJECT_TYPE:
                ByteArrayInputStream bais = new ByteArrayInputStream(readBytes(message.getBodyBuffer(), message.getBodyBuffer().readInt()));
                ObjectInputStream ois = new org.hornetq.utils.ObjectInputStreamWithClassLoader(bais);
                ois.close();
                return (Serializable)ois.readObject();
            case Message.BYTES_TYPE:
                return readBytes(message.getBodyBuffer(), message.getBodySize());
            default:
                throw new IllegalArgumentException("Unsupported type "+message.getType());
        }
        
    }
    
    protected byte[] readBytes(HornetQBuffer buffer, int size)
    {
        byte[] data = new byte[size];
        buffer.readBytes(data);
        
        return data;
    }
    
    protected Map<String,Object> cleanMapOfSimpleString(Map<String,Object> m)
    {
        Map<String,Object> cleanMap = new LinkedHashMap<String,Object>(m.size());
        for(Map.Entry<String, Object> e: m.entrySet())
        {
            if(e.getValue() instanceof SimpleString)
            {
                cleanMap.put(e.getKey(), e.getValue().toString());
            } else if(e.getValue() instanceof Map)
            {
                cleanMap.put(e.getKey(), cleanMapOfSimpleString((Map)e.getValue()));
            } else if(e.getValue() instanceof Collection)
            {
                cleanMap.put(e.getKey(), cleanCollectionOfSimpleString((Collection)e.getValue()));
            } else
            {
                cleanMap.put(e.getKey(), e.getValue());
            }
        }
        
        return cleanMap;
    }
    
    protected Collection cleanCollectionOfSimpleString(Collection dirty)
    {
        Collection clean;
        try
        {
            clean = dirty.getClass().newInstance();
        } catch (InstantiationException e)
        {
            clean = new LinkedList();
        } catch (IllegalAccessException e)
        {
            clean = new LinkedList();
        }
        for(Object o: dirty)
        {
            if(o instanceof SimpleString)
            {
                clean.add(o.toString());
            } else
            {
                clean.add(o);
            }
        }
        
        return clean;
    }
    
    @Override
    public void stop() throws MuleException
    {

    }
    
    @Override
    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }
    
    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
    
    public void setQueue(Queue queue)
    {
        this.queue = queue;
    }
    
    public void setClientSession(ClientSession clientSession)
    {
        this.clientSession = clientSession;
    }
    
    public void setConsumerCount(Integer consumerCount)
    {
        this.consumerCount = consumerCount;
    }
    
    public void setFilter(String filter)
    {
        this.filter = filter;
    }
}
