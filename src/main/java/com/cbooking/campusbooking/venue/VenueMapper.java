package com.cbooking.campusbooking.venue;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface VenueMapper {
    long count(String name, VenueType type);
    List<Venue> list(String name, VenueType type, long offset, int size);
    Venue findEnabled(long id);
}
