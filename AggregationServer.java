/** 
* Distributed Systems Assignment 2 2021
* 
* AggregationServer.java
* This file implements the aggregation server
*  
* @author Moyang Feng a1726464
* @date 04/10/2021
*/

package ds.assignment2;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;  
import java.util.concurrent.Executors;

public class AggregationServer {

    public static LamportClock clock = new LamportClock(0);    // Lamport Clock

    /**
    * Main function for the aggregation server
    */
    public static void main(String args[]) {

        // use input port if exist, default port 4567
        int port = (args.length == 0) ? 4567 : Integer.parseInt(args[0]);

        // create thread pool of size 10
        ExecutorService executer = Executors.newFixedThreadPool(10);

        // record thread ID
        int tid = 1;

        // check if server was crashed and needs recovery
        try {
            File log = new File("./server.log");
            Scanner scanner = new Scanner(log);
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split("=");
                if(parts[0].equals("crashed") && parts[1].equals("1")) {
                    // server was crashed, restore feed from backup
                    Path source = Paths.get("./atom.xml.bak");
                    Path target = Paths.get("./atom.xml");
                    try {
                        Files.deleteIfExists(target);
                        Files.copy(source, target);
                    } catch (IOException e) {
                        ATOMHelper.printErrTime("Restore Failed");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            ATOMHelper.printErrTime("Feed doesn't exist");
        }

        // create server socket on port
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            ATOMHelper.printlnTime("Server ready");

            // listen to client requests
            ATOMHelper.printlnTime("Waiting for connection...");
            while (true) {
                try {
                    // accept connection
                    Socket socket = serverSocket.accept();

                    // create new server thread to handle request
                    RequestHandler server = new RequestHandler(socket,tid);
                    executer.execute(server);
                    tid ++;
                } catch(IOException e) {
                    ATOMHelper.printErrTime("Failed to accept connection");
                }
            }
        } catch (IOException e) {
            ATOMHelper.printErrTime("Failed to create server socket");
        }
    }
}