package org.mule.modules.hornetq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HornetQMessageProcessor implements MessageProcessor, MuleContextAware
{

    public static final String JMS_CORRELATION_ID = "JMSCorrelationID";
    
    private transient Logger logger = LoggerFactory.getLogger(getClass());
    
    private ClientProducer producer;
    private ClientSession clientSession;
    private String addressExpression;
    private Map<String,String> headerExpressions = Collections.emptyMap();
    private ExpressionManager expressionManager;
    
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            String address2Use = expressionManager.parse(addressExpression, event.getMessage());

            ClientMessage msg = clientSession.createMessage(true);
            
            if(StringUtils.isNotBlank(event.getMessage().getCorrelationId()))
            {
                msg.putStringProperty(JMS_CORRELATION_ID, event.getMessage().getCorrelationId());
            }
            
            for(Map.Entry<String, String> e: headerExpressions.entrySet())
            {
                Object value2Use = expressionManager.evaluate(e.getValue(), event.getMessage());
                msg.putObjectProperty(e.getKey(), value2Use);
            }
            
            for(String prop :event.getMessage().getOutboundPropertyNames())
            {
                msg.putObjectProperty(prop, event.getMessage().getOutboundProperty(prop));
            }
            
            msg.getBodyBuffer().writeString(event.getMessage().getPayloadAsString());
            producer.send(address2Use, msg);
            
            Map<String,Object> props = new HashMap<String,Object>();
            props.put("hornetq.address", address2Use);
            
            event.getMessage().addProperties(props, PropertyScope.INBOUND);
            
            return event;
            
        } catch (Exception e)
        {
            throw new MessagingException(event,e);
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.expressionManager = context.getExpressionManager();
    }
    
    public void setAddressExpression(String addressExpression)
    {
        if(null == addressExpression) throw new IllegalArgumentException("addressExpression can't be null");
        this.addressExpression = addressExpression;
    }
    
    public void setHeaderExpressions(Map<String,String> expressions)
    {
        if(null == expressions) throw new IllegalArgumentException("expressions can't be null");
        this.headerExpressions = expressions;
    }
    
    public void setClientSession(ClientSession clientSession) throws HornetQException
    {
        this.clientSession = clientSession;
        
        this.setProducer(clientSession.createProducer());
    }
    
    public void setProducer(ClientProducer producer)
    {
        this.producer = producer;
    }
}
