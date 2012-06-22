package org.mule.modules.hornetq;

public interface Queue
{

    public String getAddress();

    public String getName();

    public boolean isDurable();

    public String getFilter();

}