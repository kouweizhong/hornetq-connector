package org.mule.modules.hornetq.impl;

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.mule.modules.hornetq.Queue;

public class QueueInternalImpl implements Queue
{
    private String address;
    private String name = UUID.randomUUID().toString();
    private boolean durable = true;
    private String filter;
    
    private boolean constructed = false;
    
    public QueueInternalImpl()
    {
    }
    
    public QueueInternalImpl(String address)
    {
        this(address,UUID.randomUUID().toString(),true,"");
    }
    
    public QueueInternalImpl(String address,String queue)
    {
        this(address,queue,true,"");
    }

    public QueueInternalImpl(String address, String queue, boolean durable, String filter)
    {
        super();
        this.address = address;
        this.name = null == queue?UUID.randomUUID().toString():queue;
        this.durable = durable;
        this.filter = filter;
    }

    /* (non-Javadoc)
     * @see org.mule.modules.hornetq.impl.Queue#getAddress()
     */
    @Override
    public String getAddress()
    {
        return address;
    }
    
    public void setAddress(String address)
    {
        this.address = address;
    }

    /* (non-Javadoc)
     * @see org.mule.modules.hornetq.impl.Queue#getQueue()
     */
    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String queue)
    {
        this.name = queue;
    }
    
    /* (non-Javadoc)
     * @see org.mule.modules.hornetq.impl.Queue#isDurable()
     */
    @Override
    public boolean isDurable()
    {
        return durable;
    }
    
    public void setDurable(boolean durable)
    {
        this.durable = durable;
    }
    
    /* (non-Javadoc)
     * @see org.mule.modules.hornetq.impl.Queue#getFilter()
     */
    @Override
    public String getFilter()
    {
        return filter;
    }
    
    public void setFilter(String filter)
    {
        this.filter = filter;
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
                name, durable, filter);
    }
    
    
}
