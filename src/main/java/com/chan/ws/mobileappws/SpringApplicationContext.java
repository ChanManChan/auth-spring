package com.chan.ws.mobileappws;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

// ApplicationContext that we use to access beans which were created by spring framework
public class SpringApplicationContext implements ApplicationContextAware {
    private static ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CONTEXT = applicationContext;
    }
    //  When spring framework creates UserServiceImpl bean for us, it will be available to us from this
    //  application context. And we can get access to that UserServiceImpl from anywhere in our application
    //  if we have access to this SpringApplicationContext object.
    public static Object getBean(String beanName) {
        return CONTEXT.getBean(beanName);
    }
}
