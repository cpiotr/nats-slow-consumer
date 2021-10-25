package pl.ciruk.nats.slowconsumer;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class ConfirmationConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Options options;
    private final Histogram histogram;

    public ConfirmationConsumer(String host, int port, String connectionName) {
        this(host, port, connectionName, null);
    }

    public ConfirmationConsumer(String host, int port, String connectionName, Histogram histogram) {
        this.histogram = histogram;
        options = Connections.createOptions(host, port, connectionName).build();
    }

    public void start(String subject) {
        LOGGER.info("{} started", options.getConnectionName());
        Connection myConnection;
        try {
            myConnection = Nats.connect(options);
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        Dispatcher dispatcher = myConnection.createDispatcher(
                message -> {
                    if (options.getConnectionName().startsWith("Slow")) {
                        // Ignore slow consumer
                        return;
                    }
                    long myPublicationTimestamp = Serialization.deserialize(message.getData());
                    long delay = System.currentTimeMillis() - myPublicationTimestamp;
                    if (histogram != null) {
                        histogram.recordValue(delay);
                    }
                }
        );
        dispatcher.subscribe(subject);
    }
}
