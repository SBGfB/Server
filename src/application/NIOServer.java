package application;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Sergei on 9/4/2016.
 */
public class NIOServer {
    private final int port;
    private final String address;

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;

    private final Map listUsers = new HashMap<SocketChannel, User>();

    private final Charset ch = Charset.forName("UTF-8");
    private final CharsetDecoder decoder = ch.newDecoder();

    public NIOServer(String address, int port) throws IOException {
        this.port = port;
        this.address = address;

        this.selector = SelectorProvider.provider().openSelector();
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.socket().bind(new InetSocketAddress(this.address, this.port));
        this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    public void run() {
        System.out.println("Server is starting! " + address + ":" + port);

        try {
            while (this.selector.select() != -1) {
                Iterator selectedKeys = this.selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        this.accept(key);
                    } else if (key.isReadable()) {
                        this.read(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(this.selector, SelectionKey.OP_READ);

        synchronized (listUsers) {
            this.listUsers.put(socketChannel, new User("Bob", ByteBuffer.allocate(2048)));
        }
        System.out.println("New connection! " + socketChannel.getRemoteAddress());
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        User currentUser = (User)listUsers.get(socketChannel);
        ByteBuffer buffer = currentUser.getBuffer();
        buffer.clear();

        int numByte = 0;
        try {
            numByte = socketChannel.read(buffer);
        } catch (IOException e) {
            this.close(key);
        }
        if (numByte == -1) {
            this.close(key);
        } else if (numByte > 0) {
            buffer.flip();
            CharBuffer buff = decoder.decode(buffer);
            JsonObject jsonObject = new JsonParser().parse(String.valueOf(buff)).getAsJsonObject();

            switch (jsonObject.get("type").getAsString()) {
                case "message":
                    JsonObject reply = new JsonObject();
                    reply.addProperty("type", "message");
                    reply.addProperty("name", currentUser.getUserName());
                    reply.addProperty("attachment", jsonObject.get("attachment").getAsString());

                    for(Object obj : this.listUsers.keySet()) {
                        SocketChannel currentChanel = (SocketChannel) obj;
                        currentChanel.write(ByteBuffer.wrap(new Gson().toJson(reply).getBytes(ch)));
                    }

                    System.out.println(currentUser.getUserName() + ": " + jsonObject.get("attachment").getAsString());
                    break;
                case "Name":
                    System.out.println(currentUser.getUserName() + " change name on " + jsonObject.get("attachment").getAsString());
                    currentUser.setUserName(jsonObject.get("attachment").getAsString());
                    break;
            }
        }
    }

    private void close(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        synchronized (listUsers) {
            listUsers.remove(socketChannel);
        }

        if(socketChannel.isConnected()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        key.cancel();
    }
}
