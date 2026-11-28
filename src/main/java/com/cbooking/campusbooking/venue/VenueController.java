package com.cbooking.campusbooking.venue;

import com.cbooking.campusbooking.booking.BookingRules;
import com.cbooking.campusbooking.common.ApiResponse;
import com.cbooking.campusbooking.common.BusinessException;
import com.cbooking.campusbooking.common.ErrorCode;
import com.cbooking.campusbooking.common.PageResult;
import jakarta.validation.constraints.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/venues")
@Transactional(readOnly = true)
public class VenueController {
    private final VenueMapper venues;
    private final SlotMapper slots;
    private final Clock clock;

    public VenueController(VenueMapper venues, SlotMapper slots, Clock clock) {
        this.venues = venues;
        this.slots = slots;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<PageResult<Venue>> list(
            @RequestParam(defaultValue = "1") @Positive int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageResult.MAX_SIZE) int size,
            @RequestParam(required = false) @Size(min = 1, max = 64) String name,
            @RequestParam(required = false) VenueType type) {
        long offset = (long) (page - 1) * size;
        long total = venues.count(name, type);
        List<Venue> items = offset < total ? venues.list(name, type, offset, size) : List.of();
        return ApiResponse.success(new PageResult<>(items, total, page, size));
    }

    @GetMapping("/{venueId}")
    public ApiResponse<Venue> detail(@PathVariable @Positive long venueId) {
        return ApiResponse.success(requireVenue(venueId));
    }

    @GetMapping("/{venueId}/slots")
    public ApiResponse<List<Slot>> slots(@PathVariable @Positive long venueId,
            @RequestParam LocalDate date, @RequestParam(required = false) SlotStatus status) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || !date.isBefore(today.plusDays(BookingRules.BOOKABLE_DAYS))) {
            throw new BusinessException(ErrorCode.SLOT_OUT_OF_RANGE);
        }
        requireVenue(venueId);
        return ApiResponse.success(slots.list(venueId, date, status));
    }

    private Venue requireVenue(long id) {
        Venue venue = venues.findEnabled(id);
        if (venue == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return venue;
    }
}
