package io.github.mightycode.mightylib.console;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * Copies the program output (System.out and System.err) to a console log, line by line, while it is still written
 * to the real streams. Static: the standard streams belong to the process.
 */
public final class LogCapture {
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    private LogCapture() {}

    public static synchronized void install(ConsoleLog log) {
        if (originalOut != null)
            uninstall();

        originalOut = System.out;
        originalErr = System.err;

        System.setOut(tee(originalOut, log, ConsoleLog.EType.OUT, streamCharset("sun.stdout.encoding")));
        System.setErr(tee(originalErr, log, ConsoleLog.EType.ERR, streamCharset("sun.stderr.encoding")));
    }

    public static synchronized void uninstall() {
        if (originalOut == null)
            return;

        System.out.flush();
        System.err.flush();
        System.setOut(originalOut);
        System.setErr(originalErr);
        originalOut = null;
        originalErr = null;
    }

    public static synchronized boolean isInstalled() {
        return originalOut != null;
    }

    // Standard output without copy to the console log (what is already in the log, written for the terminal)
    public static synchronized PrintStream rawOut() {
        return originalOut != null ? originalOut : System.out;
    }

    public static synchronized PrintStream rawErr() {
        return originalErr != null ? originalErr : System.err;
    }

    // Java 11 encodes the standard streams with this property when set, otherwise with the default charset
    private static Charset streamCharset(String property) {
        String name = System.getProperty(property);
        try {
            if (name != null && !name.isEmpty())
                return Charset.forName(name);
        } catch (RuntimeException e) {
            // Unknown charset: the default one
        }

        return Charset.defaultCharset();
    }

    static PrintStream tee(PrintStream target, ConsoleLog log, ConsoleLog.EType type, Charset charset) {
        return new PrintStream(new LineSplitter(target, log, type, charset), true, charset);
    }

    // Writes the bytes to the real stream and gives each complete line to the log
    static final class LineSplitter extends OutputStream {
        private final PrintStream target;
        private final ConsoleLog log;
        private final ConsoleLog.EType type;
        private final Charset charset;
        private final ByteArrayOutputStream pending;

        LineSplitter(PrintStream target, ConsoleLog log, ConsoleLog.EType type, Charset charset) {
            this.target = target;
            this.log = log;
            this.type = type;
            this.charset = charset;
            this.pending = new ByteArrayOutputStream();
        }

        @Override
        public synchronized void write(int b) {
            target.write(b);
            accept(b);
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            target.write(bytes, offset, length);
            for (int i = offset; i < offset + length; ++i)
                accept(bytes[i]);
        }

        private void accept(int b) {
            if (b == '\n') {
                emit();
            } else if (b != '\r') {
                pending.write(b);
            }
        }

        private void emit() {
            String line = new String(pending.toByteArray(), charset);
            pending.reset();
            log.add(type, null, line);
        }

        @Override
        public synchronized void flush() {
            target.flush();
        }
    }
}
