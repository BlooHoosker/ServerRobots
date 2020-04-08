package com.company;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static final int PORT = 4242;

    public static void main(String[] args) {
        Socket socket = null;
        // Making new server socket listener, autoclose exception catching
        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            // Infinitely waiting for new connections and creating a thread for each connection, max connections 50 by default

            System.out.println("Server starting");
            while(true){
                socket = serverSocket.accept();
                System.out.println("Client connected");
                new ServerThread(socket).serverStart();
            }

        } catch (IOException e){
            System.out.println("MainThread IOException error");
        }
    }
}
