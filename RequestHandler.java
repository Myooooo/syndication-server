/** 
* Distributed Systems Assignment 2 2021
* 
* RequestHandler.java
* This file implements the request handler thread for the aggregation server
*  
* @author Moyang Feng a1726464
* @date 04/10/2021
*/

package ds.assignment2;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;

import ds.assignment2.ATOMHelper;

public class RequestHandler implements Runnable {

    private Socket socket;          // the socket with active connection
    private int tid;                // thread ID

    /**
    * constructor
    */
    public RequestHandler(Socket socket, int tid) {
        // store active socket
        this.socket = socket;
        this.tid = tid;
    }

    /**
    * Print line to terminal with timestamp and thread ID
    */
    public void printlnTime(String msg) {
        ATOMHelper.printlnTime("[Thread " + tid + "] " + msg);
    }

    /**
    * Print error message to terminal with timestamp and thread ID
    */
    public void printErrTime(String msg) {
        ATOMHelper.printErrTime("[Thread " + tid + "] " + msg);
    }

    /**
    * @return thread ID
    */
    public int getID() {
        return tid;
    }

    /**
    * Override of Thread execution method
    */
    @Override
    public void run() {

        printlnTime("Request from " + socket.getRemoteSocketAddress());

        try {
            // write to server log and mark crashed flag to true
            File log = new File("./server.log");
            PrintWriter pw = new PrintWriter(new FileWriter(log));
            pw.println("crashed=1");
            pw.close();

            // create input and output buffer
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintStream output = new PrintStream(socket.getOutputStream());

            // set socket timeout to 5s
            socket.setSoTimeout(5000);

            // read from client
            String line = input.readLine();
            System.out.println("\t" + line);

            // check request content
            if(line == null || line.equals("")) {
                // request is empty
                output.println("HTTP/1.1 204 No Content");
            } else {
                // request is not empty, parse request line
                // method|resource|version
                String[] request = line.split(" ");

                // check request format
                if(request.length < 3) {
                    // wrong request format
                    output.println("HTTP/1.1 400 Bad Request");
                } else {
                    // split request line
                    String method = request[0];     // GET|PUT
                    String resource = "." + request[1];   // file path

                    // check request method
                    if(method.equals("GET")) {

                        // GET request from client
                        try {
                            File file = new File(resource);
                            Scanner scanner = new Scanner(file);

                            // read remaining header
                            line = input.readLine();
                            while(!line.isEmpty() && line != null) {
                                System.out.println("\t" + line);

                                // parse header and increment lamport clock
                                if(line.split(": ")[0].equals("Lamport-Clock")) {
                                    int get_clock = AggregationServer.clock.increment(Integer.parseInt(line.split(": ")[1]));
                                    System.out.println("\tUpdated Lamport Clock: " + Integer.toString(get_clock));
                                }

                                line = input.readLine();
                            }
                            
                            // send response
                            printlnTime("Sending Response...");
                            output.println("HTTP/1.1 200 OK");
                            output.println("Lamport-Clock: " + Integer.toString(AggregationServer.clock.getClock()));
                            output.println("\r\n");

                            // send file to client line by line
                            while (scanner.hasNextLine()) {
                                String fileLine = scanner.nextLine();
                                output.println(fileLine);
                            }
                            scanner.close();
                        } catch (FileNotFoundException e) {
                            // resource doesn't exist
                            printErrTime("File " + resource + " not found");
                            output.println("HTTP/1.1 500 Internal Server Error");
                        }

                    } else if (method.equals("PUT")) {

                        // make a backup of the original feed before proceeding
                        Path source = Paths.get(resource);
                        Path target = Paths.get(resource + ".bak");
                        try {
                            Files.deleteIfExists(target);
                            Files.copy(source, target);
                        } catch (IOException e) {
                            printErrTime("Backup failed");
                        }

                        // PUT request from content server
                        try {
                            // read file at requested path
                            File file = new File(resource);
                            PrintWriter writer = new PrintWriter(new FileWriter(file));
                            Boolean fileExist = file.exists();

                            // persistent connection
                            while(true) {
                                // read and parse Lamport clock
                                line = input.readLine();
                                System.out.println("\t" + line);
                                int put_clock = AggregationServer.clock.increment(Integer.parseInt(line.split(": ")[1]));

                                if(!fileExist) {
                                    // the ATOM feed is newly created
                                    output.println("HTTP/1.1 201 HTTP_CREATED");
                                } else {
                                    // the ATOM feed already exists
                                    output.println("HTTP/1.1 200 OK");
                                }

                                // send current lamport clock
                                output.println("Lamport-Clock: " + Integer.toString(AggregationServer.clock.getClock()));

                                // read remaining header
                                line = input.readLine();
                                while(!line.isEmpty() && line != null) {
                                    System.out.println("\t" + line);
                                    line = input.readLine();
                                }
                                
                                // write to file until timeout or empty
                                printlnTime("Receiving from content server...");
                                while((line = input.readLine()) != null) {
                                    if(!line.isEmpty()) writer.println(line);
                                }
                                writer.close();

                                // send response according to state
                                if(ATOMHelper.parseATOM(file) == -1) {
                                    // failed to parse XML
                                    output.println("HTTP/1.1 500 Internal Server Error");
                                    printErrTime("Empty/malformed XML");
                                }

                                // heartbeat mechanism check for connection every 12 seconds
                                try {
                                    Thread.sleep(12000);
                                } catch (InterruptedException e) {
                                    printErrTime("Thread interrupted");
                                }
                                if(input.readLine() == null) break;
                            }

                        } catch (IOException e) {
                            output.println("HTTP/1.1 500 Internal Server Error");
                            printErrTime("Failed writing to file");
                        }
                        
                    } else {

                        // unsupported request
                        output.println("HTTP/1.1 400 Bad Request");

                    }
                }
            }

            // close input buffer and socket
            input.close();
            socket.close();

            // successfully processed request, mark crashed flag to false in server log
            pw = new PrintWriter(new FileWriter(log));
            pw.println("crashed=0");
            pw.close();
        } catch (IOException e) {
            printErrTime("Failed to process request");
            e.printStackTrace();
        }

        printlnTime("Connection closed " + socket.getRemoteSocketAddress());
    }
}