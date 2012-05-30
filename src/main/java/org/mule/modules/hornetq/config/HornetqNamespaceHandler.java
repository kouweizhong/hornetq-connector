package org.mule.modules.hornetq.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.modules.hornetq.HornetQConsumerSource;
import org.mule.modules.hornetq.HornetQMessageProcessor;

public class HornetqNamespaceHandler extends AbstractMuleNamespaceHandler
{
    @Override
    public void init()
    {
        registerMuleBeanDefinitionParser("send", new MessageProcessorDefinitionParser(HornetQMessageProcessor.class));
        registerMuleBeanDefinitionParser("consume", new ChildDefinitionParser("messageSource", null, HornetQConsumerSource.class));
    }
}
