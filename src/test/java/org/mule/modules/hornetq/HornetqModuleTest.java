package org.mule.modules.hornetq;

import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.FunctionalTestCase;

public class HornetqModuleTest extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "mule-config.xml";
    }

//    @Test
//    public void testSend() throws Exception
//    {
//        ClientSessionFactory csf = muleContext.getRegistry().get("hornetq.clientSessionFactory");
//        Flow flow = muleContext.getRegistry().get("testSend");
//        MuleEvent event = getTestEvent("hello");
//        flow.process(event);
//        Thread.sleep(20*1000);
//    }

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
