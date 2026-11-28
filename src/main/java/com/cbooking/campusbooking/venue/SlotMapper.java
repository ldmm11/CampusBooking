package com.cbooking.campusbooking.venue;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.time.LocalDate;

@Mapper
public interface SlotMapper {
    List<Slot> list(long venueId, LocalDate date, SlotStatus status);
}
