package com.kush.newrelic.util;

import com.kush.newrelic.server.TCPServer;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by root on 7/29/17.
 */
@Component
public class PrintReport implements CommandLineRunner,InitializingBean,ApplicationContextAware {

    
    @Autowired
    ConcurrentSkipListSet<String> uniqueValues;

    @Autowired
    ApplicationListener<TcpConnectionOpenEvent> listener;

    @Autowired
    private ConfigurableApplicationContext appContext;

    @Autowired
    MessageChannel tcpIn;
    
    @Autowired
    AtomicInteger currentUniqueCount;

    @Autowired
    AtomicInteger currentDuplicateCount;

    @Autowired
    AtomicInteger totalUniqueCount;

    @Autowired
    AtomicInteger totalDuplicateCount;


    @Override
    public void afterPropertiesSet() throws Exception {
        appContext.addApplicationListener(listener);
    }

    @Override
    public void run(String... args) throws Exception {


        System.out.println("Starting up the TCP server.....\n"
                        +"Line lineSeparator:                "+
                        StringEscapeUtils.escapeJava(ValidateInput.NEW_LINE)+"\n");

    }

    //Print report after every 10 seconds and reset the counters.
    @Scheduled(fixedRate = 10000)
    public void report() {
        StringBuilder sb = new StringBuilder();
        sb.append("Received ");
        sb.append(currentUniqueCount);
        sb.append(" unique numbers, ");
        sb.append(currentDuplicateCount);
        sb.append(" duplicates. Unique total: ");
        sb.append(uniqueValues.size());

        System.out.println(sb.toString());

        resetCounters();
    }

    public void resetCounters() {
        currentUniqueCount.set(0);
        currentDuplicateCount.set(0);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {}

    private void shutDownCommand() {
        tcpIn.send(MessageBuilder.withPayload(TCPServer.SHUT_DOWN_COMMAND + ValidateInput.NEW_LINE).build());
    }
}

