package org.mule.modules.hornetq;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.MessageHandler;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.tck.FunctionalTestCase;

public class HornetqModuleTest extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
        ClientSession s1 = csf.createSession();
        s1.createQueue("app.test", "sailthru.send");
        ClientConsumer c = s1.createConsumer("sailthru.send");
        s1.start();
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
        final CountDownLatch latch = new CountDownLatch(1);
        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
        ClientSession s1 = csf.createSession();
        s1.createQueue("app.test", "sailthru.send");
        ClientConsumer c = s1.createConsumer("sailthru.send");
        s1.start();
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
        final CountDownLatch latch = new CountDownLatch(1);
        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
        ClientSession s1 = csf.createSession();
        s1.createQueue("app.test", "sailthru.send");
        ClientConsumer c = s1.createConsumer("sailthru.send");
        s1.start();
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
        final CountDownLatch latch = new CountDownLatch(1);
        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
        ClientSession s1 = csf.createSession();
        s1.createQueue("app.test", "sailthru.send");
        ClientConsumer c = s1.createConsumer("sailthru.send");
        s1.start();
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
        final CountDownLatch latch = new CountDownLatch(1);
        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
        ClientSession s1 = csf.createSession();
        s1.createQueue("app.test", "sailthru.send");
        ClientConsumer c = s1.createConsumer("sailthru.send");
        s1.start();
        Flow flow = muleContext.getRegistry().get("testSendWithHeaderExpression");
        MuleEvent event = getTestEvent("hello");
        event.getMessage().addProperties(Collections.singletonMap("address",(Object) "app.test"), PropertyScope.INBOUND);
        flow.process(event);
        ClientMessage msg = c.receive();
        msg.acknowledge();
        assertEquals("hello",msg.getBodyBuffer().readString());
        assertEquals(0L,msg.getObjectProperty("TEST"));
    }
    
    @Test
    public void testConsume() throws Exception
    {
        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
        ClientSession session = csf.createSession();

        ClientMessage msg = session.createMessage(true);
        msg.getBodyBuffer().writeString("hello, world");
        Thread.sleep(5*1000);
        session.createProducer().send("app.test", msg);
        Thread.sleep(20*1000);
    }
}
