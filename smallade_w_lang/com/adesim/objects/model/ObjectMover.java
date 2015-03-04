package com.adesim.objects.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.adesim.datastructures.SimShape;
import com.adesim.objects.SimContainerEntity;
import com.adesim.objects.SimEntity;

/** a utility class that handles object moving.  this is particularly 
 * important when objects are placed within objects (i.e., when moving a box.
 * must move its contents, too! */

public class ObjectMover {

	/** translate object and all of its contained objects -- ad infinitum 
	 * returns list of all impacted sim entities (i.e., at least this entity, and anything contained within)  
	 * Order is outside to inside, which is important, because of the way the environment broadcasts
	 * entity updates to its children:  i.e., remove object, replace with new; remove object, replace with new. 
	 * Hence need to go from outside in, removing first the "parent" and replacing with a brand new copy, then 
	 * its children.  Otherwise the updates woudl be lost.
	 * @param z */
	public static List<SimEntity> translate(SimEntity entity, double x, double y, double z) {
		ArrayList<SimEntity> impactedSimEntities = new ArrayList<SimEntity>();
		impactedSimEntities.add(entity);
		
		translateShapeOnly(entity.getShape(), x, y, z);
		
		if (entity instanceof SimContainerEntity) {
			for (SimEntity eachContainedEntity : ((SimContainerEntity) entity).getObjectsHolder().getObjects()) {
				impactedSimEntities.addAll(translate(eachContainedEntity, x, y, z));
			}
		}
		
		return impactedSimEntities;
	}

	/** translate the object ONLY, NOT anything contained within 
	 * @param z */
	public static void translateShapeOnly(SimShape shape, double x, double y, double z) {
		AffineTransform transform = new AffineTransform();
		transform.translate(x, y);
		shape.updateShape(transform);
		shape.setZ(shape.getZ() + z);
	}
	
	

	/** rotate object and all of its contained objects -- ad infinitum 
	 * returns list of all impacted sim entities (i.e., at least this entity, and anything contained within). 
	 * Order is outside to inside, which is important, because of the way the environment broadcasts
	 * entity updates to its children:  i.e., remove object, replace with new; remove object, replace with new. 
	 * Hence need to go from outside in, removing first the "parent" and replacing with a brand new copy, then 
	 * its children.  Otherwise the updates woudl be lost.*/
	public static List<SimEntity> rotate(SimEntity entity, double theta, Point2D around) {
		ArrayList<SimEntity> impactedSimEntities = new ArrayList<SimEntity>();
		impactedSimEntities.add(entity);
		
		rotateShapeOnly(entity.getShape(), theta, around);
		
		if (entity instanceof SimContainerEntity) {
			for (SimEntity eachContainedEntity : ((SimContainerEntity) entity).getObjectsHolder().getObjects()) {
				List<SimEntity> theRestOfTheImpactedShapes = 
						rotate(eachContainedEntity, theta, entity.getShape().getCenter()); // the rest of the rotations
						//    are relative to the parent component itself, not the original around point.
				impactedSimEntities.addAll(theRestOfTheImpactedShapes);
			}
		}
		
		return impactedSimEntities;
	}

	/** rotate object and all of its contained objects AROUND OBJECT'S OWN CENTER 
	 * returns list of all impacted sim entities (i.e., at least this entity, and anything contained within)  
	 * Order is outside to inside, which is important, because of the way the environment broadcasts
	 * entity updates to its children:  i.e., remove object, replace with new; remove object, replace with new. 
	 * Hence need to go from outside in, removing first the "parent" and replacing with a brand new copy, then 
	 * its children.  Otherwise the updates woudl be lost.*/
	public static List<SimEntity> rotate(SimEntity entity, double theta) {
		return rotate(entity, theta, entity.getShape().getCenter());
	}

	/** rotate the shape ONLY, NOT anything contained within */
	public static void rotateShapeOnly(SimShape shape, double theta, Point2D around) {
		// transform shape to origin:
    	translateShapeOnly(shape, -1 * around.getX(), -1 * around.getY(), 0);
    	
    	// rotate, once it is at the origin:
    	AffineTransform transformRotateOriginShape = new AffineTransform();
    	transformRotateOriginShape.rotate(theta);
    	shape.updateShape(transformRotateOriginShape);
    	
    	// and transform back to center:
    	translateShapeOnly(shape, around.getX(), around.getY(), 0);     
	}

	/** rotate the shape ONLY, NOT anything contained within -- AROUND SHAPE'S OWN CENTER*/
	public static void rotateShapeOnly(SimShape shape, double theta) {
		rotateShapeOnly(shape, theta, shape.getCenter());
	}
	
	
	/** scale shape only (and only in x and y, NOT z, that would be bad 99% of the time),
	 *  based on its own center (the only thing that makes sense)*/
	public static void scaleShapeOnly(SimShape shape, double xFactor, double yFactor) {
		Point2D shapeCenter = shape.getCenter();
		
		// transform shape to origin:
    	translateShapeOnly(shape, -1 * shapeCenter.getX(), -1 * shapeCenter.getY(), 0);
    	
    	// rotate, once it is at the origin:
    	shape.updateShape(AffineTransform.getScaleInstance(xFactor, yFactor));
    	
    	// and transform back to center:
    	translateShapeOnly(shape, shapeCenter.getX(), shapeCenter.getY(), 0);     
	}
	
}
