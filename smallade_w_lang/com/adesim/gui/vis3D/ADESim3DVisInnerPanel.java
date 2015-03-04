package com.adesim.gui.vis3D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map.Entry;

import javax.swing.JPanel;

import com.adesim.datastructures.SimShape;
import com.adesim.gui.PaintUtils;
import com.adesim.ADESimActorComponent;
import com.engine3D.my3DCore.*;
import com.engine3D.my3DShapes.*;
import com.adesim.objects.SimEntity;
import com.adesim.objects.model.SimModel;
import com.adesim.gui.vis3D.SimObject3D;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ADESim3DVisInnerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	protected JPanel3DMoveWorld panel3D;
	private Object3D simStatic3D;
	private Universe u;
        private BufferedImage texture = null;

	ADESim3DVisInnerPanel(boolean allowMouseDragging) {
		simStatic3D = new NullObject3D();

		u =  new Universe();
		u.root = simStatic3D;
		panel3D = new JPanel3DMoveWorld(u);
                //panel3D.tX = Math.toRadians(-30.0);

		this.setLayout(new BorderLayout());
		this.add(panel3D, BorderLayout.CENTER);
	}

	/** load map based on model.  and don't visualize a particular robot, if specified
	 * (that way, on single actor, don't accidentally block your own self's view!) */
	public void reloadObjects(SimModel model, String robotNameNotToVisualize) {
		simStatic3D = loadMapHelper(model, robotNameNotToVisualize);
		panel3D.u.root = simStatic3D;
	}

	private Object3D loadMapHelper(SimModel model, String robotNameNotToVisualize) {
		Object3D p = new NullObject3D();

		// visualize world objects:
		for(SimEntity simEntity : model.worldObjects.getObjects()) {
			SimShape shape = simEntity.getShape();
			if(shape != null) {
				Double zHeight = simEntity.getShape().getZLength();
				if (zHeight == null) {
					zHeight = simEntity.defaultZLength();
				}

				for(Line2D line : simEntity.getShape().lines) {

					Color color = simEntity.getColorAssigned();
					if (color == null) {
						color = simEntity.getColorDefault();
					}
                                        if (simEntity.getType().equalsIgnoreCase("wall")) {
                                            if (false && texture == null) {
						URL textureURL = ADESimActorComponent.class.getResource("gui/images/brick_wall.png");
						try {
							texture = ImageIO.read(textureURL);
                                                } catch (IOException ioe) {

                                                }
                                            }
                                        }

					p.addChild(new SimObject3D(line.getP1().getX(), line.getP1().getY(), line.getP2().getX(), line.getP2().getY(),
							shape.getZ(), zHeight, 2, new Material(color, simEntity.getToolTipIfAny(), texture, 2 ,.3 ,.2, 0)));
				}

				// TODO:  ideally, put tops and bottoms.  If container, and open, visualize the contents within.
			}
		}

		// visualize other robots:
		for (Entry<String, SimShape> eachRobotEntry : model.otherRobotShapes.entrySet()) {
			if (!eachRobotEntry.getKey().equals(robotNameNotToVisualize)) {
				SimShape eachRobotShape = eachRobotEntry.getValue();
				for(Line2D line : eachRobotShape.lines) {
					Color color = PaintUtils.COLOR_ROBOT_SHAPES;

					p.addChild(new SimObject3D(line.getP1().getX(), line.getP1().getY(), line.getP2().getX(), line.getP2().getY(),
							eachRobotShape.getZ(), eachRobotShape.getZLength(), 2, new Material(color, null, null, 2 ,.3 ,.4, 0)));
				}
			}
		}

		return p;
	}

	public void setView() {
		panel3D.setView();
	}

	public BufferedImage getImage(int width, int height) {

		BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		u.render(img,panel3D.renderType);
		return img;//panel3D.getImage(width, height);
	}

	/** Sets the camera position.  The sim and the 3D visualization think of coordinates
	 * somewhat differently, this method will take care of the necessary conversions.
	 * passing null to any argument will ensure that it remains untouched. */
	public void setCameraLocation(Double x, Double y, Double z, Double theta) {
		if (x != null) {
			panel3D.py = -1 * x;
		}

		if (y != null) {
			panel3D.px = -1 * y;
		}

		if (z != null) {
			panel3D.pz = z;
		}

		if (theta != null) {
			panel3D.tY = -1 * theta + Math.PI;
		}
	}

	public void offsetTheta(double amount) {
		panel3D.tY += -1 * amount;
	}

	public double getTheta() {
		return ( -1 * (panel3D.tY - Math.PI) );
	}

}
