package org.mule.modules.hornetq;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.MessageHandler;
import org.junit.After;
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
