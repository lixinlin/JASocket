package org.agilewiki.jasocket;

import org.agilewiki.jactor.lpc.JLPCActor;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class SocketWriter extends JLPCActor {
    protected SocketChannel socketChannel;
    protected ByteBuffer writeBuffer;

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        writeBuffer = ByteBuffer.allocateDirect(maxPacketSize);
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.connect(inetSocketAddress);
    }

    public void writeBytes(byte[] bytes) throws Exception {
        int i = 0;
        while (i < bytes.length) {
            int l = bytes.length - i;
            int r = writeBuffer.remaining();
            if (l > r)
                l = r;
            writeBuffer.put(bytes, i, l);
            i += l;
            if (!writeBuffer.hasRemaining())
                write();
        }
        if (writeBuffer.position() > 0 && getMailbox().isEmpty())
            write();
    }

    void write() throws Exception {
        writeBuffer.flip();
        try {
            while (writeBuffer.hasRemaining())
                socketChannel.write(writeBuffer);
        } catch (ClosedChannelException ex) {
        }
        writeBuffer.clear();
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (Exception ex) {
        }
    }
}