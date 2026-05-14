package com.ksa.financing.infra.audit;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Reads the entire request body once into memory at construction time, then
 * replays it for downstream filters and the controller. Lets the audit filter
 * see the body even when Spring Security rejects the request before the
 * controller is reached.
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = readAll(request);
    }

    private static byte[] readAll(HttpServletRequest request) throws IOException {
        try (var in = request.getInputStream()) {
            return in.readAllBytes();
        }
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        String enc = getCharacterEncoding();
        Charset cs = (enc != null && !enc.isBlank()) ? Charset.forName(enc) : StandardCharsets.UTF_8;
        return new BufferedReader(new InputStreamReader(getInputStream(), cs));
    }

    private static final class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream delegate;

        CachedServletInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override public int read() { return delegate.read(); }
        @Override public int read(byte[] b, int off, int len) { return delegate.read(b, off, len); }
        @Override public int available() { return delegate.available(); }
        @Override public boolean isFinished() { return delegate.available() == 0; }
        @Override public boolean isReady() { return true; }
        @Override public void setReadListener(ReadListener listener) { /* no-op for cached body */ }
    }
}
