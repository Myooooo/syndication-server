/** 
* ATOMHelper.java
* This file implements the helper functions on ATOM files
*/

package ds.assignment2;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Scanner;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;

public class ATOMHelper {

    /**
    * Obtain current timestamp
    * 
    * @return current timestamp formatted into string
    */
    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
    }

    /**
    * Print line to terminal with timestamp
    */
    public static void printlnTime(String msg) {
        System.out.println("[" + getCurrentTimeStamp() + "] " + msg);
    }

    /**
    * Print error message to terminal with timestamp
    */
    public static void printErrTime(String msg) {
        System.err.println("[" + getCurrentTimeStamp() + "] Error: " + msg);
    }

    /**
    * Parse ATOM XML feed to plain text and print to terminal
    * 
    * @param file the ATOM XML file to be parsed
    * @return status code, 0 for success, -1 for error
    */
    public static int parseATOM(File file) {
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(file));

            // get entries, authors and links
            @SuppressWarnings("unchecked")
            List<SyndEntry> entries = feed.getEntries();
            @SuppressWarnings("unchecked")
            List<SyndPerson> authors = feed.getAuthors();
            @SuppressWarnings("unchecked")
            List<SyndLink> links = feed.getLinks();

            // print feed info
            System.out.println("\n\tTitle: " + feed.getTitle());
            System.out.println("\tSubtitle: " + feed.getDescription());
            System.out.println("\tLink: " + links.get(0).getHref());
            System.out.println("\tUpdated: " + feed.getPublishedDate().toString());
            System.out.println("\tAuthor: " + authors.get(0).getName());
            System.out.println("\tID: " + feed.getUri());
            
            // print entries
            int count = 1;
            for(SyndEntry entry:entries) {
                System.out.printf("\n\t[Entry %d]\n", count);
                System.out.println("\tTitle: " + entry.getTitle());
                System.out.println("\tLink: " + entry.getLink());
                System.out.println("\tID: " + entry.getUri());
                System.out.println("\tUpdated: " + entry.getUpdatedDate().toString());
                System.out.println("\tSummary: " + entry.getDescription().getValue());
                count ++;
            }
            System.out.println("\n");

            return 0;

        } catch (Exception e) {
            printErrTime("Failed to parse ATOM feed");
            e.printStackTrace();
            return -1;
        }
    }

    /**
    * Assemble a plain text file into an ATOM XML feed
    *
    * @param file the text file to be converted
    * @return The XML assembled
    */
    public static File assembleATOM(File file) {
        try {
            // create feed object
            SyndFeed feed = new SyndFeedImpl();
            feed.setFeedType("atom_1.0");

            // create list for entries
            List<SyndEntry> entries = new ArrayList<SyndEntry>();
            int index = -1;     // index of current entry, -1 for feed

            // date formatter
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

            // loop through each line of the input file
            Scanner scanner = new Scanner(file);
            String line;        // current line
            String element;     // element of the line
            String content;     // content of the line

            while (scanner.hasNextLine()) {
                line = scanner.nextLine();

                // extract element and content using the first : sign
                element = line.split(":")[0];
                content = line.substring(line.indexOf(":") + 1);

                if(element.equals("title")) {

                    if(index == -1) {
                        // feed
                        feed.setTitle(content);
                    } else {
                        // entry
                        entries.get(index).setTitle(content);
                    }

                } else if(element.equals("subtitle")) {

                    feed.setDescription(content);

                } else if(element.equals("link")) {

                    if(index == -1) {
                        // feed
                        feed.setLink(content);
                    } else {
                        // entry
                        entries.get(index).setLink(content);
                    }

                } else if(element.equals("updated")) {

                    if(index == -1) {
                        // feed
                        feed.setPublishedDate(dateFormat.parse(content));
                    } else {
                        // entry
                        entries.get(index).setUpdatedDate(dateFormat.parse(content));
                    }

                } else if(element.equals("author")) {

                    // wrap name with author
                    List<SyndPerson> authors = new ArrayList<SyndPerson>();
                    SyndPerson author = new SyndPersonImpl();
                    author.setName(content);
                    authors.add(author);

                    if(index == -1) {
                        // feed
                        feed.setAuthors(authors);
                    } else {
                        // entry
                        entries.get(index).setAuthors(authors);
                    }

                } else if(element.equals("id")) {

                    if(index == -1) {
                        // feed
                        feed.setUri(content);
                    } else {
                        // entry
                        entries.get(index).setUri(content);
                    }

                } else if(element.equals("entry")) {

                    // create a new entry and add to the entry list
                    entries.add(new SyndEntryImpl());
                    index ++;

                } else if(element.equals("summary")) {

                    SyndContent description = new SyndContentImpl();
                    description.setValue(content);
                    entries.get(index).setDescription(description);

                } else {

                    // unsupported element
                    System.out.println("Unsupported element: " + element);

                }
            }
            scanner.close();

            // add entries to the feed
            feed.setEntries(entries);
            
            // create temporary file
            File tmpFile = File.createTempFile("feed-", ".xml");
            tmpFile.deleteOnExit();

            // write feed to temporary file
            SyndFeedOutput feedOut = new SyndFeedOutput();
            feedOut.output(feed,tmpFile);

            return tmpFile;

        } catch (Exception e) {
            printErrTime("Failed to create ATOM feed");
            e.printStackTrace();
            return null;
        }
    }
}
