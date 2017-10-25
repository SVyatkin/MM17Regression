package com.ge.predix.labs.data.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.web.context.support.StandardServletEnvironment;

import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication 
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    
    static String[] profiles ;

	public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(Application.class);
        ApplicationContext ctx = springApplication.run(args);
        
        log.debug("Let's inspect the beans provided by Spring Boot:"); //$NON-NLS-1$
        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames)
        {
            log.debug(beanName);
        }

        log.debug("Let's inspect the profiles provided by Spring Boot:"); //$NON-NLS-1$
        profiles = ctx.getEnvironment().getActiveProfiles();
        for (int i = 0; i < profiles.length; i++)
            log.debug("profile****=" + profiles[i]); //$NON-NLS-1$

        log.info("Let's inspect the properties provided by Spring Boot:"); //$NON-NLS-1$
        MutablePropertySources propertySources = ((StandardServletEnvironment) ctx.getEnvironment())
                .getPropertySources();
        Iterator<org.springframework.core.env.PropertySource<?>> iterator = propertySources.iterator();
        while (iterator.hasNext())
        {
            Object propertySourceObject = iterator.next();
            if ( propertySourceObject instanceof org.springframework.core.env.PropertySource )
            {
                org.springframework.core.env.PropertySource<?> propertySource = (org.springframework.core.env.PropertySource<?>) propertySourceObject;
                log.info("propertySource=" + propertySource.getName() + " values=" + propertySource.getSource() //$NON-NLS-1$ //$NON-NLS-2$
                        + "class=" + propertySource.getClass()); //$NON-NLS-1$
            }
        }
    }
}
