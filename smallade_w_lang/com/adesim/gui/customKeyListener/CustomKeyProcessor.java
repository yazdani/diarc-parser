package com.adesim.gui.customKeyListener;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;

/** The key listener I'm implementing with a bit of misdirection, to make
 * it so that two key presses can be detected simultaneously.
 * Swing should really have a better way, but it looks like they don't!  
 * This still isn't ideal, because after a keyReleased event, the current
 * keyPresses stop firing.  Bah on this lack of foresight on Swing's part */

public class CustomKeyProcessor implements KeyListener {
	private HashSet<Integer> pressedCumulativeKeys = new HashSet<Integer>();
	
	private CustomKeyListener listener;
	private Component attachTo;
	
	
	public CustomKeyProcessor(Component attachTo, CustomKeyListener listener) {
		this.listener = listener;
		this.attachTo = attachTo;
		
		attachTo.addKeyListener(this);
	}
	

	@Override
	public void keyPressed(KeyEvent e) {
		synchronized (pressedCumulativeKeys) {
			pressedCumulativeKeys.add(e.getKeyCode());
			for (Integer eachPressedKey : pressedCumulativeKeys) {
				listener.keyPressed(eachPressedKey);
			}
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		synchronized (pressedCumulativeKeys) {
			int keyCode = e.getKeyCode();
			pressedCumulativeKeys.remove(e.getKeyCode());

			// here don't need to iterate, just call the release callback
			listener.keyReleased(keyCode);
		}
	}
	
	/** keyTyped I am not using, it's more for character input rather than
	 * the purpose of this class, i.e., low-level keyboard pressed/released events.
	 */
	@Override
	public void keyTyped(KeyEvent e) {
		// do nothing
	}


	public void removeKeyListener() {
		attachTo.removeKeyListener(this);
	}


	public boolean checkKeyHeld(int keyCode) {
		return pressedCumulativeKeys.contains(keyCode);
	}
}
