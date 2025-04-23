package com.example.sentrydemo.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DemoDate implements Comparable<DemoDate>  {

        /** Returns the date in nanoseconds as long. */
        public abstract long nanoTimestamp();

        /**
         * Calculates a date by using another date.
         *
         * <p>This is a workaround for limited precision offered in some cases (e.g. when using {@link
       }). This makes it possible to have high precision duration by using
         * nanoseconds for the finish timestamp where normally the start and finish timestamps would only
         * offer millisecond precision.
         *
         * @param otherDate another {@link DemoDate}
         * @return date in seconds as long
         */
        public long laterDateNanosTimestampByDiff(final @Nullable DemoDate otherDate) {
            if (otherDate != null && compareTo(otherDate) < 0) {
                return otherDate.nanoTimestamp();
            } else {
                return nanoTimestamp();
            }
        }

        /**
         * Difference between two dates in nanoseconds.
         *
         * @param otherDate another {@link DemoDate}
         * @return difference in nanoseconds
         */
        public long diff(final @NotNull DemoDate otherDate) {
            return nanoTimestamp() - otherDate.nanoTimestamp();
        }

        @Override
        public int compareTo(@NotNull DemoDate otherDate) {
            return Long.compare(nanoTimestamp(), otherDate.nanoTimestamp());
        }
    }

