package org.mule.modules.hornetq;

public interface Queue
{

    public String getAddress();

    public String getQueue();

    public boolean isDurable();

    public String getFilter();

}