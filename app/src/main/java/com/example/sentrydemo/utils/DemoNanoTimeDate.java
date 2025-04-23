package com.example.sentrydemo.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Date;

public final class DemoNanoTimeDate extends DemoDate {

    private final @NotNull Date date;
    private final long nanos;


    public DemoNanoTimeDate(final @NotNull Date date, final long nanos) {
        this.date = date;
        this.nanos = nanos;
    }

    @Override
    public long diff(final @NotNull DemoDate otherDate) {
        if (otherDate instanceof DemoNanoTimeDate) {
            final @NotNull DemoNanoTimeDate otherNanoDate = (DemoNanoTimeDate) otherDate;
            return nanos - otherNanoDate.nanos;
        }
        return super.diff(otherDate);
    }

    @Override
    public long nanoTimestamp() {
        return date.getTime() * 1000000L;
    }

    @Override
    public long laterDateNanosTimestampByDiff(final @Nullable DemoDate otherDate) {
        if (otherDate instanceof DemoNanoTimeDate) {
            final @NotNull DemoNanoTimeDate otherNanoDate = (DemoNanoTimeDate) otherDate;
            if (compareTo(otherDate) < 0) {
                return nanotimeDiff(this, otherNanoDate);
            } else {
                return nanotimeDiff(otherNanoDate, this);
            }
        } else {
            return super.laterDateNanosTimestampByDiff(otherDate);
        }
    }

    @Override
    @SuppressWarnings("JavaUtilDate")
    public int compareTo(@NotNull DemoDate otherDate) {
        if (otherDate instanceof DemoNanoTimeDate) {
            final @NotNull DemoNanoTimeDate otherNanoDate = (DemoNanoTimeDate) otherDate;
            final long thisDateMillis = date.getTime();
            final long otherDateMillis = otherNanoDate.date.getTime();
            if (thisDateMillis == otherDateMillis) {
                return Long.compare(nanos, otherNanoDate.nanos);
            } else {
                return Long.compare(thisDateMillis, otherDateMillis);
            }
        } else {
            return super.compareTo(otherDate);
        }
    }

    private long nanotimeDiff(
            final @NotNull DemoNanoTimeDate earlierDate, final @NotNull DemoNanoTimeDate laterDate) {
        final long nanoDiff = laterDate.nanos - earlierDate.nanos;
        return earlierDate.nanoTimestamp() + nanoDiff;
    }
}
