package com.cbooking.campusbooking.venue;

public enum VenueType {
    BADMINTON(0), STUDY_ROOM(1), BASKETBALL(2);

    private final int code;

    VenueType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
