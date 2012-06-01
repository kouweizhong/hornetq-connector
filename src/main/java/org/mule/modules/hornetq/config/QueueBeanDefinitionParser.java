package org.mule.modules.hornetq.config;

import org.mule.modules.hornetq.impl.QueueInternalImpl;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.w3c.dom.Element;

public class QueueBeanDefinitionParser extends AbstractSimpleBeanDefinitionParser 
{

    protected Class getBeanClass(Element element) 
    {
        return QueueInternalImpl.class;
    }
}
