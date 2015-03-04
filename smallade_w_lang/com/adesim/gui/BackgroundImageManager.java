package com.adesim.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BackgroundImageManager {
    public static final float DEFAULT_BACKGROUND_TRANSPARENCY = 0.5f;
	public static final boolean DEFAULT_PAINT_BEFORE_THE_REST_OF_THE_PANEL = true;

	private BufferedImage originalBackgroundImage;
	private String originalFilePath;
	private BufferedImage cachedBackgroundImage;
	private Dimension cachedVisualizationDimension;
	
	private float backgroundImageTransparency = DEFAULT_BACKGROUND_TRANSPARENCY;
	private boolean paintBeforeTheRestOfThePanel = DEFAULT_PAINT_BEFORE_THE_REST_OF_THE_PANEL;
	
	private ADESimMapVis vis;
	
	public BackgroundImageManager(ADESimMapVis vis) {
		this.vis = vis;
	}
	
	/** paints background image, if it is set.
	 * @param currentVisualizationDimension 
	 * @param isBeforePaintingTheRestOfThePanel = whether or not this 
	 *        call is BEFORE painting everything else, or after
	 */
	public void paintBackgroundIfRelevant(Graphics g, Dimension currentVisDimension, 
			boolean isBeforePaintingTheRestOfThePanel) {
		if (paintBeforeTheRestOfThePanel != isBeforePaintingTheRestOfThePanel) {
			return;
		}
		
		if (originalBackgroundImage == null) {
			return;
		}
		
		if (  (cachedBackgroundImage == null)  || 
			  (! (cachedVisualizationDimension.equals(currentVisDimension)) )  ) {
			this.cachedVisualizationDimension = currentVisDimension;
			cacheBackgroundImage();
		}
		
		g.drawImage(cachedBackgroundImage, 0, 0, null);    	
	}


	private void cacheBackgroundImage() {		
		BufferedImage scaledImage = new BufferedImage(
				(int)cachedVisualizationDimension.getWidth(), 
				(int)cachedVisualizationDimension.getHeight(), 
				BufferedImage.TYPE_INT_ARGB);
		scaledImage.getGraphics().drawImage(originalBackgroundImage, 0, 0, 
				(int)cachedVisualizationDimension.getWidth(), 
				(int)cachedVisualizationDimension.getHeight(), 
				0, 0, originalBackgroundImage.getWidth(), 
				originalBackgroundImage.getHeight(), null);
		
		BufferedImage transparentScaledImage = new BufferedImage(scaledImage.getWidth(), 
				scaledImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		float[] scales = {1f, 1f, 1f, backgroundImageTransparency};
		float[] offsets = new float[4];
		RescaleOp transparencyOp = new RescaleOp(scales, offsets, null);
		((Graphics2D) transparentScaledImage.getGraphics()).
				drawImage(scaledImage, transparencyOp, 0, 0);
		
		this.cachedBackgroundImage = transparentScaledImage;
	}


	/** sets the background image, after first copying it into ARGB space,
	 * so that can do transparencies */
	public void setBackgroundImage(File file) {
		if (file == null) {
			this.originalBackgroundImage = null;
			this.cachedBackgroundImage = null;
		} else {
			this.originalFilePath = file.getAbsolutePath();
			
			try {
				this.originalBackgroundImage = ImageIO.read(file);
				// if succeeded in reading the image, set cachedBackgroundImage to null, 
				//     so that will get re-cached on the next panel re-draw, if relevant.
				this.cachedBackgroundImage = null;
			} catch (IOException e1) {
				vis.showErrorMessage("Could not open image due to exception " + e1, 
						"Could not set background image");
			}
		}
	}

	public void setBackgroundImageTransparency(float factor) {
		this.backgroundImageTransparency = factor;
		this.cachedBackgroundImage = null; // set to null, so will re-draw and re-cache with new 
		//    transparency factor, during panel re-painting
	}


	public void setPaintBeforeTheRestOfThePanel(boolean flag) {
		this.paintBeforeTheRestOfThePanel = flag;
	}

	public String getFileChooserInitDirectory() {
		return originalFilePath;
	}
	
	
}
