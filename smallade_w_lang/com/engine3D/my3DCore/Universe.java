package com.engine3D.my3DCore;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Vector;

import com.engine3D.my3DShapes.*;
import java.util.ConcurrentModificationException;
import java.io.Serializable;

/**
 * The root object for a 3D rendering.
 * @author bkievitk
 */
public class Universe {

  // Objects to render.
  public Object3D root = new NullObject3D();
  // View to render from.
  public TransformMy3D view = new TransformMy3D();
  // Buffers.
  public double[][] zBuffer;
  public Object[][] objBuffer;
  public BufferedImage background = null;
  // Different render types.
  public static final int RENDER_WIREFRAME = 0;
  public static final int RENDER_DEPTH = 1;
  public static final int RENDER_NORMAL = 2;
  public static final int RENDER_FLAT = 3;
  public static final int RENDER_POINTS = 4;
  public Vector<SceneChangeListener> changeListeners = new Vector<SceneChangeListener>();

  public void sceneChanged() {
    for (SceneChangeListener listener : changeListeners) {
      listener.sceneChanged();
    }
  }

  /**
   * Perform rendering on this image with this image type.
   * @param img			Image to render onto.
   * @param renderType	Type of rendering to make.
   */
  public void render(BufferedImage img, int renderType) {

    //System.out.println(root);
    if (background == null || img.getWidth() != background.getWidth() || img.getHeight() != background.getHeight()) {
      makeBackground(img.getWidth(), img.getHeight());
    }
    render(img, background, renderType);
  }

  public void render(BufferedImage img, BufferedImage background, int renderType) {

    Graphics g = img.getGraphics();
    g.drawImage(background, 0, 0, null);

    if (root != null) {
      zBuffer = new double[img.getWidth()][img.getHeight()];
      objBuffer = new Object[img.getWidth()][img.getHeight()];
      for (int x = 0; x < img.getWidth(); x++) {
        for (int y = 0; y < img.getHeight(); y++) {
          zBuffer[x][y] = Double.NEGATIVE_INFINITY;
        }
      }
      try {
        root.render(img, zBuffer, objBuffer, view, renderType);
      } catch (ConcurrentModificationException ex) {
        // This is expected, it only means that something as been added to the universe while it was rendering, which really just means something was added
      }
    }
  }

  public void makeBackground(int width, int height) {
    background = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics g = background.getGraphics();

    g.setColor(Color.BLACK);
    g.setColor(Color.lightGray);
    g.fillRect(0, 0, width, height);

    /*

    Color light = new Color(220,220,220);
    Color dark = new Color(200,200,200);

    for(int x=0;x<width;x+=20) {
    for(int y=0;y<height;y+=20) {
    if((x+y)/20%2==0) {
    g.setColor(light);
    } else {
    g.setColor(dark);
    }
    g.fillRect(x,y,20,20);
    }
    }
     */
  }
}
