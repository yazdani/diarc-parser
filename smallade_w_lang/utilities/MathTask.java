/** 
 * Modulo Artithmetic Task
 * Author: Matthias Scheutz
 * Last modified: 03/12/08
 */
package utilities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class MathTask implements Runnable, KeyListener {
    int subjectID;
    int phase;
    JFrame frame;
    JLabel label;
    boolean keydown = false;
    boolean spacedown = false;
    boolean sdown = false;
    int sleeptime;
    int fontStyle = Font.BOLD;
    String fontName = "SansSerif";
    boolean problemtrue = false;
    boolean answertrue = false;
    long keypressedtime = 0;
    Random gen1;
    Random gen2;

    int[][] practiceproblems = new int[][]{
	{8,2,3,1},
	{7,2,3,0},
	{37,16,7,1}};

    // easy and hard problems, hard-coded for now
    int[][] easyproblems = new int[][]{
	{7,2,5,1},
	{7,2,4,0},
	{5,2,3,1},
	{5,2,2,0},
	{9,1,4,1},
	{9,1,3,0},
	{8,3,5,1},
	{8,3,2,0},
	{6,0,3,1},
	{6,0,4,0},
	{7,3,2,1},
	{7,3,5,0},
	{5,1,4,1},
	{5,1,3,0},
	{9,4,5,1},
	{9,4,2,0},
	{8,4,4,1},
	{8,4,5,0},
	{6,4,2,1},
	{6,4,5,0}};

    int[][] hardproblems = new int[][]{
	{51,19,4,1},
	{51,19,3,0},
	{33,17,8,1},
	{33,17,6,0},
	{63,25,19,1},
	{63,25,18,0},
	{87,29,2,1},
	{87,29,5,0},
	{33,15,9,1},
	{33,15,8,0},
	{45,19,13,1},
	{45,19,12,0},
	{57,3,6,1},
	{57,3,8,0},
	{31,17,7,1},
	{31,17,6,0},
	{57,39,3,1},
	{57,39,5,0},
	{91,67,4,1},
	{91,67,9,0}};

    int numproblems = 10;
    int currentproblem = 0;
    String[] answers = new String[numproblems*2];

    // assigns keys to "yes" and "no"
    int yeskey = KeyEvent.VK_Y;
    int nokey = KeyEvent.VK_N;

    public MathTask(int subjectID, int phase) {
	this.subjectID = subjectID;
	this.phase = phase;
	frame = new JFrame();
	frame.addKeyListener(this);
	frame.setUndecorated(true);
	label = new JLabel("");
	label.setBackground(Color.BLACK);
	label.setOpaque(true);
	label.setHorizontalAlignment(SwingConstants.CENTER);
	frame.getContentPane().add(label);

	GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(frame);
	label.setFont(new Font(fontName, fontStyle, 40));
	gen1 = new Random(subjectID);
	gen2 = new Random(gen1.nextInt());
    }

    // run the experiment
    public static void main(String[] args) {
	// use the first argument for the subject ID to determine
	if (args.length < 2 || args.length > 2) {
	    System.out.println("Usage: java MathTask <subjectID> <phase>");
	    System.out.println("       (note: the subjectID needs to be a unique number)");
	    System.exit(-1);
	}
	else {
	    File logfile = new File("logs");
	    if (! logfile.exists()) {
		System.out.println("Making logs directory");
		logfile.mkdir();
	    } else if (! logfile.isDirectory()) {
		System.out.println("ERROR: 'logs' exists but is not a directory.  Aborting.");
		System.exit(-1);
	    }
	    new MathTask(Integer.parseInt(args[0]), Integer.parseInt(args[1])).run();
	}
    }


    // the function doing all the work...
    public void run() {
	label.setForeground(Color.WHITE);
	if (phase == 1) {
	    label.setText("Press space key to begin practice problems...");
	    while (!spacedown)
	    {
		try {
		    Thread.sleep(1);
		} catch(InterruptedException e) {
		    System.out.println("Got interrupted waiting for <space>!");
		}
	    }
	    label.setText("");
	    runPractice();
	}
	label.setText("Press space key to begin task...");
	while (!spacedown)
	{
	    try {
		Thread.sleep(1);
	    } catch(InterruptedException e) {
		System.out.println("Got interrupted waiting for <space>!");
	    }
	}
	// use subject ID and phase to determine when to run easy and hard 
	// tasks (hard first on phase 1 for subjects with even IDs...)
	runBlock((subjectID+phase)%2 == 0);
	runBlock((subjectID+phase)%2 != -0);
	// write out file
	try {
	    FileWriter fw = new FileWriter("logs/MathResults_"+subjectID+"_"+phase);
	    fw.write("# Subject " + subjectID + " phase " + phase + ": <problem number>,<status of subject answer>,<time in ms>\n");
	    fw.write("# " + Calendar.getInstance().getTime() + "\n");
	    for(int i = 0; i<numproblems*2; i++)
		fw.write(answers[i]+"\n");
	    fw.close();
	} catch(IOException e) {
	    System.err.println("COULD NOT WRITE SUBJECT DATA!!!");
	}      
	label.setText("The math task is now complete.");
	try {
	    Thread.sleep(5000);
	} catch(InterruptedException e) {
	    System.out.println("Got interrupted sleeping!");
	}
	label.setText("Please press the button to call the experimenter.");
	while (!sdown)
	{
	    try {
		Thread.sleep(1);
	    } catch(InterruptedException e) {
		System.out.println("Got interrupted waiting for 's'!");
	    }
	}
	System.exit(0);
    }

    // 
    void runBlock(boolean useeasy) {
	int[][] problems = (useeasy ? easyproblems : hardproblems);
	int offset = 0;
	String problem;
	// initialize counters
	int count = 0;
	long cur = 0;
	long old = System.currentTimeMillis();
	// generate random sequence of problem seeded by subject ID
	Random gen = (useeasy?gen1:gen2);
	ArrayList<Integer> index = new ArrayList<Integer>(numproblems);
	ArrayList<Integer> trues = new ArrayList<Integer>(numproblems);
	ArrayList<Integer> falses = new ArrayList<Integer>(numproblems);
	// There are probably at least 17 better ways of doing this...
	// Create lists containing just the true and false indexes
	for (int i = 0; i<numproblems*2; i += 2) {
	    trues.add(new Integer(i));
	    falses.add(new Integer(i+1));
	}
	// Shuffle those
	java.util.Collections.shuffle(trues, new Random(gen.nextInt()));
	java.util.Collections.shuffle(falses, new Random(gen.nextInt()));
	// Want the second half of those lists in phase 2
	if (phase == 2)
	    offset = 5;
	// Add half of the trues and half of the falses to the index list
	for (int i = 0; i < numproblems/2; i++) {
	    index.add(trues.get(i + offset));
	    index.add(falses.get(i + offset));
	}
	// Shuffle that
	java.util.Collections.shuffle(index, new Random(gen.nextInt()));
	for(int i = 0; i<numproblems; i++) {
	    int p = index.get(i);
	    label.setText("");
	    try {
		Thread.sleep(2000);
	    } catch(InterruptedException e) {
		System.out.println("Got interrupted!");
	    }
	    // display reset the counters
	    //label.setText("+");
	    label.setText("\u2022");
	    try {
		Thread.sleep(500);
	    } catch(InterruptedException e) {
		System.out.println("Got interrupted!");
	    }
	    problem = problems[p][0] + " \u2261 " + problems[p][1] + "  (mod  " + problems[p][2] + ")";
	    //answers[currentproblem] = "Subject " + subjectID + " phase " + phase + ": " + problem;
	    label.setText(problem);
	    problemtrue = (problems[p][3] == 1);
	    count = 0;
	    old = System.currentTimeMillis();
	    // check if a key is already down, then we have an early start
	    if (keydown) {
		//answers[currentproblem++] += ": key pressed prematurely";
		// Coding premature key with a 2 in correct/incorrect colmun
		answers[currentproblem++] = (useeasy?p:(numproblems*2)+p)+",2,0";
		//System.out.println("+++++++++++++++++++++++ key is already down!");
	    } else {
		long oldtemp = old;
		// otherwise wait for keydown or check for 30 second time out
		//while (!keydown && ((cur = System.currentTimeMillis()) < old + 30000))
		while (!keydown)
		{
		    // try to get at close as possible to millisecond resolution...
		    // MS: note if this sleep is not in here, then the keyboard
		    //     listener will not have enough time to run and might
		    //     miss a keypress event
		    // check if key is pressed, then we'll 
		    // check if counter needs to be refreshed
		    try {
			Thread.sleep(1);
		    } catch(InterruptedException e) {
			System.out.println(": Got interrupted in displaying fixation point!");
		    }
		}
		// compute the most accurate count based on when the key was actually pressed...
		count = (int)(keypressedtime - old);
		//answers[currentproblem++] += ": " + (answertrue?"correct":"incorrect") + ", time: " + count;
		answers[currentproblem++] = (useeasy?p:(numproblems*2)+p)+","+(answertrue?1:0)+","+count;
		/*
		   if (answertrue) {
		   label.setText("Correct");
		   label.setForeground(Color.GREEN);
		   } else {
		   label.setText("Incorrect");
		   label.setForeground(Color.RED);
		   }
		   try {
		   Thread.sleep(1000);
		   } catch(InterruptedException e) {
		   System.out.println("Got interrupted!");
		   }
		   label.setForeground(Color.BLUE);
		   */
	    }
	    // wait for key to be released to continue...
	    while (keydown) {
		try {
		    Thread.sleep(1);
		} catch(InterruptedException e) {
		    System.out.println("Got interrupted during waiting for key release!");
		}
	    }	    
	}
    }

    void runPractice() {
	int[][] problems = practiceproblems;
	// initialize counters
	int count = 0;
	long cur = 0;
	long old = System.currentTimeMillis();
	boolean early;

	for(int p = 0; p<3; p++) {
	    early = false;
	    try {
		Thread.sleep(2000);
	    } catch(InterruptedException e) {
		System.out.println("Got interrupted!");
	    }
	    label.setForeground(Color.WHITE);
	    // display reset the counters
	    label.setText("\u2022");
	    try {
		Thread.sleep(500);
	    } catch(InterruptedException e) {
		System.out.println("Got interrupted!");
	    }
	    label.setText(problems[p][0] + " \u2261 " + problems[p][1] + " (mod "
		    + problems[p][2] + ")");
	    problemtrue = (problems[p][3] == 1);
	    count = 0;
	    old = System.currentTimeMillis();
	    // check if a key is already down, then we have an early start
	    if (keydown) {
		early = true;
	    } else {
		long oldtemp = old;
		// otherwise wait for keydown or check for 30 second time out
		//while (!keydown && ((cur = System.currentTimeMillis()) < old + 30000))
		while (!keydown)
		{
		    try {
			Thread.sleep(1);
		    } catch(InterruptedException e) {
			System.out.println("Got interrupted in displaying fixation point!");
		    }
		}
		// compute the most accurate count based on when the key was actually pressed...
		count = (int)(keypressedtime - old);
	    }
	    // wait for key to be released to continue...
	    while (keydown) {
		try {
		    Thread.sleep(1);
		} catch(InterruptedException e) {
		    System.out.println("Got interrupted during waiting for key release!");
		}
	    }	    
	    if (! answertrue) {
		label.setForeground(Color.RED);
		label.setText("Incorrect response.  Please review the problem.");
		p--;
	    } else if (early) {
		label.setForeground(Color.RED);
		label.setText("Early key press.  Please wait for problem to be displayed.");
		p--;
	    } else {
		label.setText("");
	    }
	}
    }
    public void keyPressed(KeyEvent event) {
	keypressedtime = System.currentTimeMillis();
	//int kchar = event.getKeyChar();
	int kchar = event.getKeyCode();
	// PWS: Only accepting 'y' or 'n' for items
	if ((kchar == yeskey) || (kchar == nokey)) {
	    keydown = true;
	    answertrue = (((kchar == yeskey) && problemtrue) || ((kchar == nokey) && !problemtrue));
	} else if (kchar == KeyEvent.VK_S) {
	    sdown = true;
	} else if (kchar == KeyEvent.VK_SPACE) {
	    spacedown = true;
	}
    }

    public void keyReleased(KeyEvent event) {
	keydown = false;
	spacedown = false;
	sdown = false;
    }

    public void keyTyped(KeyEvent event) {}
}
