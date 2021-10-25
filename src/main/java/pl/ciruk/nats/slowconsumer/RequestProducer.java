package pl.ciruk.nats.slowconsumer;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class RequestProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Options options;

    public RequestProducer(String host, int port, String connectionName) {
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
        while (!Thread.currentThread().isInterrupted()) {
            myConnection.publish(subject, Serialization.serialize(System.currentTimeMillis()));
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                exception.printStackTrace();
            }
        }
    }
}
