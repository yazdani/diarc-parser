/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Evan Krause
 *
 * Copyright 1997-2013 Evan Krause and HRILab (hrilab.org) All rights reserved.
 * Do not copy and use without permission. For questions contact Evan Krause at
 * evan.krause@tufts.edu.
 */
package com.interfaces;

import ade.ADEComponent;
import com.Predicate;
import com.vision.stm.MemoryObject;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Core interface for vision servers.
 */
public interface VisionComponent extends ADEComponent {

  /**
   * Find all SearchManagers containing at least one token in STM. One each,
   * regardless of how many tokens of a particular type are in STM.
   *
   * @param conf minimum confidence of tokens to be included
   * @return SearchManager IDs
   * @throws RemoteException
   */
  public List<Long> getTypeIds(final double conf) throws RemoteException;

  /**
   * Get the SearchManager ID (search ID) based on a Predicate description of
   * the object. The Predicate description must match the SearchManager
   * description exactly. If an existing SearchManager does not match the
   * description, a new SearchManager will be attempted to be built. This
   * assumes that all descriptors refer to the same object.  Also
   * starts the SearchManager if not running. 
   * NOTE: currently just ignores descriptors that vision doesn't know how 
   * to handle.
   *
   * @param descriptors Predicate description of object
   * @return SearchManager Id (-1 if there is no match)
   * @throws RemoteException
   */
  public Long getTypeId(final List<Predicate> descriptors) throws RemoteException;

  /**
   * Get the SearchManager ID (search ID) based on a Predicate description of
   * the object. The Predicate description must match the SearchManager
   * description exactly. If a current SearchManager does not match the
   * description a new SearchManager will be attempted to be built. Also
   * starts the SearchManager if not running. 
   * NOTE: This is only a convenience method so a List doesn't need to be 
   * passed when only a single Predicate is used.
   *
   * @param descriptor Predicate description of object
   * @return SearchManager Id (-1 if there is no match)
   * @throws RemoteException
   */
  public Long getTypeId(final Predicate descriptor) throws RemoteException;

  /**
   * Get the descriptors for a particular SearchManager.
   *
   * @param typeId
   * @return
   * @throws RemoteException
   */
  public List<Predicate> getDescriptors(final Long typeId) throws RemoteException;

  /**
   * Name the collection of Predicate descriptors referred to by the
   * SearchManager ID (search ID).
   *
   * @param typeId SearchManager ID (search ID)
   * @param typeName Predicate name of object (name will be bound to typeId in
   * VisionComponent)
   * @return true if typeId exists, false otherwise
   * @throws RemoteException
   */
  public boolean nameDescriptors(final Long typeId, final Predicate typeName) throws RemoteException;

  /**
   * Get MemoryObject IDs in STM with a confidence level over the specified
   * threshold. Only considers results from SearchManager searches that are
   * currently running.
   *
   * @param conf minimum confidence of tokens to include
   * @return List of STM MemoryObject IDs
   * @throws RemoteException
   */
  public List<Long> getTokenIds(final double conf) throws RemoteException;

  /**
   * Get MemoryObject IDs that have specified SearchManager ID with a
   * confidence level above the specified threshold. If the specified
   * SearchManager exists but is not running, it will be started before
   * returning results.
   *
   * @param typeId SearchManager ID of tracked objects
   * @param conf minimum confidence of tokens to include
   * @return List of STM MemoryObject IDs
   * @throws RemoteException
   */
  public List<Long> getTokenIds(final long typeId, final double conf) throws RemoteException;

  /**
   * Get MemoryObject IDs for all tokens *exactly* matching descriptors in STM
   * with confidence greater than threshold. If a matching type is found, it
   * will be started (if not already running). If no match exists, a
   * SearchManager will attempt to be built and started before returning.
   * This is largely a convenience method and <getTypeId> should be used
   * whenever possible.
   *
   * @param descriptors list of Predicate descriptors
   * @param conf the minimum confidence level
   * @return List of STM MemoryObject IDs
   * @throws RemoteException
   */
  public List<Long> getTokenIds(final List<Predicate> descriptors, final double conf) throws RemoteException;

  /**
   * Get MemoryObjects in STM with a confidence over the specified threshold.
   * Only considers results from SearchManager searches that are currently
   * running.
   *
   * @param conf minimum confidence of tokens to include
   * @return List of MemoryObjects
   * @throws RemoteException
   */
  public List<MemoryObject> getTokens(final double conf) throws RemoteException;

  /**
   * Get MemoryObjects of the specified SearchManager ID with confidence
   * above specified threshold. If the specified SearchManager exists but is
   * not running, it will be started before returning results.
   *
   * @param typeId SearchManager ID of tracked objects
   * @param conf minimum confidence of tokens to include
   * @return List of MemoryObjects
   * @throws RemoteException
   */
  public List<MemoryObject> getTokens(final long typeId, final double conf) throws RemoteException;

  /**
   * Get MemoryObjects *exactly* matching descriptors in STM with confidence
   * greater than threshold. If a matching type is found, it will be started (if
   * not already running). If no match exists, a SearchManager will attempt
   * to be built and started before returning. This is largely a convenience
   * method and <getTypeId> should be used whenever possible.
   *
   * @param descriptors list of Predicate descriptors
   * @param conf the minimum confidence level
   * @return List of STM MemoryObjects
   * @throws RemoteException
   */
  public List<MemoryObject> getTokens(final List<Predicate> descriptors, final double conf) throws RemoteException;

  /**
   * Get the MemoryObject with the specified id.
   *
   * @param tokenId MemoryObject ID in STM
   * @param conf minimum confidence of tokens to include
   * @return MemoryObject token (Null if doesn't exist)
   * @throws RemoteException
   */
  public MemoryObject getToken(final long tokenId, final double conf) throws RemoteException;

  /**
   * Confirms that the object is still in STM.
   *
   * @param tokenId MemoryObject ID of the object to be confirmed
   * @return true if the MemoryObject is present in STM
   * @throws RemoteException
   */
  public boolean confirmToken(final long tokenId) throws RemoteException;

  /**
   * Confirms that the object is still in STM.
   *
   * @param token MemoryObject to be confirmed
   * @return true if the object is present in STM
   * @throws RemoteException
   */
  public boolean confirmToken(final MemoryObject token) throws RemoteException;

  // ============== START Incremental Search Methods ====================================
  /**
   * This instantiates a new MemoryObjecType (search manager) which can be
   * incrementally configured via <addDescriptor>.
   *
   * @return SearchManager ID (search id)
   * @throws RemoteException
   */
  public Long createNewType() throws RemoteException;

  /**
   * Add new search constraint (i.e., ImageProcessor) to an existing
   * SearchManager (specified by searchID).
   *
   * @param typeId - unique ID returned by <createNewType>
   * @param descriptor - predicate describing visual search attribute (e.g.,
   * "red", "round")
   * @throws RemoteException
   */
  public boolean addDescriptor(final long typeId, final Predicate descriptor) throws RemoteException;

  /**
   * Remove search constraint (i.e., processing descriptor and/or
   * ImageProcessor) to an existing SearchManager (specified by searchID).
   *
   * @param typeId
   * @param descriptor - to remove
   * @return - if removal was successful
   * @throws RemoteException
   */
  public boolean removeDescriptor(final long typeId, final Predicate descriptor) throws RemoteException;

  /**
   * Signal the end of constraint addition. Descriptors/constraints can no
   * longer be added to the SearchManager after this has been called. This
   * also starts the Detector and Tracker and will choose the "object" Detector
   * if one hasn't already been selected.
   *
   * @param typeId
   * @throws RemoteException
   */
  public void endDescriptorChanges(final long typeId) throws RemoteException;
  // ============== END Incremental Search Methods ====================================

  /**
   * Turns off search for typeId, and removes SearchManager.
   *
   * @param typeId
   * @throws RemoteException
   */
  public void stopAndRemoveType(final long typeId) throws RemoteException;
}
// vi:ai:smarttab:expandtab:ts=8 sw=4

