/**
 * ADE 1.0
 * (c) copyright HRILab (http://hrilab.org/)
 *
 * All rights reserved.  Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 * Last update: April 2010
 *
 * SocketHandler.java
 */
package com;

import java.io.*;

/**
Interface for ADE socket handling.
 */
public interface SocketHandler {

    /** Check whether the socket is open.
     * @return true if open/ false otherwise */
    public boolean isOpen();

    /** Set open/closed state of the socket.
     * @param b boolean indicating open state of socket */
    public void setOpen(boolean b);

    /** Close socket. */
    public void close();

    /** Receives incoming string from the socket. Note that the socket created
     * by the <tt>ClientComponentThread</tt> class has a socket timeout set, which
     * we need to catch. Also, sending back an empty string will cause the
     * <tt>ClientComponentThread</tt> to skip the parseMessage method.
     * @return the received string as an object */
    public Object receiveMessage() throws IOException;

    /** Send the first item in toSend list (only if needToSend is true). */
    public void sendMessage() throws IOException;

    /** Send the message immediately, rather than placing it in the send
     * queue.
     * @param mesg the message to send */
    public void sendMessageNow(String mesg) throws IOException;

    /** Allows us to update our SocketComponent and/or customize responses to
     * whatever we receive from the socket. */
    public void parseMessage(Object o);
}
