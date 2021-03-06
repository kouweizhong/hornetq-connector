package org.mule.modules.hornetq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.hornetq.api.core.Message;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.junit.Test;
import org.mule.tck.FunctionalTestCase;

public class HornetQConsumerSourceTestCase extends FunctionalTestCase
{
    
    ClientProducer producer;
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
        Queue q = muleContext.getRegistry().get("testq");
        session = csf.createSession();
        if(!(session.queueQuery(SimpleString.toSimpleString(q.getName())).isExists()))
        {
            session.createQueue(q.getAddress(), q.getName());
        }
        producer = session.createProducer();
        
        session.start();
        
        Thread.sleep(1*1000);
    }
    
    @Test
    public void testConsume() throws Exception
    {

        CountDownLatch l = muleContext.getRegistry().get("countDownLatch1");
        AtomicInteger ai = muleContext.getRegistry().get("atomicInt1");
        ClientMessage msg = session.createMessage(Message.TEXT_TYPE, true);
        msg.getBodyBuffer().writeString("hello, world");
        producer.send("app.test2", msg);
        l.await();
        
        assertEquals(1,ai.get());
    }
    
    @Test
    public void testConsumeMap() throws Exception
    {

        CountDownLatch l = muleContext.getRegistry().get("countDownLatch1");
        AtomicInteger ai = muleContext.getRegistry().get("atomicInt1");
        ClientMessage msg = session.createMessage(Message.TEXT_TYPE, true);
        
        Map<String,Object> data = new HashMap<String, Object>();
        data.put("a", 1);
        data.put("b",2);
        data.put("c",3);
        
        msg.getBodyBuffer().writeString("hello, world");
        producer.send("app.test2", msg);
        l.await();
        
        assertEquals(1,ai.get());
    }
    
    @Test
    public void testConsumeWithProperties() throws Exception
    {

        CountDownLatch l = muleContext.getRegistry().get("countDownLatch1");
        CountDownLatch l2 = muleContext.getRegistry().get("countDownLatch2");
        
        ClientMessage msg = session.createMessage(Message.TEXT_TYPE, true);
        msg.putObjectProperty("TEST", "hello");
        msg.getBodyBuffer().writeString("hello, world");
        producer.send("app.test2", msg);
        
        l.await();
        
        assertEquals(1,l2.getCount());
    }
    
    @Test
    public void testConsumeWithFilter() throws Exception
    {

        CountDownLatch l = muleContext.getRegistry().get("countDownLatch1");
        CountDownLatch l2 = muleContext.getRegistry().get("countDownLatch2");
        AtomicInteger ai = muleContext.getRegistry().get("atomicInt1");
        ClientMessage msg = session.createMessage(Message.TEXT_TYPE, true);
        msg.putObjectProperty("TEST", "hello");
        msg.putObjectProperty("EVENT_TYPE", "TEST");
        msg.getBodyBuffer().writeString("hello, world");
        producer.send("app.test2", msg);
        
        l.await();
        l2.await();
    }
    
    @Test
    public void testConsumeCount() throws Exception
    {

        Queue q = muleContext.getRegistry().get("testq3");
        int count = session.queueQuery(SimpleString.toSimpleString(q.getName())).getConsumerCount();
        assertEquals(5,count);
    }
}
