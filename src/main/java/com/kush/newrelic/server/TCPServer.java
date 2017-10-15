package com.kush.newrelic.server;

import com.kush.newrelic.app.AppSetup;
import com.kush.newrelic.util.ValidateInput;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.*;
import org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by root on 7/28/17.
 */
@Configuration
@Import(ValidateInput.class)
public class TCPServer {


    public static final String SHUT_DOWN_COMMAND = "terminate";
    protected final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    ValidateInput validateInput;

    @Value("${tcp.server.port:4000}")
    int port;

    @Bean
    public ConcurrentSkipListSet<String> uniqueValues() {

        return new ConcurrentSkipListSet();
    }
    @Bean
    public ByteArrayRawSerializer byteArrayCrLfSerializer(){
       return new ByteArrayRawSerializer();
    }

    /**
     * TCP server side Connection factory to provide connection factory.
     * Using Thread pool to support parallel execution.
     * @return server factory.
     */
    @Bean
    public TcpNetServerConnectionFactory serverCF() {

        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(30);
        threadPoolTaskExecutor.setQueueCapacity(10);
        threadPoolTaskExecutor.initialize();

        TcpNetServerConnectionFactory serverCF = new TcpNetServerConnectionFactory(port);
        serverCF.setTaskExecutor(threadPoolTaskExecutor);
        serverCF.getMapper().setAddContentTypeHeader(true);
        serverCF.setDeserializer(byteArrayCrLfSerializer());
        return serverCF;
    }

    /**
     * Inbound chanel adapter,tied to server factory
     * @param cf
     * @return adapter.
     */
    @Bean
    public TcpReceivingChannelAdapter inbound(AbstractServerConnectionFactory cf) {
        TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
        adapter.setConnectionFactory(cf);
        adapter.setOutputChannel(tcpIn());
        return adapter;
    }

    /**
     * Input chanel.
     * @return
     */
    @Bean
    public MessageChannel tcpIn() {
        DirectChannel directChannel = new DirectChannel();
        DefaultMessageBuilderFactory defaultMessageBuilderFactory = new DefaultMessageBuilderFactory();

        directChannel.setMessageBuilderFactory(defaultMessageBuilderFactory);

        return new DirectChannel();
    }

    @Transformer(inputChannel = "tcpIn", outputChannel = "serviceChannel")
    @Bean
    public ObjectToStringTransformer transformer() {

        return new ObjectToStringTransformer();
    }


    @ServiceActivator(inputChannel = "serviceChannel")
    public void service(String in) {

        if (in.matches("^" + SHUT_DOWN_COMMAND + ValidateInput.NEW_LINE + "$")) {
            AppSetup.shutdown();
        }
        logMessageAndIncreementCounters(in);
    }

    /**
     *Capture all TCP connection open events.
     * @return
     */
    @Bean
    public ApplicationListener<TcpConnectionOpenEvent> listener() {
        return new ApplicationListener<TcpConnectionOpenEvent>() {
            @Override
            public void onApplicationEvent(TcpConnectionOpenEvent event) {
                // Restrict to 5 connections only
                if (serverCF().getOpenConnectionIds().size() > 5)
                    serverCF().closeConnection(event.getConnectionId());
            }
        };
    }

    @Bean
    public AtomicInteger currentUniqueCount() {

        return new AtomicInteger(0);
    }

    @Bean
    public AtomicInteger currentDuplicateCount() {

        return new AtomicInteger(0);
    }

    @Bean
    public AtomicInteger totalUniqueCount() {

        return new AtomicInteger(0);
    }

    @Bean
    public AtomicInteger totalDuplicateCount() {

        return new AtomicInteger(0);
    }

    @Bean
    public TcpConnectionInterceptor getInterceptor() {
        return new TcpConnectionInterceptorSupport () {
            @Override
            public void close() {
                this.close();
            }
        };
    }
    @Async
    private void logMessageAndIncreementCounters(String input) {
        if (!validateInput.validate(input)) {
            getInterceptor().close();
        } else {
            if (uniqueValues().add(input)) {
                currentUniqueCount().incrementAndGet();
                totalUniqueCount().incrementAndGet();
                logger.info(input);
            } else {
                currentDuplicateCount().incrementAndGet();
                totalDuplicateCount().incrementAndGet();
            }
        }
    }
}

