package pl.ciruk.nats.slowconsumer;

import com.github.terma.javaniotcpproxy.StaticTcpProxyConfig;
import com.github.terma.javaniotcpproxy.TcpProxyConfig;
import com.github.terma.javaniotcpserver.TcpServer;
import com.github.terma.javaniotcpserver.TcpServerConfig;
import com.github.terma.javaniotcpserver.TcpServerHandler;
import com.github.terma.javaniotcpserver.TcpServerHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SlowProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void start(String remoteHost, int remotePort, int localPort) {
        StaticTcpProxyConfig myProxyConfig = new StaticTcpProxyConfig(
                localPort,
                remoteHost,
                remotePort,
                4);
        TcpProxy myTcpProxy = new TcpProxy(myProxyConfig);
        myTcpProxy.start();
    }

    static class TcpProxy {
        private final TcpServer server;

        public TcpProxy(TcpProxyConfig config) {
            TcpProxyConnectorFactory handlerFactory = new TcpProxyConnectorFactory(config);
            TcpServerConfig serverConfig = new TcpServerConfig(config.getLocalPort(), handlerFactory, config.getWorkerCount());
            this.server = new TcpServer(serverConfig);
        }

        public void start() {
            this.server.start();
        }

        public void shutdown() {
            this.server.shutdown();
        }
    }

    static class TcpProxyConnectorFactory implements TcpServerHandlerFactory {
        private final TcpProxyConfig config;

        public TcpProxyConnectorFactory(TcpProxyConfig config) {
            this.config = config;
        }

        public TcpServerHandler create(SocketChannel clientChannel) {
            return new MyProxyConnector(clientChannel, this.config);
        }
    }

    static class MyProxyConnector implements TcpServerHandler {
        private final TcpProxyBuffer clientBuffer = new TcpProxyBuffer();
        private final TcpProxyBuffer serverBuffer = new TcpProxyBuffer();
        private final SocketChannel clientChannel;
        private Selector selector;
        private SocketChannel serverChannel;
        private final TcpProxyConfig config;

        public MyProxyConnector(SocketChannel clientChannel, TcpProxyConfig config) {
            this.clientChannel = clientChannel;
            this.config = config;
        }

        public void readFromClient() throws IOException {
            this.serverBuffer.writeFrom(this.clientChannel);
            if (this.serverBuffer.isReadyToRead()) {
                this.register();
            }

        }

        public void readFromServer() throws IOException {
            this.clientBuffer.writeFrom(this.serverChannel);
            if (this.clientBuffer.isReadyToRead()) {
                this.register();
            }

        }

        public void writeToClient() throws IOException {
            sleepUninterruptedly();
            this.clientBuffer.writeTo(this.clientChannel);
            if (this.clientBuffer.isReadyToWrite()) {
                this.register();
            }

        }

        public void writeToServer() throws IOException {
            this.serverBuffer.writeTo(this.serverChannel);
            if (this.serverBuffer.isReadyToWrite()) {
                this.register();
            }

        }

        public void register() throws ClosedChannelException {
            int clientOps = 0;
            if (this.serverBuffer.isReadyToWrite()) {
                clientOps |= 1;
            }

            if (this.clientBuffer.isReadyToRead()) {
                clientOps |= 4;
            }

            this.clientChannel.register(this.selector, clientOps, this);
            int serverOps = 0;
            if (this.clientBuffer.isReadyToWrite()) {
                serverOps |= 1;
            }

            if (this.serverBuffer.isReadyToRead()) {
                serverOps |= 4;
            }

            this.serverChannel.register(this.selector, serverOps, this);
        }

        public void register(Selector selector) {
            this.selector = selector;

            try {
                this.clientChannel.configureBlocking(false);
                InetSocketAddress socketAddress = new InetSocketAddress(this.config.getRemoteHost(), this.config.getRemotePort());
                this.serverChannel = SocketChannel.open();
                this.serverChannel.connect(socketAddress);
                this.serverChannel.configureBlocking(false);
                this.register();
            } catch (IOException exception) {
                this.destroy();
                LOGGER.warn("Could not connect to {}:{}", this.config.getRemoteHost(), this.config.getRemotePort(), exception);
            }

        }

        public void process(SelectionKey key) {
            try {
                if (key.channel() == this.clientChannel) {
                    if (key.isValid() && key.isReadable()) {
                        this.readFromClient();
                    }

                    if (key.isValid() && key.isWritable()) {
                        this.writeToClient();
                    }
                }

                if (key.channel() == this.serverChannel) {
                    if (key.isValid() && key.isReadable()) {
                        this.readFromServer();
                    }

                    if (key.isValid() && key.isWritable()) {
                        this.writeToServer();
                    }
                }
            } catch (ClosedChannelException var3) {
                this.destroy();
                LOGGER.info("Channel was closed by client or server.", var3);
            } catch (IOException var4) {
                this.destroy();
            }
        }

        public void destroy() {
            closeQuietly(this.clientChannel);
            closeQuietly(this.serverChannel);
        }

        private static void closeQuietly(SocketChannel channel) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException var2) {
                    LOGGER.warn("Could not close channel properly.", var2);
                }
            }
        }
    }

    private static void sleepUninterruptedly() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted", exception);
        }
    }

    static class TcpProxyBuffer {
        private final ByteBuffer buffer = ByteBuffer.allocateDirect(1000);
        private TcpProxyBuffer.BufferState state;

        TcpProxyBuffer() {
            this.state = TcpProxyBuffer.BufferState.READY_TO_WRITE;
        }

        public boolean isReadyToRead() {
            return this.state == TcpProxyBuffer.BufferState.READY_TO_READ;
        }

        public boolean isReadyToWrite() {
            return this.state == TcpProxyBuffer.BufferState.READY_TO_WRITE;
        }

        public void writeFrom(SocketChannel channel) throws IOException {
            int read = channel.read(this.buffer);
            if (read == -1) {
                throw new ClosedChannelException();
            } else {
                if (read > 0) {
                    this.buffer.flip();
                    this.state = TcpProxyBuffer.BufferState.READY_TO_READ;
                }

            }
        }

        public void writeTo(SocketChannel channel) throws IOException {
            channel.write(this.buffer);
            if (this.buffer.remaining() == 0) {
                this.buffer.clear();
                this.state = TcpProxyBuffer.BufferState.READY_TO_WRITE;
            }

        }

        private enum BufferState {
            READY_TO_WRITE,
            READY_TO_READ;

            BufferState() {
            }
        }
    }
}
