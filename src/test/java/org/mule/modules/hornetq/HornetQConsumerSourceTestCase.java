package org.mule.modules.hornetq;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
        if(!(session.queueQuery(SimpleString.toSimpleString(q.getQueue())).isExists()))
        {
            session.createQueue(q.getAddress(), q.getQueue());
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
        ClientMessage msg = session.createMessage(true);
        msg.getBodyBuffer().writeString("hello, world");
        producer.send("app.test2", msg);
        l.await();
        
        assertEquals(1,ai.get());
    }
    
    @Test
    public void testConsumeWithProperties() throws Exception
    {

        CountDownLatch l = muleContext.getRegistry().get("countDownLatch1");
        
        ClientMessage msg = session.createMessage(true);
        msg.putObjectProperty("TEST", "hello");
        msg.getBodyBuffer().writeString("hello, world");
        producer.send("app.test2", msg);
        
        l.await();
    }
}
