package com.cbooking.campusbooking.venue;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record Venue(String id, String name, VenueType type, String location, int capacity,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime openTime,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime closeTime, String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'+08:00'") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'+08:00'") LocalDateTime updatedAt) {}
