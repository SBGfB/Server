package application;

import java.io.IOException;

/**
 * Created by Sergei on 9/4/2016.
 */
public class Start {
    public static void main(String[] args) throws IOException {
        new NIOServer("localhost", 1080).run();
    }
}
