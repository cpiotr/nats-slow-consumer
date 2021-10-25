package pl.ciruk.nats.slowconsumer;

import org.HdrHistogram.Histogram;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Testcontainers
public class SlowConsumerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Container private static final GenericContainer<?> NATS_SERVER = new GenericContainer<>("nats:2.6.2")
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

        Histogram histogram = createDelayHistogramAndStartLoggingIt();

        for (int i = 1; i <= Configuration.getNumberOfConsumers(); i++) {
            ConfirmationConsumer myConsumer = new ConfirmationConsumer(host, port, "Consumer" + i, histogram);
            threadPool.execute(() -> myConsumer.start(Subjects.CONFIRMATIONS));
        }

        for (int i = 1; i <= Configuration.getNumberOfSlowConsumers(); i++) {
            ConfirmationConsumer mySlowConsumer = new ConfirmationConsumer("localhost", SLOW_PROXY_PORT, "SlowConsumer" + i);
            threadPool.execute(() -> mySlowConsumer.start(Subjects.CONFIRMATIONS));
        }

        for (int i = 1; i <= Configuration.getNumberOfProducers(); i++) {
            RequestProducer myRequestProducer = new RequestProducer(host, port, "Producer" + i);
            threadPool.execute(() -> myRequestProducer.start(Subjects.REQUESTS));
        }

        ConfirmationPublisher myConfirmationPublisher = new ConfirmationPublisher(host, port);
        threadPool.execute(() -> myConfirmationPublisher.start(Subjects.REQUESTS, Subjects.CONFIRMATIONS));

        threadPool.shutdown();
        threadPool.awaitTermination(1, TimeUnit.HOURS);
    }

    @NotNull
    private Histogram createDelayHistogramAndStartLoggingIt() {
        Histogram histogram = new Histogram(10_000, 5);
        Executors.newScheduledThreadPool(1)
                .scheduleWithFixedDelay(
                        () -> LOGGER.debug(
                                "Delays: count={}, p95={}ms, p99={}ms, p100={}ms",
                                histogram.getTotalCount(),
                                histogram.getValueAtPercentile(0.95),
                                histogram.getValueAtPercentile(0.99),
                                histogram.getMaxValue()),
                        5L,
                        5L,
                        TimeUnit.SECONDS
                );
        return histogram;
    }

    private static class Subjects {
        public static final String CONFIRMATIONS = "Confirmations";
        public static final String REQUESTS = "Requests";
    }
}
