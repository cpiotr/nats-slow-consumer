package pl.ciruk.nats.slowconsumer;

public class Serialization {
    private static final int MASK = 0xFF;
    static final int RESULT_SIZE_BYTES = 1024;

    static byte[] serialize(long timestamp) {
        byte[] myBytes = new byte[RESULT_SIZE_BYTES];
        // Serialize timestamp as most significant bytes
        for (int i = 1; i <= 8; i++) {
            myBytes[i - 1] = (byte) ((timestamp >> (64 - i * 8) & MASK));
        }
        // Populate byte array with insignificant data
        for (int i = 8; i < myBytes.length; i++) {
            myBytes[i] = (byte) (i % 255);
        }
        return myBytes;
    }

    static long deserialize(byte[] bytes) {
        long result = 0;
        // Deserialize just the most significant bytes to retrieve the timestamp
        for (int i = 0; i < 8; i++) {
            byte myByte = bytes[i];
            result <<= 8;
            result |= ((long) myByte & MASK);
        }
        return result;
    }
}
