package com.lrf.extractor;

import ade.ADEComponent;
import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;
import com.lrf.feature.Line;

import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import java.util.List;

/**
 * The LaserFeatureExtractor component subscribes to a component implementing the {@link com.interfaces.LaserComponent}
 * interface which provides a well-formed {@link com.LaserScan} and uses this data to extract features. These features
 * are available individually by API specified below.
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 * @since 2013-10-25
 */
public interface LaserFeatureExtractorComponent extends ADEComponent {

    /**
     * Returns the points extracted from a laser scan or an empty list if point extraction fails.
     *
     * @return a list of extracted points
     * @throws RemoteException an ADE error
     */
    public List<Point2D> getPoints() throws RemoteException;

    /**
     * Returns the lines extracted from a laser scan or an empty list if line extraction fails.
     *
     * @return a list of extracted lines
     * @throws RemoteException an ADE error
     */
    public List<Line> getLines() throws RemoteException;

    /**
     * Returns the doors extracted from a laser scan or an empty list if door extraction fails.
     *
     * @return a list of extracted doors
     * @throws RemoteException an ADE error
     */
    public List<Door> getDoors() throws RemoteException;

    /**
     * Returns the intersections extracted from a laser scan or an empty list if intersection extraction fails.
     *
     * @return a list of extracted intersections
     * @throws RemoteException an ADE error
     */
    public List<IntersectionBranch> getIntersections() throws RemoteException;

    /**
     * Returns the right angles extracted from a laser scan or an empty list if right angle extractionfails.
     *
     * @return a list of extracted right angles
     * @throws RemoteException an ADE error
     */
    public List<Point2D> getRightAngles() throws RemoteException;

    /**
     * Determines if the current laser scan is from a hallway.
     *
     * @return true i.f.f. hallway is detected
     * @throws RemoteException an ADE error
     */
    public boolean inHallway() throws RemoteException;

    public void setModel(FeatureModel model) throws RemoteException;
}
