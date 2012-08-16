package org.mule.modules.hornetq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.Message;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.MessageHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.tck.FunctionalTestCase;

public class HornetQMessageProcessorTestCase extends FunctionalTestCase
{
    
    ClientConsumer c;
    ClientSession session;
    
    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }
    
    @Override
    protected void doSetUp() throws Exception
    {
        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
        session = csf.createSession();
        if(!(session.queueQuery(SimpleString.toSimpleString("sailthru.send")).isExists()))
        {
            session.createQueue("app.test", "sailthru.send");
        }
        c = session.createConsumer("sailthru.send");
        
        session.start();
    }
    
    @Override
    protected void doTearDown() throws Exception
    {
        session.stop();
    }

    @Test
    public void testSend() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSend");
        MuleEvent event = getTestEvent("hello");
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals("hello",msg.getBodyBuffer().readString());
    }

    @Test
    public void testSendByteArray() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSend");
        MuleEvent event = getTestEvent("hello".getBytes());
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals(Message.BYTES_TYPE,msg.getType());
        assertEquals("hello".getBytes().length,msg.getBodyBuffer().readableBytes());
        byte[] body = new byte[msg.getBodySize()];
        msg.getBodyBuffer().readBytes(body);
        Assert.assertArrayEquals("hello".getBytes(), body);
    }

    @Test
    public void testSendInputstream() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSend");
        MuleEvent event = getTestEvent(new ByteArrayInputStream("hello".getBytes()));
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals(Message.BYTES_TYPE,msg.getType());
        assertEquals("hello".getBytes().length,msg.getBodyBuffer().readableBytes());
        byte[] body = new byte[msg.getBodySize()];
        msg.getBodyBuffer().readBytes(body);
        Assert.assertArrayEquals("hello".getBytes(), body);
    }

    @Test
    public void testSendObject() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSend");
        MuleEvent event = getTestEvent(new Integer(5));
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals(Message.OBJECT_TYPE,msg.getType());
        msg.getBodyBuffer().resetReaderIndex();
        int len = msg.getBodyBuffer().readInt();
        byte[] data = new byte[len];
        msg.getBodyBuffer().readBytes(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new org.hornetq.utils.ObjectInputStreamWithClassLoader(bais);
        Serializable object = (Serializable)ois.readObject();
        ois.close();
        Assert.assertEquals(object, new Integer(5));
    }


    @Test
    public void testSendWithProperties() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSend");
        MuleEvent event = getTestEvent("hello");
        event.getMessage().setOutboundProperty("TEST", "hello");
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals("hello",msg.getBodyBuffer().readString());
        assertEquals("hello",msg.getStringProperty("TEST"));
    }


    @Test
    public void testSendWithCorrelationId() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSend");
        MuleEvent event = getTestEvent("hello");
        event.getMessage().setCorrelationId("BOB");
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals("hello",msg.getBodyBuffer().readString());
        assertEquals("BOB",msg.getStringProperty(HornetQMessageProcessor.JMS_CORRELATION_ID));
    }


    @Test
    public void testSendWithExpressionAddress() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSendWithAddressExpression");
        MuleEvent event = getTestEvent("hello");
        event.getMessage().addProperties(Collections.singletonMap("address",(Object) "app.test"), PropertyScope.INBOUND);
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals("hello",msg.getBodyBuffer().readString());
    }


    @Test
    public void testSendWithExpressionHeaders() throws Exception
    {
        Flow flow = muleContext.getRegistry().get("testSendWithHeaderExpression");
        MuleEvent event = getTestEvent("hello");
        event.getMessage().addProperties(Collections.singletonMap("address",(Object) "app.test"), PropertyScope.INBOUND);
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals("hello",msg.getBodyBuffer().readString());
        assertEquals(0L,msg.getObjectProperty("TEST"));
    }
}
