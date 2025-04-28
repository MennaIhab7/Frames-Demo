package com.example.frames_demo.sentry.span;

import org.jetbrains.annotations.Nullable;

public interface ISpan {
    /**
     * Sets extra data on span or transaction.
     *
     * @param key   the data key
     * @param value the data value
     */
    void setData(@Nullable String key, @Nullable Object value);

    /**
     * Returns extra data from span or transaction.
     *
     * @return the data
     */
    @Nullable
    Object getData(@Nullable String key);


    /**
     * Returns the start date of this span or transaction.
     *
     * @return the start date
     */
    long getStartDate();

    /**
     * Returns the end date of this span or transaction.
     *
     * @return the end date
     */

    long getFinishDate();
}

