package pl.ciruk.nats.slowconsumer;

public class Configuration {
    public static int getSlowProxyPort() {
        return Integer.parseInt(System.getProperty("nats.slow.proxy.port", "12345"));
    }

    public static int getNumberOfConsumers() {
        return Integer.parseInt(System.getProperty("nats.consumers.count", "4"));
    }

    public static int getNumberOfProducers() {
        return Integer.parseInt(System.getProperty("nats.producers.count", "1"));
    }

    public static int getNumberOfSlowConsumers() {
        return Integer.parseInt(System.getProperty("nats.consumers.slow.count", "1"));
    }

    public static int getThreadNumber() {
        return getNumberOfProducers() + 4;
    }
}
