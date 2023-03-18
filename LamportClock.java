/** 
* Distributed Systems Assignment 2 2021
* 
* LamportClock.java
* This file implements the Lamport Clock instance
*  
* @author Moyang Feng a1726464
* @date 04/10/2021
*/

package ds.assignment2;

public class LamportClock {

    private int latest_clock;       // stores the lastest clock

    /**
    * constructor
    */
    public LamportClock(int clock) {
        latest_clock = clock;
    }

    /**
    * Compare local clock with request clock and increment
    * 
    * @param request_clock request Lamport clock
    * @return incremented Lamport clock
    */
    public int increment(int request_clock) {
        latest_clock = Integer.max(latest_clock, request_clock);
        latest_clock ++;
        return latest_clock;
    }

    /**
    * Obtain current Lamport clock
    * 
    * @return current Lamport clock
    */
    public int getClock() {
        return latest_clock;
    }

}