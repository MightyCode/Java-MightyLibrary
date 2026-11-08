package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;

/** Reads bits (most significant first) of a byte array, as the FLAC format stores them. */
final class FlacBitReader {
    private final byte[] data;
    // Position in bits
    private long position;

    FlacBitReader(byte[] data) {
        this.data = data;
    }

    boolean hasBits(long bits) {
        return position + bits <= (long) data.length * 8;
    }

    // Up to 32 bits, as the low bits of the result
    int readInt(int bits) {
        long value = 0;

        while (bits > 0) {
            int index = (int) (position >>> 3);
            if (index >= data.length)
                throw new ResourceException("truncated flac file");

            int offset = (int) (position & 7);
            int take = Math.min(bits, 8 - offset);
            int current = ((data[index] & 0xFF) >>> (8 - offset - take)) & ((1 << take) - 1);

            value = (value << take) | current;
            position += take;
            bits -= take;
        }

        return (int) value;
    }

    // Two's complement on the given number of bits (up to 32)
    int readSigned(int bits) {
        if (bits == 0)
            return 0;

        int value = readInt(bits);
        return (value << (32 - bits)) >> (32 - bits);
    }

    // Number of zero bits before the next one bit (which is consumed)
    int readUnary() {
        int count = 0;

        while (true) {
            int index = (int) (position >>> 3);
            if (index >= data.length)
                throw new ResourceException("truncated flac file");

            int offset = (int) (position & 7);
            int current = (data[index] & 0xFF) & (0xFF >>> offset);

            if (current == 0) {
                count += 8 - offset;
                position += 8 - offset;
            } else {
                int zeros = Integer.numberOfLeadingZeros(current) - 24 - offset;
                position += zeros + 1;
                return count + zeros;
            }
        }
    }

    void skipBytes(int bytes) {
        position += (long) bytes * 8;
        if (position > (long) data.length * 8)
            throw new ResourceException("truncated flac file");
    }

    void alignToByte() {
        position = (position + 7) & ~7L;
    }
}
