package bgu.spl.net.impl.tftp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

import java.util.concurrent.ConcurrentHashMap;


public class ConnectionsImpl<T> implements Connections<T> {

    private static ConnectionsImpl<?> instance;
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionMap;
    private final ConcurrentHashMap<String, Integer> users = new ConcurrentHashMap<>();

    public static synchronized <T> ConnectionsImpl<T> getInstance() {
        if (instance == null) {
            instance = new ConnectionsImpl<>();
        }
        return (ConnectionsImpl<T>) instance;
    }


    private ConnectionsImpl() {
        connectionMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean connect(int connectionId, ConnectionHandler<T> handler, String username) {

        if(users.containsKey(username))
            return false;
        users.put(username, connectionId);
        connectionMap.put(connectionId, handler);
        System.out.println("Connected: " + connectionId + " " + username);

        System.out.println("there are " + connectionMap.size() + " connections");
        return true;
    }

    @Override
    public boolean isConnected(String username) {
        return users.containsKey(username);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = connectionMap.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false; // Connection ID not found
    }

    @Override
    public boolean disconnect(int connectionId, String username) {
        connectionMap.remove(connectionId);
        if(users.remove(username) == null)
            return false;
        System.out.println("Disconnected: " + connectionId + " " + username);
        System.out.println("there are " + connectionMap.size() + " connections");
        return true;
    }

    public void sendToAll(T msg) {
        for (Integer id : connectionMap.keySet())
            send(id, msg);

    }
}
