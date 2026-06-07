package jxmvc.core;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Compresión GZIP opt-in.
 * Activar: {@code jxmvc.gzip.enabled=true} en {@code application.properties}.
 */
final class JxGzip {

    private static final JxLogger log = JxLogger.getLogger(JxGzip.class);

    static final boolean ENABLED;
    static final int     MIN_BYTES;

    static {
        ENABLED   = "true".equalsIgnoreCase(BaseDbResolver.property("jxmvc.gzip.enabled", "false"));
        MIN_BYTES = BaseDbResolver.propertyInt("jxmvc.gzip.minBytes", 860);
        if (ENABLED) log.info("GZIP habilitado (minBytes={})", MIN_BYTES);
    }

    private JxGzip() {}

    static final class GzipWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(2048);
        private PrintWriter       writer;
        private ServletOutputStream stream;
        private String            contentType;

        GzipWrapper(HttpServletResponse r) { super(r); }

        @Override public void setContentType(String t) { contentType = t; super.setContentType(t); }

        @Override public PrintWriter getWriter() throws IOException {
            if (stream != null) throw new IllegalStateException("getOutputStream ya fue llamado");
            if (writer == null) writer = new PrintWriter(new OutputStreamWriter(buffer, "UTF-8"));
            return writer;
        }

        @Override public ServletOutputStream getOutputStream() {
            if (writer != null) throw new IllegalStateException("getWriter ya fue llamado");
            if (stream == null) stream = new ByteBufferStream(buffer);
            return stream;
        }

        @Override public void setContentLength(int l)     {}
        @Override public void setContentLengthLong(long l) {}

        void finish(HttpServletResponse resp, int minBytes) throws IOException {
            if (writer != null) writer.flush();
            byte[] data = buffer.toByteArray();
            if (data.length == 0) return;

            if (data.length >= minBytes && isText(contentType)) {
                ByteArrayOutputStream gz = new ByteArrayOutputStream(data.length / 2);
                try (GZIPOutputStream gos = new GZIPOutputStream(gz)) { gos.write(data); }
                byte[] compressed = gz.toByteArray();
                resp.setHeader("Content-Encoding", "gzip");
                resp.setHeader("Vary", "Accept-Encoding");
                resp.setContentLength(compressed.length);
                resp.getOutputStream().write(compressed);
            } else {
                resp.setContentLength(data.length);
                resp.getOutputStream().write(data);
            }
        }

        private static boolean isText(String ct) {
            if (ct == null) return false;
            String l = ct.toLowerCase();
            return l.startsWith("text/") || l.contains("json") || l.contains("xml");
        }
    }

    private static final class ByteBufferStream extends ServletOutputStream {
        private final ByteArrayOutputStream buf;
        ByteBufferStream(ByteArrayOutputStream buf) { this.buf = buf; }
        @Override public void write(int b)                    { buf.write(b); }
        @Override public void write(byte[] b, int off, int l) { buf.write(b, off, l); }
        @Override public boolean isReady()                    { return true; }
        @Override public void setWriteListener(WriteListener w) {}
    }
}
