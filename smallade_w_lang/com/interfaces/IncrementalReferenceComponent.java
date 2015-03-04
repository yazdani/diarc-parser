 /**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Tom Williams
 *
 * Copyright 1997-2013 M@ Dunlap and HRILab (hrilab.org)
 * All rights reserved. Do not copy and use without permission.
 * For questions contact Tom at <ThomasEmrysWilliams@gmail.com>
 */
package com.interfaces;
import ade.*;
import com.Predicate;
import java.rmi.*;
import java.util.List;

public interface IncrementalReferenceComponent extends ADEComponent{
  
  /**
   * This instantiates a new search manager which can be
   * incrementally configured via <addDescriptor>.
   *
   * @return search id
   * @throws RemoteException
   */
  public Long createNewType() throws RemoteException;

  /**
   * Add new search constraint to an existing search (specified by searchID).
   *
   * @param typeId - unique ID returned by <createNewType>
   * @param descriptor - predicate describing search attribute (e.g.,
   * "red", "round")
   * @throws RemoteException
   */
  public boolean addDescriptor(final long typeId, final Predicate descriptor) throws RemoteException;

  /**
   * Remove search constraint from an existing search (specified by searchID).
   *
   * @param typeId
   * @param descriptor - to remove
   * @return - if removal was successful
   * @throws RemoteException
   */
  public boolean removeDescriptor(final long typeId, final Predicate descriptor) throws RemoteException;

  /**
   * Signal the end of constraint addition. Descriptors/constraints can no
   * longer be added to the search after this has been called.
   *
   * @param typeId
   * @throws RemoteException
   */
  public void endDescriptorChanges(final long typeId) throws RemoteException;

  /**
   * Turns off search for typeId, and removes search.
   *
   * @param typeId
   * @throws RemoteException
   */
  public void stopAndRemoveType(final long typeId) throws RemoteException;

  /**
   * Find all searches containing at least one token in STM. One each,
   * regardless of how many tokens of a particular type are in STM.
   *
   * @param conf minimum confidence of tokens to be included
   * @return MemoryObjectType IDs
   * @throws RemoteException
   */
  public List<Long> getTypeIds(final double conf) throws RemoteException;
}
