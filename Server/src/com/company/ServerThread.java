package com.company;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ServerThread extends Thread {

    private Socket socket;
    private final int SERVER_KEY = 54621;
    private final int CLIENT_KEY = 45328;

    private final String ENDLN = "\u0007\b";
    private final String SERVER_MOVE = "102 MOVE\u0007\b";
    private final String SERVER_TURN_LEFT = "103 TURN LEFT\u0007\b";
    private final String SERVER_TURN_RIGHT = "104 TURN RIGHT\u0007\b";
    private final String SERVER_PICK_UP = "105 GET MESSAGE\u0007\b";
    private final String SERVER_LOGOUT = "106 LOGOUT\u0007\b";
    private final String SERVER_OK = "200 OK\u0007\b";
    private final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED\u0007\b";
    private final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR\u0007\b";
    private final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR\u0007\b";

    private final String CLIENT_RECHARGING = "RECHARGING\u0007\b";
    private final String CLIENT_FULL_POWER = "FULL POWER\u0007\b";

    private final int CLIENT_NORMAL = 12;
    private final int CLIENT_MESSAGE = 100;
    private final int TIMEOUT = 1;
    private final int TIMEOUT_RECHARGING = 5;

    /* ERROR CODES
        -1 = bad behavior error
        0 = OK
        1 = Syntax error
        2 = Timeout
        3 = Logic error
    */

    public ServerThread(Socket socket){
        this.socket = socket;
    }

    // Based on error code writes error message to client
    private void ErrorStates(int state, PrintWriter serverOutWriter){
        if (state == 1) {
            // Syntax error
            serverOutWriter.write(SERVER_SYNTAX_ERROR);
            System.out.println("SERVER_SYNTAX_ERROR");
        } else if (state == 2) {
            // Timeout, connection terminated
            //System.out.println("Timeout");
        } else if (state == 3) {
            // Recharging messed up
            serverOutWriter.write(SERVER_LOGIC_ERROR);
            //System.out.println("SERVER_LOGIC_ERROR");
        } else {
            // Login failed one one way or another
            serverOutWriter.write(SERVER_LOGIN_FAILED);
            //System.out.println("SERVER_LOGIN_FAILED");
        }
        serverOutWriter.flush();
    }

    // Sends move message to client based on which action its supposed to do
    private void MoveStates(int state, PrintWriter serverOutWriter){
        if (state == 1) {
            serverOutWriter.write(SERVER_TURN_RIGHT);
            //System.out.println("SERVER_TURN_RIGHT");
        } else if (state == -1){
            serverOutWriter.write(SERVER_TURN_LEFT);
            //System.out.println("SERVER_TURN_LEFT");
        } else{
            serverOutWriter.write(SERVER_MOVE);
            //System.out.println("SERVER_MOVE");
        }
        serverOutWriter.flush();
    }

    // Returns verification code generated from clients username
    private int GetHash(String username){
        // Uživatelské jméno: Mnau!
        // ASCII reprezentace: 77 110 97 117 33
        // Výsledný hash: ((77 + 110 + 97 + 117 + 33) * 1000) % 65536 = 40784

        if (username == null || username.isBlank()){
            System.out.println("username is null or empty");
            throw new UnsupportedOperationException();
        }

        // Gets sum of char Ascii decimal values
        int usernameAsciiSum = 0;
        for (int i = 0; i < username.length(); i++){
            usernameAsciiSum += username.charAt(i);
        }

        int hash = (usernameAsciiSum * 1000) % 65536;
        return hash;
    }

    // Returns command as string without \a\b
    private String GetMessage(String command){
        String res = "";
        for (int i = 0; i < command.length() - 2; i++){
            res += command.charAt(i);
        }
        return res;
    }

    // Gets coordinates + error code if something goes wrong
    private Pair<Pair<Integer, Integer>, Integer> GetOK(String command){

        Pair<Integer, Integer> position = new Pair<>(0,0);
        Scanner scanner = new Scanner(GetMessage(command));
        int first;
        int second;

        scanner.useDelimiter(" ");
        try{
            // Getting values from command based on format OK X Y
            String ok = scanner.next();
            if (!ok.equals("OK")){
                return new Pair<>(position, 1);
            }
            first = scanner.nextInt();
            second = scanner.nextInt();
            scanner.useDelimiter("");
            if (scanner.hasNext()){
                return new Pair<>(position, 1);
            }
        // If format doesnt match it will catch it as an exception
        } catch (InputMismatchException e) {
            return new Pair<>(position, 1);
        }
        return new Pair<>(new Pair<>(first, second), 0);
    }

    private Pair<String, Integer> CommandFromBuffer(BufferedReader serverInReader, int length){
        StringBuilder response = new StringBuilder();
        boolean bell = false;
        char charRead;
        try{
            int i = 0;
            for(i = 1; i <= length; i++){
                charRead = (char)serverInReader.read();
                response.append(charRead);
                // checks if the char is \a if yes sets flag
                if (charRead == '\u0007'){
                    bell = true;
                } else {
                    // if bell is true that means the cycle before there was \a and now we check \b
                    if (charRead == '\b' && bell) {
                        return new Pair<>(response.toString(), 0);
                    } else {
                        bell = false;
                    }
                }
                // Optimisation, if the last two chars arent \a\b then returns syntax error
                if ((i == length - 1 && charRead != '\u0007') || (i == length && charRead != '\b') ){
                    break;
                }
            }
            // Timeout
        } catch (SocketTimeoutException e){
            return new Pair<>("", 2);
        } catch (IOException e){
            System.out.println("CommandToBuffer IO exception");
            e.printStackTrace();
            return new Pair<>("", -1);
        }

        return new Pair<>("", 1);
    }

    // Receives a command from client and checks it for correctness
    private Pair<String, Integer> GetCommand(BufferedReader serverInReader, int length) {
        while (true){
            Pair<String, Integer> commandStat = CommandFromBuffer(serverInReader,length);
            if (commandStat.getKey().equals(CLIENT_RECHARGING)) {
                int state = Recharging(serverInReader);
                if (state != 0){
                    return new Pair<>("", state);
                }
                continue;
            }
            return commandStat;
        }
    }

    // Receives coordinates from client and parses them + error if something goes wrong
    private Pair<Pair<Integer, Integer>, Integer> ReceiveCoordinates(BufferedReader serverInReader){
        Pair<String, Integer> commandState;
        Pair<Pair<Integer,Integer>, Integer> position;

        // Returns coordinates + error state
        commandState = GetCommand(serverInReader, CLIENT_NORMAL);
        if (commandState.getValue() != 0) {
            return new Pair<>(new Pair<>(0, 0), commandState.getValue());
        }

        // Parsing position coordinates
        position = GetOK(commandState.getKey());
        if (position.getValue() != 0) {
            return new Pair<>(new Pair<>(0, 0), 1);
        }

        return position;
    }

    // Method used for clients logging in, returns error code
    private int Login(BufferedReader serverInReader, PrintWriter serverOutWriter){
        String username;
        String command;
        int hash;
        int code;
        int clientCode;
        Pair<String, Integer> commandState;

        try{
            // Sets socket timeout
            this.socket.setSoTimeout(1000*TIMEOUT);

            // Reads command and checks correctness
            commandState = GetCommand(serverInReader, CLIENT_NORMAL);
            if (commandState.getValue() != 0){
                return commandState.getValue();
            }

            // Gets code and sends it
            username = GetMessage(commandState.getKey());
            hash = GetHash(username);
            System.out.println("Robot name: " + username);

            // Vysledny kod: (40784 + 54621) % 65536 = 29869
            code = (hash + SERVER_KEY) % 65536;
            serverOutWriter.write(code + ENDLN);
            serverOutWriter.flush();

            // Gets Confirmation from client
            commandState = GetCommand(serverInReader, CLIENT_NORMAL);
            if (commandState.getValue() != 0){
                return commandState.getValue();
            }

            command = GetMessage(commandState.getKey());
            // Tries to parse string to int
            try{
                clientCode = Integer.parseInt(command);
                if (command.length() > 5 || clientCode >= 65536 ) {
                    return 1;
                }
            } catch (NumberFormatException e){
                return 1;
            }

            // Cod klienta: (40784 + 45328) % 65536 = 20576
            code = (hash + CLIENT_KEY) % 65536;
            if (code != clientCode){
                return -1;
            }
        } catch (IOException e){
            System.out.println("ServerThread IO exception");
            return -1;
        }
        System.out.println("Robot: " + username + " logged in");
        return 0;
    }

    // Controls robots recharging process, returns error code
    private int Recharging(BufferedReader serverInReader){

        Pair<String, Integer> CommandState;
        //System.out.println("RECHARGING");

        try{
            // Setting recharging timeout
            this.socket.setSoTimeout(1000*TIMEOUT_RECHARGING);

            // Waits for FULLPOWER signal

            //CommandState = GetCommand(serverInReader, CLIENT_NORMAL);
            CommandState = CommandFromBuffer(serverInReader, CLIENT_NORMAL);
            if(CommandState.getValue() != 0) {
                return CommandState.getValue();
            }

            // if the next command isnt fullpower then logic fail
            if (!CommandState.getKey().equals(CLIENT_FULL_POWER)){
                return 3;
            }
            this.socket.setSoTimeout(1000*TIMEOUT);
        } catch (IOException e){
            System.out.println("Recharging socket exception");
            return -1;
        }
        //System.out.println("FULLPOWER");
        return 0;

    }

    // Main servers function
    public void run(){
        try(InputStream serverIn = socket.getInputStream();
            BufferedReader serverInReader = new BufferedReader(new InputStreamReader(serverIn));
            OutputStream serverOut = socket.getOutputStream();
            PrintWriter serverOutWriter = new PrintWriter(new OutputStreamWriter(serverOut), true);){

            Pair<String, Integer> commandState;
            Pair<Pair<Integer,Integer>, Integer> position;
            Pair<Pair<Integer,Integer>, Integer> positionOrigin = new Pair<>(new Pair<>(0,0), 0);
            NavigationAI navAI;

            System.out.println("Robot connecting to server.");

            // Client tries to log to the server ============================================================================================================================================================================================================
            int state = Login(serverInReader, serverOutWriter);
            if (state != 0) {
                ErrorStates(state,serverOutWriter);
                socket.close();
                return;
            }
            serverOutWriter.write(SERVER_OK);
            serverOutWriter.flush();

            // Setup NavAI ============================================================================================================================================================================================================
            boolean navSet = false;
            while(true){
                // First two moves to determine direction and position
                serverOutWriter.write(SERVER_MOVE);
                serverOutWriter.flush();

                position = ReceiveCoordinates(serverInReader);
                //System.out.println("Position: " + position.getKey().getKey()+ " " + position.getKey().getValue());
                if (position.getValue() != 0){
                    ErrorStates(position.getValue(), serverOutWriter);
                    socket.close();
                    return;
                }

                // First we get the original position and then position after making a step. To determine direction
                if (!navSet){
                    navSet = true;
                    positionOrigin = position;
                    continue;
                } else if (position.getKey().getKey().equals(positionOrigin.getKey().getKey()) && position.getKey().getValue().equals(positionOrigin.getKey().getValue())){
                    continue;
                }
                break;
            }

            // Setting initial direction for the robot
            navAI = new NavigationAI(position.getKey());
            navAI.SetDirection(positionOrigin.getKey(), position.getKey());

            // Work loop controlling the robot getting him to 2,2 ========================================================================================================================================
            int move = 0;
            while(true) {
                //Generating next move from NavAI
                move = navAI.Move(position.getKey());
                if (move == 0) {
                    break;
                }

                // Sending command based on next command generated
                MoveStates(move, serverOutWriter);
                // Waiting for new coordinates
                position = ReceiveCoordinates(serverInReader);
                //System.out.println("Position: " + position.getKey().getKey()+ " " + position.getKey().getValue());
                if (position.getValue() != 0) {
                    ErrorStates(position.getValue(), serverOutWriter);
                    socket.close();
                    return;
                }
            }
            // Search loop ============================================================================================================================================================================================================
            while(true) {
                // Generating next move from NavAI
                move = navAI.Search(position.getKey());
                // If it arrived to next tile then tries to pick up message
                if (move == 0) {
                    serverOutWriter.write(SERVER_PICK_UP);
                    serverOutWriter.flush();
                    //System.out.println("SERVER_PICK_UP");
                    // Gets the message
                    commandState = GetCommand(serverInReader, CLIENT_MESSAGE);
                    if (commandState.getValue() != 0){
                        ErrorStates(commandState.getValue(), serverOutWriter);
                        socket.close();
                        return;
                    }

                    // Checks if message contains something
                    if(commandState.getKey().equals(ENDLN)){
                        continue;
                    } else {
                        break;
                    }
                }

                if (move == 3){
                    break;
                }

                // Sending command based on next command generated
                MoveStates(move, serverOutWriter);
                // Waiting for new coordinates
                position = ReceiveCoordinates(serverInReader);
                //System.out.println("Position: " + position.getKey().getKey()+ " " + position.getKey().getValue());
                if (position.getValue() != 0) {
                    ErrorStates(position.getValue(), serverOutWriter);
                    socket.close();
                    return;
                }

            }

            System.out.println("====================================================================");
            serverOutWriter.write(SERVER_LOGOUT);
            serverOutWriter.flush();
            socket.close();
        } catch (IOException e){
            System.out.println("ServerThread IOException error");
        }


    }

}
