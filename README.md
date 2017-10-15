## infra-kushal-singh
kushalvir


## Tools and technologies used in Application ##

1. Spring Boot
2. Spring Integration
3. Logback
4. Java 8
5. maven
6. Apache Commons
7. Apache Commons Net

**Application is desgined and implemented as per the requirements came in email.**

1. ValiadteInput.java is validating input using regex.
2. Thread-safe ConcurrentSkipListSet is used to avoid duplicates.
3. For reporting purpose AtomicInteger class is used since its work well in multithreading environment.
4. @Async anotation is used on logMessageAndIncreementCounters to not to block the threads while validing and logging messages.
5. TcpConnectionInterceptor and TcpConnectionInterceptorSupport are used to close current tcp connection in case of invalid input.
 

## Assumptions ##

1. Server-native newline sequence is determined using System.lineSeparator()
2. numbers.log is located in the project directory: /${project-dir}/numbers.log
3. During Integration testing throughput was not tested as per requirements. 
4. Assumes you have Java 8 mavan is installed locally


## Testing ##

**TelentClient or below telnet command can be used to test application.**

telnet localhost 4000

or we can use follwing commant as well(I tested it worked for me).Do not add \n after input serializer would handle NEW-LINE.

echo "678123129" | nc localhost 4000


## Run unit/integration tests:##

mvn -Dtest=AppSetupTest test

## Starting the application ##
mvn spring-boot:run
