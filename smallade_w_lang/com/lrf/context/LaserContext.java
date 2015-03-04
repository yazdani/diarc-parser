package com.lrf.context;

import com.google.common.base.Objects;

public class LaserContext {

    private RoomType roomType;

    public LaserContext() {
        roomType = RoomType.UNKNOWN;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public LaserContext setRoomType(RoomType roomType) {
        this.roomType = roomType;
        return this;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("roomType", roomType)
                .toString();
    }

    public enum RoomType {
        HALL, ROOM, UNKNOWN
    }
}
