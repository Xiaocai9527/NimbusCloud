package util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {
    private long left;
    private long mark = -1;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.left = limit;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(in.available(), left);
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
        mark = left;
    }

    @Override
    public int read() throws IOException {
        if (left == 0) {
            return -1;
        }
        int result = in.read();
        if (result != -1) {
            left--;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (left == 0) {
            return -1;
        }
        int bytesToRead = (int) Math.min(len, left);
        int bytesRead = in.read(b, off, bytesToRead);
        if (bytesRead != -1) {
            left -= bytesRead;
        }
        return bytesRead;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (!in.markSupported()) {
            throw new IOException("Mark not supported");
        }
        if (mark == -1) {
            throw new IOException("Mark not set");
        }
        in.reset();
        left = mark;
    }

    @Override
    public long skip(long n) throws IOException {
        long bytesToSkip = Math.min(n, left);
        long bytesSkipped = in.skip(bytesToSkip);
        left -= bytesSkipped;
        return bytesSkipped;
    }
}

