package org.mule.modules.hornetq;

import java.util.UUID;

import javax.annotation.PostConstruct;

public class Queue
{
    private String address;
    private String queue;
    private boolean durable;
    private String filter;
    
    private boolean constructed = false;
    
    public Queue(String address)
    {
        this(address,UUID.randomUUID().toString(),true,"");
    }



    public Queue(String address, String queue, boolean durable, String filter)
    {
        super();
        this.address = address;
        this.queue = null == queue?UUID.randomUUID().toString():queue;
        this.durable = durable;
        this.filter = filter;
    }

    public String getAddress()
    {
        return address;
    }

    public String getQueue()
    {
        return queue;
    }

    public boolean isDurable()
    {
        return durable;
    }
    
    public String getFilter()
    {
        return filter;
    }
    
    @PostConstruct
    private void postConstruct()
    {
        this.constructed = true;
    }

    @Override
    public String toString()
    {
        return String.format(
                "Queue [address=%s, queue=%s, durable=%s, filter=%s]", address,
                queue, durable, filter);
    }
    
    
}
