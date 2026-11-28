package com.cbooking.campusbooking.venue;

public enum SlotStatus {
    AVAILABLE(0), OCCUPIED(1), CLOSED(2);

    private final int code;

    SlotStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
