/** 
* Distributed Systems Assignment 2 2021
* 
* ContentServer.java
* This file implements the content server
*  
* @author Moyang Feng a1726464
* @date 04/10/2021
*/

package ds.assignment2;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ContentServer {
    /**
    * Main function for the content server
    */
    public static void main(String args[]) {

        if (args.length < 2) {
            System.err.println("Usage: [server_url] [file_path]");
            System.exit(0);
        }

        // store host url and file path
        String url = args[0];
        String path = args[1];

        // number of retries and connection timeout in milliseconds
        int retries = 3;
        int timeout = 5000;
        Boolean connected = false;

        // lamport clock
        LamportClock clock = new LamportClock(0);

        try {
            // parse server URL
            String[] parts = url.split(":");
            String host;
            int port;

            // check URL format
            if(parts.length == 3) {
                // http://host:port
                URL aURL = new URL(url);
                host = aURL.getHost();
                port = aURL.getPort();
            } else {
                // host:port
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            }

            ATOMHelper.printlnTime("Connecting to host " + host + " on port " + Integer.toString(port));

            try {
                // connect to aggregation server on port
                Socket socket = null;
                SocketAddress socketAddress = new InetSocketAddress(host, port);

                // connect to server, retry until exceed limit
                while(retries > 0) {
                    try {
                        socket = new Socket();
                        socket.connect(socketAddress,timeout);
                        connected = true;
                    } catch (SocketTimeoutException e) {
                        // decrement retry count and retry after 3s
                        System.err.printf("[" + ATOMHelper.getCurrentTimeStamp() + "] Retrying after 3s (%d)...\n",retries);
                        retries--;
                        connected = false;
                        Thread.sleep(3000);
                    }

                    if(connected) {
                        ATOMHelper.printlnTime("Connected to server");
                        break;
                    }
                }

                // set socket timeout to 5s
                socket.setSoTimeout(5000);
                
                // create input and output buffer
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream output = new PrintStream(socket.getOutputStream());

                try {
                    // read file from input path
                    File textFile = new File(path);

                    // assemble plain text into ATOM feed
                    File xmlFile = ATOMHelper.assembleATOM(textFile);

                    if(xmlFile != null) {
                        // assembled successfully
                        Scanner scanner = new Scanner(xmlFile);
                        ATOMHelper.printlnTime("Sending Request...");
                        
                        // send request
                        output.println("PUT /atom.xml HTTP/1.1");
                        output.println("Lamport-Clock: " + Integer.toString(clock.getClock()));

                        // wait for server response
                        String response = input.readLine();
                        System.out.println("\t" + response);
                        
                        // read lamport clock in header and increment
                        response = input.readLine();
                        System.out.println("\t" + response);
                        clock.increment(Integer.parseInt(response.split(": ")[1]));

                        // send remaining request header
                        output.println("User-Agent: ATOMClient/1/0");
                        output.println("Content-Type: application/atom+xml");
                        output.println("Content-Length: " + Long.toString(xmlFile.length()));
                        output.println("\r\n");

                        // send XML file to server line by line
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            output.println(line);
                        }
                        scanner.close();
                    }

                } catch (FileNotFoundException e) {
                    ATOMHelper.printErrTime("File " + path + " not found");
                }

                input.close();
                output.close();
                socket.close();
                
            } catch (IOException e) {
                ATOMHelper.printErrTime("Connection failed");
            }

            ATOMHelper.printlnTime("Connection closed");

        } catch (Exception e) {
            ATOMHelper.printErrTime("Failed to parse input URL");
            System.err.println("Format: http://host:port | host:port");
        }   
    }
}