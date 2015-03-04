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

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;
import javax.swing.JFileChooser;

public class ADELogHelper {

    public static final String LOG_START_TIME_FLAG = "START TIME";
    public static final String LOG_END_TIME_FLAG = "END TIME";
    public static final String LOG_SYNC_TIME_FLAG = "SYNC TIME";
    public static final String TIME_STAMP_SEPARATOR = " ";
    private String logFileName;
    private boolean run;
    private int positionTime;
    private int positionIndex;
    private UUID credentialsID; // a unique ID, generated each time that the
    //    component is run, that is passed to ADELogPlaybackVis, so that the
    //    visualization can control the log-playback (typically, ADE uses
    //    the component itself as credentials, but this is impractical for 
    //    passing such credentials to the log playback visualization, 
    //    because some components cannot be serialized).
    ArrayList<Integer> times;
    ArrayList<String> texts = new ArrayList<String>();

    public ADELogHelper() {
        this.credentialsID = UUID.randomUUID();

        this.run = true; // once logging is turned on, start running immediately by default

        this.times = new ArrayList<Integer>(); // adjusted so that first time is 0
        this.texts = new ArrayList<String>();
    }

    public static String promptForFile(Component parent, String path) {
        if (path == null) {
            path = new File("").getAbsolutePath(); // abstract path to current directory
        }

        JFileChooser chooser = new JFileChooser(path);
        int acceptCode = chooser.showOpenDialog(parent);
        if (acceptCode == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        } else {
            return null;
        }
    }

    public UUID getCredentialsUUID() {
        return credentialsID;
    }

    public boolean hasPlaybackLogFile() {
        return (logFileName != null);
    }

    public void openPlaybackLogFile(String logFileName) throws IOException, RemoteException {
        if (logFileName == null) {
            // must have canceled.
            throw new IOException("No file selected");
        }

        this.logFileName = logFileName;

        times = new ArrayList<Integer>();
        texts = new ArrayList<String>();
        ArrayList<Long> tempTimes = new ArrayList<Long>();

        Scanner in = new Scanner(new File(logFileName));
        while (in.hasNextLine()) {
            String tmp = in.nextLine();
            int stampSeparatatorIndex = tmp.indexOf(TIME_STAMP_SEPARATOR);
            if (stampSeparatatorIndex < 0) {
                throw new IOException("Invalid file format.  Expected time stamp, "
                        + "followed by space, on each new line "
                        + "(as defined by System.getProperty(\"line.separator\")");
            }

            String stamp = tmp.substring(0, stampSeparatatorIndex);
            String content = tmp.substring(stampSeparatatorIndex + TIME_STAMP_SEPARATOR.length());

            try {
                long stampTime = Long.parseLong(stamp);
                tempTimes.add(stampTime);
                texts.add(content);
            } catch (NumberFormatException e) {
                throw new IOException("Could not parse stamp in line\"" + tmp + "\".");
            }
        }

        // having read in the "full" times, adjust them
        long minTime = tempTimes.get(0);
        for (Long fullTimeStamp : tempTimes) {
            long diff = fullTimeStamp - minTime;
            int intDiff = (int) diff;
            times.add(intDiff);
        }

        positionTime = 0;
        positionIndex = 0;
    }

    public String getCurrentFileName() {
        return logFileName;
    }

    public int getPlaybackPosition() {
        return positionTime;
    }

    public int maxPlaybackPosition() {
        if (times.size() > 0) {
            return times.get(times.size() - 1);
        } else {
            return 0;
        }
    }

    public void setPlaybackPosition(int position) {
        this.positionTime = position;

        // search through the times to find the first index greater than the position
        for (int i = 0; i < this.times.size(); i++) {
            if (times.get(i) > position) {
                this.positionIndex = i;
                return;
            }
        }
        // if searched all the way, and found nothing, then must be essentially out of bounds
        this.positionIndex = this.times.size();
    }

    public int getPlaybackLineIndex() {
        return this.positionIndex;
    }

    public String getContents(int index) {
        if (this.times.size() > index) {
            return this.texts.get(index);
        } else {
            return "";
        }

    }

    public int getTime(int index) {
        if (this.times.size() > index) {
            return this.times.get(index);
        } else {
            return maxPlaybackPosition();
        }
    }

    public void advancePlaybackLineIndex() {
        this.positionIndex++;
    }

    public boolean playbackIndexWithinBounds() {
        return playbackIndexWithinBounds(this.positionIndex);
    }

    public boolean playbackIndexWithinBounds(int index) {
        return ((index >= 0) && (index < this.times.size()));
    }

    public void advancePlaybackTime(int elapsedAlready) {
        this.positionTime = this.positionTime + elapsedAlready;
    }

    public void setIsRunning(boolean run) {
        this.run = run;
    }

    public boolean getIsRunning() {
        return run;
    }
}
