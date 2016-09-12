package application;

import java.nio.ByteBuffer;

/**
 * Created by Sergei on 9/8/2016.
 */
public class User {
    String userName = "Bob";
    ByteBuffer buffer;

    public User(String userName, ByteBuffer buffer) {
        this.userName = userName;
        this.buffer = buffer;
    }

    public User() {
        this.buffer = ByteBuffer.allocate(2048);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", buffer=" + buffer +
                '}';
    }
}
