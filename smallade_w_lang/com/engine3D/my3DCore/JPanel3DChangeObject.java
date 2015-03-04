package com.engine3D.my3DCore;

import com.engine3D.my3DShapes.*;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JPanel3DChangeObject extends JPanel3D {
	private static final long serialVersionUID = 8260415992743063710L;
	public double tX = Math.PI / 2.0;	// Rotation around x (left, right)
	public double tY = 0;	// Rotation around y (up, down)
	public double x = 0, y = 0, z = 0;  // Camera position
	private List<ChangeListener> viewChange =
					Collections.synchronizedList(new ArrayList<ChangeListener>());

	public JPanel3DChangeObject(Universe u) {
		super(u);
	}

	public void addViewChangeListener(ChangeListener cl) {
		viewChange.add(cl);
	}

	public static void main(String[] args) {

		Universe u = new Universe();

		NullObject3D root = new NullObject3D();

		Wall3D wall = new Wall3D(2, -20, 2, 20, 3, 2, new Material(Color.RED, null, null));
		root.addChild(wall);

		u.root = root;

		/*
		u.root = obj;
		double[] pt = {5,10,-1};
		Sphere3D sphere = new Sphere3D(pt, .1, 3, 3, new Material(Color.BLUE,null,null));
		u.root.addChild(sphere);
		System.out.println(obj.inside(pt, 0));
		 */

		// Create frame.
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1000, 700);
		frame.add(new JPanel3DChangeObject(u));
		frame.setVisible(true);
	}

	@Override
	public void setView() {
		u.view = TransformMy3D.translate(x, y, z + -Object3D.SCREEN_DISTANCE - 10);
		u.view.combine(TransformMy3D.rotateX(tX));
		u.view.combine(TransformMy3D.rotateY(tY));
		u.view.combine(TransformMy3D.stretch(3, 3, 3));

		if (viewChange != null) {
			for (ChangeListener cl : viewChange) {
				ChangeEvent ce = new ChangeEvent(this);
				cl.stateChanged(ce);
			}
		}
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent click) {
		System.out.println("Z: " + u.zBuffer[click.getX()][click.getY()]);
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		int dx = mouseDown.x - arg0.getX();
		int dy = mouseDown.y - arg0.getY();
		if (dx * dx + dy * dy > 4) {
			tX += .01 * dy;
			tY -= .01 * dx;
			mouseDown = arg0.getPoint();
			setView();
		}
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// Change render type.
		switch (arg0.getKeyChar()) {
			case '1':
				renderType = Universe.RENDER_WIREFRAME;
				break;
			case '2':
				renderType = Universe.RENDER_NORMAL;
				break;
			case '3':
				renderType = Universe.RENDER_DEPTH;
				break;
			case '4':
				renderType = Universe.RENDER_FLAT;
				break;
			// the following bindings are standard FEDS; the one true 3D keybinding
			case 'e':
				z += .1;
				break;
			case 'd':
				z -= .1;
				break;
			case 's':
				x += .1;
				break;
			case 'f':
				x -= .1;
				break;
			case ' ':
			case '\b':		// \b is Backspace
				y += .1;
				break;
			case 'a':
				y -= .1;
				break;
			case 'r':
				u.root.transform.combine(TransformMy3D.rotateY(.1));
				break;
			case 'w':
				u.root.transform.combine(TransformMy3D.rotateY(-.1));
				break;
			case 'c':
				u.root.transform.combine(TransformMy3D.rotateZ(.1));
				break;
			case 'v':
				u.root.transform.combine(TransformMy3D.rotateZ(-.1));
				break;
			case 't':
				u.root.transform.combine(TransformMy3D.rotateX(.1));
				break;
			case 'g':
				u.root.transform.combine(TransformMy3D.rotateX(-.1));
				break;
		}
		setView();
	}
}
