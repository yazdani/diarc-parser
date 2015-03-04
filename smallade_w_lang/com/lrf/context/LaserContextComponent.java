package com.lrf.context;

import java.rmi.RemoteException;


/**
 * This component uses information from the rest of the architecture to determine current contextual information. This
 * information can be leveraged by the feature extractors to enhance their extraction performance.
 *
 * @author Jeremiah Via <jeremiah.via@gmail.com>
 * @since 2013-10-29
 */
public interface LaserContextComponent {

    /**
     * Get the current context of the laser component.
     *
     * @return the current context
     * @throws RemoteException an ADE error
     */
    public LaserContext getLaserContext() throws RemoteException;
}
