package com.ygohappy123.server.threads;

import com.ygohappy123.server.controllers.MainController;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class WriteServer extends Thread {
    private final MainController controller;

    public WriteServer(MainController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        ObjectOutputStream objectOutput = null;

        while (true) {
            try {
                for (Socket client : this.controller.getClientList()) {
                    objectOutput = new ObjectOutputStream(client.getOutputStream());
                }
            } catch (Exception e) {
            }
        }
    }
}