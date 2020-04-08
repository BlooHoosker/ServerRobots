package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Main {
    private static final String NAME = "Bruhpix";
    private static final int SERVER_KEY = 54621;
    private static final int CLIENT_KEY = 45328;
    private static final String ENDLN = "\u0007\b";
    private static final String CLIENT_RECHARGING = "RECHARGING\u0007\b";
    private static final String CLIENT_FULL_POWER = "FULL POWER\u0007\b";

    private static String GetCommand(String command){
        String res = "";
        for (int i = 0; i < command.length() - 2; i++){
            res += command.charAt(i);
        }
        return res;
    }

    private static short GetHash(String username){
        // Uživatelské jméno: Mnau!
        // ASCII reprezentace: 77 110 97 117 33
        // Výsledný hash: ((77 + 110 + 97 + 117 + 33) * 1000) % 65536 = 40784

        if (username == null || username.isBlank()){
            System.out.println("username is null or empty");
            throw new UnsupportedOperationException();
        }

        // Gets sum of char Ascii decimal values
        int usernameAsciiSum = 0;
        for (int i = 0; i < username.length() - 4; i++){
            usernameAsciiSum += username.charAt(i);
        }

        int hash = (usernameAsciiSum * 1000) % 65536;
        return (short) hash;
    }

    public static void main(String[] args) {

        try(Socket socket = new Socket(InetAddress.getByName("192.168.0.111"), 4242);
            InputStream clientIn = socket.getInputStream();
            BufferedReader clientInReader = new BufferedReader(new InputStreamReader(clientIn));
            OutputStream clientOut = socket.getOutputStream();
            PrintWriter clientOutWriter = new PrintWriter(new OutputStreamWriter(clientOut), true);){

            System.out.println("Robot " + NAME + " connecting");
            clientOutWriter.write(NAME + ENDLN);
            clientOutWriter.flush();



            String response = clientInReader.readLine();
            System.out.println(response);


            clientOutWriter.write(CLIENT_RECHARGING+CLIENT_FULL_POWER);
            clientOutWriter.flush();
            /*
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e){

            }

             */

            response = GetCommand(response);
            short code = (short) Integer.parseInt(response);
            short hash = GetHash(NAME);
            short codeServer = (short)((hash + SERVER_KEY) % 65536);
            if (code != codeServer){
                System.out.println("Codes dont match");
                return;
            }

            code = (short)((hash + CLIENT_KEY) % 65536);
            clientOutWriter.write(code + ENDLN);
            clientOutWriter.flush();



            response = clientInReader.readLine();
            System.out.println(response);
            // test Navai setup  ----------------------------------------------

            int x = -16;
            int y = -16;
            int direction = 0;
            int recharging = 0;
            while (true){
                response = clientInReader.readLine();
                System.out.println(response);
                if (response.equals("106 LOGOUT\u0007\b")){
                    break;
                } else if (response.equals("105 GET MESSAGE\u0007\b")){
                    if (x == 0 && y == 0){
                        clientOutWriter.write("Bruhpix is a nigger" +ENDLN);
                    } else {
                        clientOutWriter.write(ENDLN);
                    }
                    clientOutWriter.flush();
                    continue;
                }
                recharging = (int) (Math.random() * ((10 - 0) + 1));
                if (recharging == 0){
                    clientOutWriter.write(CLIENT_RECHARGING);
                    clientOutWriter.flush();
                    System.out.println(CLIENT_RECHARGING);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e){

                    }
                    clientOutWriter.write(CLIENT_FULL_POWER);
                    clientOutWriter.flush();
                    System.out.println(CLIENT_FULL_POWER);
                }

                if (response.equals("102 MOVE\u0007\b")){
                    if (direction == 0){
                        y++;
                    } else if (direction == 90){
                        y--;
                    } else if (direction == 45){
                        x++;
                    } else if (direction == 135){
                        x--;
                    }
                } else if (response.equals("103 TURN LEFT\u0007\b")){
                    if (direction == 0){
                        direction = 135;
                    } else {
                        direction -= 45;
                    }
                } else if (response.equals("104 TURN RIGHT\u0007\b")){
                    direction = (direction + 45) % 180;
                } else {
                    System.out.println("errorrrrrrrr");
                    break;
                }
                clientOutWriter.write("OK " + x + " " + y + ENDLN);
                clientOutWriter.flush();
                System.out.println("OK " + x + " " + y);
            }

        } catch (IOException e){
            System.out.println("Error");
        }

    }
}
