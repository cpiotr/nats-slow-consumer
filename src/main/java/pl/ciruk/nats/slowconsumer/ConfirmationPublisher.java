package pl.ciruk.nats.slowconsumer;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class ConfirmationPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Options options;

    public ConfirmationPublisher(String host, int port) {
        options = Connections.createOptions(host, port, "ConfirmationPublisher").build();
    }

    public void start(String subjectFrom, String subjectTo) {
        LOGGER.info("{} started", options.getConnectionName());
        final Connection connection;
        try {
            connection = Nats.connect(options);
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        Dispatcher dispatcher = connection.createDispatcher(
                message -> connection.publish(subjectTo, Serialization.serialize(System.currentTimeMillis()))
        );
        dispatcher.subscribe(subjectFrom);
    }
}
