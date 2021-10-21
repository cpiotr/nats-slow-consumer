package pl.ciruk.nats.slowconsumer;

import io.nats.client.Options;

public class Connections {
    public static Options.Builder createOptions(String host, int port, String connectionName) {
        return new Options.Builder()
                .connectionName(connectionName)
                .servers(new String[]{String.format("nats://%s:%d", host, port)})
                .maxReconnects(-1)
                .reconnectBufferSize(0)
                .maxMessagesInOutgoingQueue(0);
    }
}
