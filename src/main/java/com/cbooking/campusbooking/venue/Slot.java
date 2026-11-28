package com.cbooking.campusbooking.venue;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public record Slot(String id, String venueId, LocalDate slotDate,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime endTime, SlotStatus status) {}
