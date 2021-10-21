package pl.ciruk.nats.slowconsumer;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Testcontainers
public class SlowConsumerTest {
    @Container private static final GenericContainer<?> NATS_SERVER = new GenericContainer<>("nats:1.4.1")
            .withExposedPorts(4222)
            .withReuse(true)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    private static final int SLOW_PROXY_PORT = Configuration.getSlowProxyPort();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(Configuration.getThreadNumber());

    @Test
    void shouldReportDelayedMessages() throws InterruptedException {
        String host = NATS_SERVER.getContainerIpAddress();
        Integer port = NATS_SERVER.getFirstMappedPort();
        SlowProxy.start(host, port, SLOW_PROXY_PORT);

        for (int i = 1; i <= Configuration.getNumberOfConsumers(); i++) {
            ConfirmationConsumer myConsumer = new ConfirmationConsumer(host, port, "Consumer" + i);
            threadPool.execute(() -> myConsumer.start(Subjects.CONFIRMATIONS));
        }

        ConfirmationConsumer mySlowConsumer = new ConfirmationConsumer("localhost", SLOW_PROXY_PORT, "SlowConsumer");
        threadPool.execute(() -> mySlowConsumer.start(Subjects.CONFIRMATIONS));

        for (int i = 1; i <= Configuration.getNumberOfProducers(); i++) {
            RequestProducer myRequestProducer = new RequestProducer(host, port, "Producer" + i);
            threadPool.execute(() -> myRequestProducer.start(Subjects.REQUESTS));
        }

        ConfirmationPublisher myConfirmationPublisher = new ConfirmationPublisher(host, port);
        threadPool.execute(() -> myConfirmationPublisher.start(Subjects.REQUESTS, Subjects.CONFIRMATIONS));

        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }

    private static class Subjects {
        public static final String CONFIRMATIONS = "Confirmations";
        public static final String REQUESTS = "Requests";
    }
}
