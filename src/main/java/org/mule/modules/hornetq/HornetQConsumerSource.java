package org.mule.modules.hornetq;

import org.hornetq.api.core.HornetQException;
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


public class HornetQConsumerSource implements MessageSource, FlowConstructAware, MuleContextAware, Startable,Stoppable
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
            QueueQuery qq = clientSession.queueQuery(SimpleString.toSimpleString(queue.getQueue()));
            if (!qq.isExists())
            {
                logger.info("Creating {}",queue);
                clientSession.createQueue(queue.getAddress(), queue.getQueue(), queue.getFilter(),queue.isDurable());
            }
            for(int i=0;i<consumerCount;i++)
            {
                ClientConsumer consumer = createConsumer(queue.getQueue(), this.filter);
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
                    muleMessage = new DefaultMuleMessage(message.getBodyBuffer().readString(),message.toMap(), null, null, muleContext);
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
                        clientSession.rollback();
                    } catch (HornetQException e1)
                    {
                        logger.error("Something bad happened while rollback {}",e1);
                    }
                }
            }
        });
        
        return consumer;
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
