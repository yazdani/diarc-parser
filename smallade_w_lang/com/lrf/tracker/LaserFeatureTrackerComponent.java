package com.lrf.tracker;

import com.lrf.feature.Door;
import com.lrf.feature.IntersectionBranch;

import java.rmi.RemoteException;
import java.util.List;

/** @author Jeremiah Via <jeremiah.via@gmail.com> */
public interface LaserFeatureTrackerComponent {

    public List<Door> getTrackedDoors() throws RemoteException;

    public List<IntersectionBranch> getTrackedIntersections() throws RemoteException;
}
