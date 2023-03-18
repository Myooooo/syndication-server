/** 
* GETClient.java
* This file implements the GET client
*/

package ds.assignment2;

import java.io.*;
import java.net.*;

public class GETClient {
    /**
    * Main function for the GET client
    */
    public static void main(String args[]) {

        // use input URL if exist, default localhost:4567
        String url = (args.length == 0) ? "127.0.0.1:4567" : args[0];

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

            //ATOMHelper.printlnTime("Connecting to host " + host + " on port " + Integer.toString(port));

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
                        //ATOMHelper.printlnTime("Connected to server\n");
                        break;
                    }
                }

                // set socket timeout to 5s
                socket.setSoTimeout(5000);
                
                // create input and output buffer
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream output = new PrintStream(socket.getOutputStream());
                
                // send request header
                //ATOMHelper.printlnTime("Sending Request...");
                output.println("GET /atom.xml HTTP/1.1");
                output.println("Lamport-Clock: " + Integer.toString(clock.getClock()));
                output.println("\r\n");

                // create temporary file
                File tmpFile = File.createTempFile("feed-", ".xml");
                PrintWriter writer = new PrintWriter(new FileWriter(tmpFile));
                tmpFile.deleteOnExit();

                // read response header and print to terminal
                String line = input.readLine();
                while(!line.isEmpty() && line != null) {
                    System.out.println("\t" + line);

                    // parse header and increment lamport clock
                    if(line.split(": ")[0].equals("Lamport-Clock")) {
                        clock.increment(Integer.parseInt(line.split(": ")[1]));
                    }

                    line = input.readLine();
                }

                // read and save response body to the temporary file
                while((line = input.readLine()) != null) {
                    if(!line.isEmpty()) writer.println(line);
                }
                writer.close();

                // parse ATOM feed and print to terminal
                if(ATOMHelper.parseATOM(tmpFile) == -1) ATOMHelper.printErrTime("Failed to parse response");;
                
                input.close();
                output.close();
                socket.close();
            } catch (IOException e) {
                ATOMHelper.printErrTime("Connection failed");
            }

            //System.out.println("\n[" + ATOMHelper.getCurrentTimeStamp() + "] Connection closed");

        } catch (Exception e) {
            ATOMHelper.printErrTime("Failed to parse input URL");
            System.err.println("Format: http://host:port | host:port");
        }   
    }
}
