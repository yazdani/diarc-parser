package com.lrf.context;

import com.LaserScan;
import com.lrf.context.LaserContext.RoomType;

import java.util.List;

/** @author Jeremiah M. Via <jeremiah.via@gmail.com> */
public interface RoomTypeClassifier<E> {


    public RoomType classify(List<E> features);
    public LaserContext classify(LaserScan scan);
}
