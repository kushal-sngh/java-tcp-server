package com.kush.newrelic.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

/**
 * Created by root on 7/29/17.
 */
@SpringBootApplication
@EnableAsync
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan({"com.kush.newrelic.server","com.kush.newrelic.util"})
public class AppSetup {

    private static ConfigurableApplicationContext configurableApplicationContext;

    public static void main(String[] args) throws IOException, InterruptedException {
        configurableApplicationContext = SpringApplication.run(AppSetup.class, args);
    }

    public static void shutdown() {
        configurableApplicationContext.close();
        System.exit(0);
    }
}
