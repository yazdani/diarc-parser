/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 *
 * Copyright 1997-2013 Matthias Scheutz and the HRILab Development Team
 * All rights reserved.  For information or questions, please contact
 * the director of the HRILab, Matthias Scheutz, at mscheutz@gmail.com
 * 
 * Redistribution and use of all files of the ADE package, in source and
 * binary forms with or without modification, are permitted provided that
 * (1) they retain the above copyright notice, this list of conditions
 * and the following disclaimer, and (2) redistributions in binary form
 * reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR ANY
 * OF THE CONTRIBUTORS TO THE ADE PROJECT BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.

 * Note: This license is equivalent to the FreeBSD license.
 */
package ade;

import java.io.RandomAccessFile;
import java.util.*;

/*
http://forums.sun.com/thread.jspa?threadID=572557

Problem:
Let there be a file containing thousands of lines. We want to read last N lines. Similar to functionality provided by "tail" command.

Constraints:
1 Minimize the number of file reads.
Avoid reading the complete file to get last few lines.
2 Minimize the JVM in-memory usage.
Avoid storing the complete file info in in-memory.

Approach:
Read a chunk of characters from end of file. One chunk should contain multiple lines. Reverse this chunk and extract the lines. Repeat this until you get required number of last N lines. In this way we read and store only the required part of the file.

Below is a utility program:
 */

/** Helper class for getting information from files. */
public class ADEFileHelper {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String dir = "C:\\";
        String fileName = args[0];
        int lineCount = Integer.parseInt(args[1]);
        ADEFileHelper.tail(dir + fileName, lineCount);
    }

    public static ArrayList tail(String fileName, int lineCount) {
        return tail(fileName, lineCount, 2000);
    }

    /**
     * Given a byte array this method:
     * a. creates a String out of it
     * b. reverses the string
     * c. extracts the lines
     * d. characters in extracted line will be in reverse order,
     *    so it reverses the line just before storing it
     *
     *  On extracting required number of lines, this method returns TRUE,
     *  Else it returns FALSE.
     *
     * @param bytearray
     * @param lineCount
     * @param lastNlines
     * @return
     */
    private static boolean parseLinesFromLast(byte[] bytearray, int lineCount, ArrayList lastNlines) {
        String lastNChars = new String(bytearray);
        StringBuilder sb = new StringBuilder(lastNChars);
        lastNChars = sb.reverse().toString();
        StringTokenizer tokens = new StringTokenizer(lastNChars, "\n");
        while (tokens.hasMoreTokens()) {
            StringBuilder sbLine = new StringBuilder((String) tokens.nextToken());
            lastNlines.add(sbLine.reverse().toString());
            if (lastNlines.size() == lineCount) {
                return true; //indicates we got 'lineCount' lines
            }
        }
        return false; //indicates didn't read 'lineCount' lines
    }

    /**
     * Reads last N lines from the given file. File reading is done in chunks.
     *
     * Constraints:
     * 1 Minimize the number of file reads -- Avoid reading the complete file
     * to get last few lines.
     * 2 Minimize the JVM in-memory usage -- Avoid storing the complete file
     * info in in-memory.
     *
     * Approach: Read a chunk of characters from end of file. One chunk should
     * contain multiple lines. Reverse this chunk and extract the lines.
     * Repeat this until you get required number of last N lines. In this way
     * we read and store only the required part of the file.
     *
     * 1 Create a RandomAccessFile.
     * 2 Get the position of last character using (i.e length-1). Let this be curPos.
     * 3 Move the cursor to fromPos = (curPos - chunkSize). Use seek().
     * 4 If fromPos is less than or equal to ZERO then go to step-5. Else go to step-6
     * 5 Read characters from beginning of file to curPos. Go to step-9.
     * 6 Read 'chunksize' characters from fromPos.
     * 7 Extract the lines. On reading required N lines go to step-9.
     * 8 Repeat step 3 to 7 until
     *			a. N lines are read.
     *		OR
     *			b. All lines are read when num of lines in file is less than N.
     * Last line may be a incomplete, so discard it. Modify curPos appropriately.
     * 9 Exit. Got N lines or less than that.
     *
     * @param fileName
     * @param lineCount
     * @param chunkSize
     * @return the last lineCount lines of the file
     */
    public static ArrayList tail(String fileName, int lineCount, int chunkSize) {
        try {
            RandomAccessFile raf = new RandomAccessFile(fileName, "r");
            ArrayList lastNlines = new ArrayList();
            int delta;
            long curPos = raf.length() - 1;
            long fromPos;
            byte[] bytearray;
            while (true) {
                fromPos = curPos - chunkSize;
                System.out.println(curPos);
                System.out.println(fromPos);
                if (fromPos <= 0) {
                    raf.seek(0);
                    bytearray = new byte[(int) curPos];
                    raf.readFully(bytearray);
                    parseLinesFromLast(bytearray, lineCount, lastNlines);
                    break;
                } else {
                    raf.seek(fromPos);
                    bytearray = new byte[chunkSize];
                    raf.readFully(bytearray);
                    if (parseLinesFromLast(bytearray, lineCount, lastNlines)) {
                        break;
                    }
                    delta = ((String) lastNlines.get(lastNlines.size() - 1)).length();
                    lastNlines.remove(lastNlines.size() - 1);
                    curPos = fromPos + delta;
                }
            }
            ListIterator it = lastNlines.listIterator();
            while (it.hasNext()) {
                System.out.println(it.next());
            }
            return lastNlines;
        } catch (Exception e) {
            System.err.println("Exception during tail on file: " + e);
            return null;
        }
    }
}
