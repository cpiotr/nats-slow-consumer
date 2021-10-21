package pl.ciruk.nats.slowconsumer;

public class Serialization {
    private static final int MASK = 0xFF;

    static byte[] serialize(long timestamp) {
        int myMask = MASK;
        return new byte[] {
                (byte) ((timestamp >> 56) & myMask),
                (byte) ((timestamp >> 48) & myMask),
                (byte) ((timestamp >> 40) & myMask),
                (byte) ((timestamp >> 32) & myMask),
                (byte) ((timestamp >> 24) & myMask),
                (byte) ((timestamp >> 16) & myMask),
                (byte) ((timestamp >> 8) & myMask),
                (byte) ((timestamp) & myMask)
        };
    }

    static long deserialize(byte[] bytes) {
        long result = 0;
        for (byte myByte : bytes) {
            result <<= 8;
            result |= ((long) myByte & MASK);
        }
        return result;
    }
}
