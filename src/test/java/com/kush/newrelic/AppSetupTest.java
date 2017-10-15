package com.kush.newrelic;

import com.kush.newrelic.app.AppSetup;
import com.kush.newrelic.server.TCPServer;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static com.kush.newrelic.util.ValidateInput.NEW_LINE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by root on 7/27/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {TCPServer.class, AppSetup.class})
@PropertySource("application.properties")
public class AppSetupTest {


    private static final long INPUT_SIZE = 2000000L;
   // private static final long INPUT_SIZE = 30;

    private static final String HOST = "localhost";
    private static final int PORT = 4000;
    private static final int EXPECTED_CONNECTIONS = 5;
    private static final long MINIMUM_THROUGHPUT = 250000L;

    private static List<String> numbersList = new ArrayList<>();

    @Autowired
    TcpNetServerConnectionFactory cf;

    @Autowired
    MessageChannel tcpIn;

    @Autowired
    AtomicInteger totalUniqueCount;

    @Autowired
    AtomicInteger totalDuplicateCount;

    @Autowired
    ConcurrentSkipListSet<String> uniqueValues;

    @BeforeClass
    public static void dataSetup() throws Exception {

        System.out.println("Generating values");
        for (long i = 0L; i < INPUT_SIZE; i++) {
            numbersList.add(getRandomNumber());
        }
    }


    public static long convertNanoToSeconds(long timeInNanos) {
        return TimeUnit.SECONDS.convert(timeInNanos, TimeUnit.NANOSECONDS);
    }
    /*
       Generate formated 9 digit number with zero pads
     */
    private static String getRandomNumber() {
        Random r = new Random();
        int Low = 1;
        int High = 999999999;
        return String.format("%09d", (r.nextInt(High - Low) + Low));
    }

    @Before
    public void setup() throws Exception {

        TestingUtilities.waitListening(cf, 10000L);
        System.out.println();
        System.out.println("Listening  on port 4000...");
    }

    @Test
    public void uniqueAndDuplicateCountsValidation()
            throws IOException {

        numbersList.parallelStream().forEach(
                n -> tcpIn.send(MessageBuilder.withPayload(n + NEW_LINE).build())
        );

        assertEquals("Sum of unique counts equal to size of unique values list",
                totalUniqueCount.get(), uniqueValues.size());
        assertEquals("Input size minus duplicates equals size of unique values list",
                INPUT_SIZE - totalDuplicateCount.get(), uniqueValues.size());
    }

    @Test
    public void connectionsLimit()
            throws IOException {

        List<TelnetClient> telnetClientList = new ArrayList<>();

        for (int i = 0; i < EXPECTED_CONNECTIONS; i++) {
            TelnetClient telnetClient = new TelnetClient();
            telnetClient.connect(HOST, PORT);

            if (telnetClient.isConnected() && telnetClient.isAvailable()) {
                telnetClientList.add(telnetClient);
                telnetClient.getOutputStream().write("123456789".getBytes());
            }
        }
        assertEquals("Must not more then 5 connections.", EXPECTED_CONNECTIONS, telnetClientList.size());
    }
    @Test(expected=UnknownHostException.class)
    public void connectionMustFail_onWrongHost()
            throws IOException {
        TelnetClient telnetClient = new TelnetClient();
        telnetClient.connect("not_localhost", 0);
    }

    @Test(expected = MessageHandlingException.class)
    public void shutDownCommant()
            throws IOException {
        tcpIn.send(MessageBuilder.withPayload(TCPServer.SHUT_DOWN_COMMAND+NEW_LINE).build());

        assertTrue(true);
    }
}
