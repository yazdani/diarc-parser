package com.adesim.datastructures;

import java.awt.geom.Dimension2D;

public class DoubleDimension extends Dimension2D {
    public double width;
    public double height;

    public DoubleDimension(double width, double height) {
        this.setSize(width, height);
    }

    @Override
    public double getWidth() {
        return width;
    }

    @Override
    public double getHeight() {
        return height;
    }

    @Override
    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }
    
    @Override
    public String toString() {
    	return "width = " + width + ";  height = " + height;
    }

}
