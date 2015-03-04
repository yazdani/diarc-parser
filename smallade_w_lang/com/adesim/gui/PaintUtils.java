package com.adesim.gui;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.adesim.robot.SimAbstractRobot;

public class PaintUtils {
	public static final Color COLOR_ROBOT_SHAPES = new Color(255, 193, 21); // TODO:  different color for each robot

	public static final Color COLOR_3D_VIEW = new Color(0, 128, 81);
	
	private Map<URL, BufferedImage> cachedImages = new HashMap<URL, BufferedImage>();
	
	
	public BufferedImage getCachedImageOrLoadIfNecessary(
			String imageRelativeFilename) {
		URL imageURL = SimAbstractRobot.class.getResource(imageRelativeFilename);
		if (!cachedImages.containsKey(imageURL)) {
			BufferedImage image;
			try {
				image = ImageIO.read(imageURL); 
			} catch (IOException e) {
				image = null;
			}
			cachedImages.put(imageURL, image);
		}
		
		// now, return cached or newly-cached image
		return cachedImages.get(imageURL);
	}
}
