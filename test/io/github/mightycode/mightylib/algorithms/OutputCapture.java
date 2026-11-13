package io.github.mightycode.mightylib.algorithms;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

// Standard output written by a piece of code, to check what an algorithm prints
public final class OutputCapture {
    private OutputCapture() {
    }

    public static String of(Runnable run) {
        PrintStream old = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            run.run();
        } finally {
            System.setOut(old);
        }
        return out.toString();
    }

    public static int lines(String text) {
        return text.isEmpty() ? 0 : text.split("\n").length;
    }
}
