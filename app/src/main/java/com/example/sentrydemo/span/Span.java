package com.example.sentrydemo.span;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Span implements ISpan {

    /**
     * The moment in time when span was started.
     */
    private final long startTimeStamp;

    /**
     * The moment in time when span has ended.
     */
    private long endTimeStamp;


    private final @NotNull Map<String, Object> data = new ConcurrentHashMap<>();


    public Span() {
        startTimeStamp = System.nanoTime();
    }

    @Override
    public long getStartDate() {
        return startTimeStamp;
    }

    @Override
    public long getFinishDate() {
        return endTimeStamp;
    }

    public @NotNull Map<String, Object> getData() {
        return data;
    }

    @Override
    public void setData(final @Nullable String key, final @Nullable Object value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    @Override
    public @Nullable Object getData(final @Nullable String key) {
        if (key == null) {
            return null;
        }
        return data.get(key);
    }


    public void setFinishedDate(long timestamp) {
        this.endTimeStamp = timestamp;
    }


}
