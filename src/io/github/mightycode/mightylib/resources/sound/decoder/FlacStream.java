package io.github.mightycode.mightylib.resources.sound.decoder;

import io.github.mightycode.mightylib.resources.ResourceException;

/**
 * Frame by frame decoder of a FLAC stream held in memory (format: https://xiph.org/flac/format.html).
 * Written for this engine, no external code. The CRCs are not checked: the MD5 of the STREAMINFO block lets a test
 * check the whole decoding. Samples keep the bit depth of the file (8 to 24 bits).
 */
final class FlacStream {
    private static final int MAX_BLOCK_SIZE = 65536;
    private static final int MAX_CHANNELS = 8;

    final int sampleRate;
    final int channels;
    final int bitsPerSample;
    // 0 when the file does not say
    final long totalSamples;
    // MD5 of the decoded samples (little endian, interleaved), all zero when the encoder did not compute it
    final byte[] md5 = new byte[16];

    private final FlacBitReader in;
    private long decodedSamples;

    FlacStream(byte[] data) {
        in = new FlacBitReader(data);

        if (!in.hasBits(32) || in.readInt(32) != 0x664C6143)
            throw new ResourceException("not a flac file");

        int rate = 0, channelCount = 0, bits = 0;
        long samples = 0;
        boolean streamInfo = false;
        boolean last;

        do {
            int header = in.readInt(8);
            last = (header & 0x80) != 0;
            int type = header & 0x7F;
            int length = in.readInt(24);

            if (type == 0 && length >= 34) {
                in.readInt(16);   // minimum block size
                in.readInt(16);   // maximum block size
                in.readInt(24);   // minimum frame size
                in.readInt(24);   // maximum frame size
                rate = in.readInt(20);
                channelCount = in.readInt(3) + 1;
                bits = in.readInt(5) + 1;
                samples = ((long) in.readInt(4) << 32) | (in.readInt(32) & 0xFFFFFFFFL);
                for (int i = 0; i < 16; ++i)
                    md5[i] = (byte) in.readInt(8);

                in.skipBytes(length - 34);
                streamInfo = true;
            } else {
                in.skipBytes(length);
            }
        } while (!last);

        if (!streamInfo)
            throw new ResourceException("flac file without STREAMINFO");
        if (channelCount > MAX_CHANNELS || rate <= 0)
            throw new ResourceException("invalid flac STREAMINFO");
        if (bits < 4 || bits > 24)
            throw new ResourceException("unsupported flac sample size (" + bits + " bits, 4 to 24 supported)");

        sampleRate = rate;
        channels = channelCount;
        bitsPerSample = bits;
        totalSamples = samples;
    }

    // New buffers for nextFrame: one array of samples per channel
    int[][] createBuffers() {
        return new int[channels][MAX_BLOCK_SIZE];
    }

    /**
     * Decodes the next frame into out (createBuffers).
     * @return the number of samples by channel, 0 at the end of the stream
     */
    int nextFrame(int[][] out) {
        if (totalSamples > 0 && decodedSamples >= totalSamples)
            return 0;
        // A frame is at least a header and a CRC: what is left is padding or a tag
        if (!in.hasBits(48))
            return 0;

        if (in.readInt(14) != 0x3FFE)
            throw new ResourceException("flac frame not found (corrupted file)");

        in.readInt(1);   // reserved
        in.readInt(1);   // blocking strategy
        int blockSizeCode = in.readInt(4);
        int sampleRateCode = in.readInt(4);
        int channelCode = in.readInt(4);
        int sizeCode = in.readInt(3);
        in.readInt(1);   // reserved

        skipUtf8Number();

        int blockSize;
        if (blockSizeCode == 0)
            throw new ResourceException("invalid flac block size");
        else if (blockSizeCode == 1)
            blockSize = 192;
        else if (blockSizeCode <= 5)
            blockSize = 576 << (blockSizeCode - 2);
        else if (blockSizeCode == 6)
            blockSize = in.readInt(8) + 1;
        else if (blockSizeCode == 7)
            blockSize = in.readInt(16) + 1;
        else
            blockSize = 256 << (blockSizeCode - 8);

        // The sample rate of the STREAMINFO is used
        if (sampleRateCode == 12)
            in.readInt(8);
        else if (sampleRateCode == 13 || sampleRateCode == 14)
            in.readInt(16);

        in.readInt(8);   // CRC-8

        int bits;
        switch (sizeCode) {
            case 0: bits = bitsPerSample; break;
            case 1: bits = 8; break;
            case 2: bits = 12; break;
            case 4: bits = 16; break;
            case 5: bits = 20; break;
            case 6: bits = 24; break;
            default: throw new ResourceException("unsupported flac sample size code " + sizeCode);
        }

        int frameChannels = channelCode < 8 ? channelCode + 1 : 2;
        if (channelCode > 10 || frameChannels != channels)
            throw new ResourceException("invalid flac channel assignment");
        if (blockSize > MAX_BLOCK_SIZE)
            throw new ResourceException("flac block too big");

        for (int channel = 0; channel < channels; ++channel) {
            // The side channel has one more bit
            boolean side = (channelCode == 8 && channel == 1) || (channelCode == 9 && channel == 0)
                    || (channelCode == 10 && channel == 1);
            decodeSubframe(out[channel], blockSize, bits + (side ? 1 : 0));
        }

        if (channelCode == 8) {
            for (int i = 0; i < blockSize; ++i)
                out[1][i] = out[0][i] - out[1][i];
        } else if (channelCode == 9) {
            for (int i = 0; i < blockSize; ++i)
                out[0][i] += out[1][i];
        } else if (channelCode == 10) {
            for (int i = 0; i < blockSize; ++i) {
                int side = out[1][i];
                int mid = (out[0][i] << 1) | (side & 1);
                out[0][i] = (mid + side) >> 1;
                out[1][i] = (mid - side) >> 1;
            }
        }

        in.alignToByte();
        in.readInt(16);   // CRC-16

        decodedSamples += blockSize;
        return blockSize;
    }

    private void skipUtf8Number() {
        int first = in.readInt(8);
        int extra;

        if (first < 0x80)
            extra = 0;
        else if (first < 0xC0)
            throw new ResourceException("invalid flac frame number");
        else if (first < 0xE0)
            extra = 1;
        else if (first < 0xF0)
            extra = 2;
        else if (first < 0xF8)
            extra = 3;
        else if (first < 0xFC)
            extra = 4;
        else if (first < 0xFE)
            extra = 5;
        else if (first == 0xFE)
            extra = 6;
        else
            throw new ResourceException("invalid flac frame number");

        in.skipBytes(extra);
    }

    private void decodeSubframe(int[] out, int blockSize, int bits) {
        if (in.readInt(1) != 0)
            throw new ResourceException("invalid flac subframe");

        int type = in.readInt(6);

        // Wasted bits: samples that are all multiples of 2^wasted
        int wasted = 0;
        if (in.readInt(1) == 1) {
            wasted = 1;
            while (in.readInt(1) == 0)
                ++wasted;
        }
        bits -= wasted;

        if (type == 0) {
            int value = in.readSigned(bits);
            for (int i = 0; i < blockSize; ++i)
                out[i] = value;
        } else if (type == 1) {
            for (int i = 0; i < blockSize; ++i)
                out[i] = in.readSigned(bits);
        } else if (type >= 8 && type <= 12) {
            int order = type - 8;
            for (int i = 0; i < order; ++i)
                out[i] = in.readSigned(bits);

            readResiduals(out, blockSize, order);
            for (int i = order; i < blockSize; ++i)
                out[i] += fixedPrediction(out, i, order);
        } else if (type >= 32) {
            int order = (type & 31) + 1;
            for (int i = 0; i < order; ++i)
                out[i] = in.readSigned(bits);

            int precision = in.readInt(4) + 1;
            if (precision == 16)
                throw new ResourceException("invalid flac coefficient precision");

            int shift = in.readSigned(5);
            if (shift < 0)
                throw new ResourceException("invalid flac shift");

            int[] coefficients = new int[order];
            for (int i = 0; i < order; ++i)
                coefficients[i] = in.readSigned(precision);

            readResiduals(out, blockSize, order);
            for (int i = order; i < blockSize; ++i) {
                long sum = 0;
                for (int j = 0; j < order; ++j)
                    sum += (long) coefficients[j] * out[i - 1 - j];

                out[i] += (int) (sum >> shift);
            }
        } else {
            throw new ResourceException("invalid flac subframe type " + type);
        }

        if (wasted > 0) {
            for (int i = 0; i < blockSize; ++i)
                out[i] <<= wasted;
        }
    }

    private static long fixedPrediction(int[] s, int i, int order) {
        switch (order) {
            case 0: return 0;
            case 1: return s[i - 1];
            case 2: return 2L * s[i - 1] - s[i - 2];
            case 3: return 3L * s[i - 1] - 3L * s[i - 2] + s[i - 3];
            default: return 4L * s[i - 1] - 6L * s[i - 2] + 4L * s[i - 3] - s[i - 4];
        }
    }

    // Rice coded residuals into out[order..blockSize)
    private void readResiduals(int[] out, int blockSize, int order) {
        int method = in.readInt(2);
        if (method > 1)
            throw new ResourceException("invalid flac residual coding");

        int partitionOrder = in.readInt(4);
        int partitions = 1 << partitionOrder;
        int parameterBits = method == 0 ? 4 : 5;
        int escape = method == 0 ? 15 : 31;

        if ((blockSize >> partitionOrder) < order || (blockSize & (partitions - 1)) != 0)
            throw new ResourceException("invalid flac partition order");

        int index = order;
        for (int partition = 0; partition < partitions; ++partition) {
            int count = (blockSize >> partitionOrder) - (partition == 0 ? order : 0);
            int parameter = in.readInt(parameterBits);

            if (parameter == escape) {
                int bits = in.readInt(5);
                for (int i = 0; i < count; ++i)
                    out[index++] = in.readSigned(bits);
            } else {
                for (int i = 0; i < count; ++i) {
                    long value = ((long) in.readUnary() << parameter) | (parameter == 0 ? 0 : in.readInt(parameter) & 0xFFFFFFFFL);
                    out[index++] = (int) ((value >>> 1) ^ -(value & 1));
                }
            }
        }
    }
}
