package org.mule.modules.hornetq;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hornetq.api.core.HornetQBuffer;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.Message;
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
import org.mule.transport.NullPayload;
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
        
            ClientMessage msg = writeBodyToMessage(event.getMessage().getPayload(),clientSession);
            
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
    
    protected ClientMessage writeBodyToMessage(InputStream payload, ClientMessage message) throws IOException
    {        
        return writeBodyToMessage(IOUtils.toByteArray(payload), message);
    }
    
    protected ClientMessage writeBodyToMessage(byte[] payload, ClientMessage message)
    {
        message.getBodyBuffer().writeBytes(payload, 0, payload.length);
        
        return message;
    }
    
    protected ClientMessage writeBodyToMessage(CharSequence payload, ClientMessage message)
    {
        message.getBodyBuffer().resetReaderIndex();
        
        HornetQBuffer buff = message.getBodyBuffer();
        buff.clear();
        
        buff.writeString(payload.toString());

        return message;
        
    }
    
    protected ClientMessage writeBodyToMessage(Object payload, ClientSession session) throws IOException
    {
        if(payload instanceof CharSequence)
        {
            ClientMessage msg = session.createMessage(Message.TEXT_TYPE, true);
            return writeBodyToMessage((CharSequence)payload, msg);
        } else if(payload instanceof byte[])
        {
            ClientMessage msg = session.createMessage(Message.BYTES_TYPE, true);
            return writeBodyToMessage((byte[])payload, msg);
        } else if(payload instanceof InputStream)
        {
            ClientMessage msg = session.createMessage(Message.BYTES_TYPE, true);
            return writeBodyToMessage((InputStream)payload, msg);
        } else if(payload instanceof Serializable)
        {
            ClientMessage msg = session.createMessage(Message.OBJECT_TYPE, true);
            return writeBodyToMessage((Serializable)payload, msg);
        } else
        {
            throw new IllegalArgumentException("Must be CharSequence, byte[], InputStream or Serializable");
        }
    }
    
    protected ClientMessage writeBodyToMessage(Serializable payload, ClientMessage message) throws IOException
    {

        if (payload != null && !(payload instanceof NullPayload))
        {

            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);

            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(payload);

            oos.flush();
            
            message.getBodyBuffer().writeInt(baos.size());

            return writeBodyToMessage(baos.toByteArray(), message);
        } else
        {
            return writeBodyToMessage(new byte[0], message);
        }

    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.expressionManager = context.getExpressionManager();
    }
    
    public void setAddress(String addressExpression)
    {
        if(null == addressExpression) throw new IllegalArgumentException("addressExpression can't be null");
        this.addressExpression = addressExpression;
    }
    
    public void setHeaders(Map<String,String> expressions)
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
