/**
PID.java

A software proportional integral differential controller.
*/
package utilities;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class PID {
	String name = "PID";
	double dState;
	double iState;
	double iMax, iMin;
	double iGain,
		   pGain,
		   dGain;

	// gui components
	JFrame frame;
	JPanel panel;
	JSlider pGainSlider, iGainSlider, dGainSlider;
	
	public PID(String n, double p, double d, double i, double ma, double mi) {
		this(n, p, d, i, ma, mi, false);
	}     
	
	public PID(String n, double p, double d, double i, double ma, double mi, boolean gui) {
		name = n;
		pGain = p;
		dGain = d;
		iGain = i;
		iMax = ma;
		iMin = mi;

		// gui gives you sliders to adjust the pGain, dGain and iGain for debugging
		if (gui) {
			frame = new JFrame(name);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			panel = new JPanel(new GridLayout(3, 2));
			
			//pGain
			panel.add(new JLabel("pGain"));
			pGainSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, (int)(100*pGain));
			
			pGainSlider.setMajorTickSpacing(50);
			pGainSlider.setPaintTicks(true);
			pGainSlider.setPaintLabels(true);
			panel.add(pGainSlider);
			pGainSlider.addChangeListener(new MyActionListener());
			
			//iGain
			panel.add(new JLabel("iGain"));
			iGainSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, (int)(100*iGain));
			iGainSlider.setMajorTickSpacing(50);
			iGainSlider.setPaintTicks(true);
			iGainSlider.setPaintLabels(true);
			panel.add(iGainSlider);
			iGainSlider.addChangeListener(new MyActionListener());
			
			//dGain
			panel.add(new JLabel("dGain"));
			dGainSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, (int)(100*dGain));
			dGainSlider.setMajorTickSpacing(50);
			dGainSlider.setPaintTicks(true);
			dGainSlider.setPaintLabels(true);
			panel.add(dGainSlider);
			dGainSlider.addChangeListener(new MyActionListener());
			
			
			panel.setOpaque(true);
			frame.setContentPane(panel);

			frame.pack();
			frame.setVisible(true);
		}
	}

	class MyActionListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			if (!source.getValueIsAdjusting()) {
				if (source == pGainSlider) {
					pGain = ((double)source.getValue()) / 100.0;
					System.out.println("pGain = " + pGain);
				} else if (source == iGainSlider) {
					iGain = ((double)source.getValue()) / 100.0;
					System.out.println("iGain = " + iGain);
				} else if (source == dGainSlider) {
					dGain = ((double)source.getValue()) / 100.0;
					System.out.println("dGain = " + dGain);
				}
			}
		}
	}

	public double UpdatePID(double error, double position) {
		double pTerm, dTerm, iTerm;

		pTerm = pGain * error;

		iState += error;
		if (iState > iMax) iState = iMax;
		else if (iState < iMin) iState = iMin;

		iTerm = iGain * iState;

		dTerm = dGain * (dState - position);
		dState = position;

		return pTerm + dTerm + iTerm;
	}
	
	public static void main(String args[]) {
		new PID("pan", 1.0, 0.0, 0.0, 300, 0, true);
	}
}

