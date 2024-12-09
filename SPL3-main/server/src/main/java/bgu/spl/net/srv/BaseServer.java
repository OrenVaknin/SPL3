package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.impl.tftp.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private final Supplier<Connections<T>> connectionsFactory;
    private ServerSocket sock;

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory,
            Supplier<Connections<T>> connectionsFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.connectionsFactory = connectionsFactory;
		this.sock = null;
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                System.out.println("client connected");

                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<T>(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get(),
                        connectionsFactory.get());
                execute(handler);

            }
            close();
        } catch (IOException ex) {}
        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);
}
