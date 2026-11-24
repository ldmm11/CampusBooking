package com.cbooking.campusbooking.booking;

import java.time.Duration;
import java.time.Period;

/** 需求 BR-02 至 BR-12 中各业务判断共用的阈值。 */
public final class BookingRules {
    public static final int BOOKABLE_DAYS = 14;
    public static final int DAILY_LIMIT = 2;
    public static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(15);
    public static final Duration CANCELLATION_NOTICE = Duration.ofHours(2);
    public static final Duration CHECK_IN_BEFORE_START = Duration.ofMinutes(15);
    public static final Duration CHECK_IN_AFTER_START = Duration.ofMinutes(15);
    public static final Period NO_SHOW_LOOKBACK = Period.ofDays(30);
    public static final int NO_SHOW_LIMIT = 3;
    public static final Period BAN_DURATION = Period.ofDays(7);

    private BookingRules() {}
}
