package bgu.spl.net.srv;

public interface Connections<T> {

    boolean connect(int connectionId, ConnectionHandler<T> handler, String username);

    boolean isConnected(String name);

    boolean send(int connectionId, T msg);

    boolean disconnect(int connectionId, String username);

    void sendToAll(T msg);
}
