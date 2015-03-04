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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * Class for gathering information about Linux hosts.
 */
public class ADEHostStatusLinux extends ADEHostStatus {

    private static String prg = "ADEHostStatusLinux";
    //HostOS os = HostOS.LINUX;

    public ADEHostStatusLinux(String tmpdir, ADEHostInfo tohost) {
        super(tohost.hostip, tohost.sshusername, tohost.sshcmd, tohost.sshargs, tohost.scpcmd, tohost.scpargs);
        ip = tohost.hostip;
        cmdsh = tohost.shellcmd;
        cmdrm = tohost.rm;
        tmpdirLocal = tmpdir;
        if (!tmpdirLocal.endsWith(tohost.filesep)) {
            tmpdirLocal += tohost.filesep;
        }
        tmpdirRemote = tohost.scratch;
    }

    /**
     * Write the host check shell script to the local temporary directory.
     */
    public String createProbeScript() throws IOException {
        String finm = probe + ip;
        PrintWriter opfi = new PrintWriter(new FileWriter(tmpdirLocal + finm));

        //System.out.println("Creating probescript "+ tmpdirLocal + finm);
        opfi.print("#!");
        opfi.println(cmdsh);
        opfi.print("# " + finm + " script; created ");
        opfi.println(Calendar.getInstance().getTime().toString());
        opfi.println("#");
        opfi.println("# get the number of cpus");
        opfi.println("# make sure to only get lines beginning with 'processor'");
        opfi.println("grep \"^[Pp]rocessor\" /proc/cpuinfo | wc -l");
        opfi.println("# get the total system memory in kB");
        opfi.println("grep MemTotal /proc/meminfo | sed -e \"s/[^0-9]//g\"");
        opfi.println("# get the total system battery capacity in mAh");
        opfi.println("#if [ -x /proc/acpi/battery ] ; then");
        opfi.println("#  Need to get all /proc/acpi/battery/BAT*");
        opfi.println("#  grep \"^design capacity:\" /proc/acpi/battery/BAT0/info | sed -e \"s/ [ ]*/ /g\" | cut -d \" \" -f 3");
        opfi.println("#fi");
        opfi.flush();
        opfi.close();
        Runtime.getRuntime().exec("chmod 755 " + tmpdirLocal + finm);
        return finm;
    }

    /**
     * Write the host stats shell script to the local temporary directory.
     */
    public String createStatsScript() throws IOException {
        String finm = gather + ip;
        PrintWriter opfi = new PrintWriter(new FileWriter(tmpdirLocal + finm));

        //System.out.println("Creating statsscript "+ tmpdirLocal + finm);
        opfi.print("#!");
        opfi.println(cmdsh);
        opfi.print("# " + finm + " script; created ");
        opfi.println(Calendar.getInstance().getTime().toString());
        opfi.println("#");
        opfi.println("# get the 1 minute cpu load (change \\1 to \\2 or \\3");
        opfi.println("# for 5 or 15 minute readings); note that in a multi-cpu");
        opfi.println("# system, the value needs to be divided by number of cpus");
        opfi.println("uptime | sed -e \"s/.*load average:\\(.*\\...\\), .*\\..., .*\\.../\\1/\" -e \"s/ //g\"");
        opfi.println("# get the free system memory in kB");
        opfi.println("grep MemFree /proc/meminfo | sed -e \"s/[^0-9]//g\"");
        opfi.println("# get the current battery capacity in mAh");
        opfi.println("#if [ -x /proc/acpi/battery ] ; then");
        opfi.println("#  Need to get all /proc/acpi/battery/BAT*");
        opfi.println("#  grep \"^remaining capacity:\" /proc/acpi/battery/BAT0/state | sed -e \"s/ [ ]*/ /g\" | cut -d \" \" -f 3");
        opfi.println("#fi");
        opfi.flush();
        opfi.close();
        Runtime.getRuntime().exec("chmod 755 " + tmpdirLocal + finm);
        return finm;
    }
}
