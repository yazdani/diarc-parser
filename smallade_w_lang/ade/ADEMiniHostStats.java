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

import com.hostmonitor.*;
import java.io.Serializable;
//import java.util.Iterator;

/**
Utility class for passing minimal (and transient) information about
a host (as specified by a {@link ade.ADEHostInfo ADEHostInfo} object)
between components. Mostly for use to pass information from a {@link
com.hostmonitor.HostMonitorComponent HostMonitorComponent} to an {@link
ade.ADERegistryImpl ADERegistryImpl}, gathered for a particular
host using the appropriate subclass of {@link ade.ADEHostStatus
ADEHostStatus}.
*/
public class ADEMiniHostStats implements Serializable {

    private static String prg = "ADEMiniHostStats";
    public String ip;
    public boolean available;
    public double cpuload;
    public int memavail;

    /** Constructor that allows manual filling in of data. */
    public ADEMiniHostStats() {
        this(new String(), false, 1.0, 0);
    }

    /** Constructor with only the ip. */
    public ADEMiniHostStats(String i) {
        this(i, false, 1.0, 0);
    }

    /** Constructor with all information parameterized. */
    public ADEMiniHostStats(String i, boolean a, double c, int m) {
        ip = i;
        available = a;
        cpuload = c;
        memavail = m;
    }

    /** Print the data contained herein. */
    public void print() {
        System.out.println(toString());
    }

    /** Return the string representation of this object. */
    public String toString() {
        StringBuffer sb = new StringBuffer(prg);

        sb.append(": ");
        sb.append(ip);
        sb.append("\n\tAvailable: ");
        sb.append(available);
        sb.append("\n\tCPU load:  ");
        sb.append(cpuload);
        sb.append("]\n\tFree mem: ");
        sb.append(memavail);
        sb.append("\n");
        return sb.toString();
    }
}

