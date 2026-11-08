package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Measures, on the card and the driver in use, the way to send the pixels of a texture by strips of rows:
 * directly or through PBOs. Main thread, blocks the program for about a second.
 *
 * For each texture size and each way: "call" is the time spent in the calls (what a frame pays),
 * "total" is the time until the card has everything (glFinish).
 */
public final class TextureUploadBench {
    private static final int REPEATS = 5;
    private static final int STRIP_BYTES = 4 * 1024 * 1024;

    // { width, height }: a big texture sent by strips, then textures that fit in one strip
    private static final int[][] SIZES = { { 4096, 4096 }, { 2048, 2048 }, { 1024, 1024 }, { 512, 512 }, { 128, 128 } };

    private TextureUploadBench() {}

    public static JSONObject run() {
        TextureUploader.Variant[] variants = TextureUploader.Variant.values();
        JSONArray results = new JSONArray();

        for (int[] size : SIZES) {
            int width = size[0], height = size[1];
            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            for (int i = 0; i < pixels.capacity(); ++i)
                pixels.put(i, (byte) (i * 31 + (i >> 8)));

            // Warm up each way once, then the repeats go through the ways in turn so that none is favoured
            List<List<Double>> calls = new ArrayList<>();
            List<List<Double>> totals = new ArrayList<>();
            for (int v = 0; v < variants.length; ++v) {
                calls.add(new ArrayList<>());
                totals.add(new ArrayList<>());
                send(variants[v], pixels, width, height);
            }

            for (int r = 0; r < REPEATS; ++r) {
                for (int v = 0; v < variants.length; ++v) {
                    double[] time = send(variants[v], pixels, width, height);
                    calls.get(v).add(time[0]);
                    totals.get(v).add(time[1]);
                }
            }

            JSONObject line = new JSONObject();
            line.put("size", width + "x" + height);
            line.put("megabytes", pixels.capacity() / 1048576.0);
            for (int v = 0; v < variants.length; ++v) {
                JSONObject way = new JSONObject();
                way.put("callMs", round(median(calls.get(v))));
                way.put("totalMs", round(median(totals.get(v))));
                line.put(variants[v].name(), way);
            }

            results.put(line);
        }

        TextureUploader.releaseBuffers();

        JSONObject json = new JSONObject();
        json.put("results", results);
        json.put("repeats", REPEATS);
        return json;
    }

    // { time in the calls, time until the card is done } in milliseconds
    private static double[] send(TextureUploader.Variant variant, ByteBuffer pixels, int width, int height) {
        int texture = GenDeleteResources.GenTexture();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glFinish();

        int rowBytes = width * 4;
        int rowsPerStrip = Math.max(1, Math.min(height, STRIP_BYTES / rowBytes));

        long callNanos = 0;
        long start = System.nanoTime();
        for (int row = 0; row < height; row += rowsPerStrip) {
            int rows = Math.min(rowsPerStrip, height - row);
            ByteBuffer strip = pixels.duplicate();
            strip.position(row * rowBytes);
            strip.limit((row + rows) * rowBytes);

            long callStart = System.nanoTime();
            TextureUploader.upload(variant, GL_TEXTURE_2D, row, width, rows, strip);
            callNanos += System.nanoTime() - callStart;
        }

        glFinish();
        long totalNanos = System.nanoTime() - start;

        glBindTexture(GL_TEXTURE_2D, 0);
        GenDeleteResources.DeleteTexture(texture);

        return new double[] { callNanos / 1e6, totalNanos / 1e6 };
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
