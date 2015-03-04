/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 * 
 * Copyright 1997-2013 Matthias Scheutz
 * All rights reserved. Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 */
package ade.gui.sysview.menu;

import ade.gui.sysview.ADESystemView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;

public class AboutMenu extends JMenu {

    private static final long serialVersionUID = 1L;
    private ADESystemView sysView;

    public AboutMenu(ADESystemView sysView) {
        super("About");

        this.sysView = sysView;

        this.add(createRunNewComponentMenuItem());
    }

    private JMenuItem createRunNewComponentMenuItem() {
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
		JDialog dia = new JDialog();
		dia.setModal(true);
		dia.setTitle("ADE Copyright Information");
		JTextArea ta = new JTextArea("SOFTWARE LICENSE FOR ADE 1.0\n\nCopyright 2013 Matthias Scheutz and the HRILab Development Team\n(for information, please write to mscheutz@gmail.com)\nhttp://ade.sourceforge.net/\nhttp://sourceforge.net/projects/ade/\n\nRedistribution and use of all files of the ADE package, in source and\nbinary forms with or without modification, are permitted provided that\n(1) they retain the above copyright notice, this list of conditions\nand the following disclaimer, and (2) redistributions in binary form\nreproduce the above copyright notice, this list of conditions and the\nfollowing disclaimer in the documentation and/or other materials\nprovided with the distribution.\n\nTHIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER \"AS IS\" AND ANY\nEXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\nIMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR\nPURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR ANY\nOF THE CONTRIBUTORS TO THE ADE PROJECT BE LIABLE FOR ANY DIRECT,\nINDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\n(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR\nSERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)\nHOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,\nSTRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING\nIN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE\nPOSSIBILITY OF SUCH DAMAGE.\n\nNote: This license is equivalent to the FreeBSD license.");
		dia.add(ta);
		dia.pack();
		dia.setVisible(true);
	    }});
        return aboutItem;
    }
}
