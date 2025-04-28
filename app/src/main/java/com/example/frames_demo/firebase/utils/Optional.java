package com.example.frames_demo.firebase.utils;

import java.util.NoSuchElementException;

public final class Optional<T> {

    /** If non-null, the value; if null, indicates no value is present */
    private final T value;

    /** Constructs an empty instance. */
    private Optional() {
        this.value = null;
    }

    /**
     * Constructs an instance with the value present.
     *
     * @param value the non-null value to be present
     * @throws NullPointerException if value is null
     */
    private Optional(T value) {
        if (value == null) {
            throw new NullPointerException("value for optional is empty.");
        } else {
            this.value = value;
        }
    }

    /**
     * Constructs an empty value instance of the Optional.
     *
     * @return an {@code Optional} with the value being empty
     */
    public static <T> Optional<T> absent() {
        return new Optional<T>();
    }

    /**
     * Returns an {@code Optional} with the specified present non-null value.
     *
     * @param <T> the class of the value
     * @param value the value to be present, which must be non-null
     * @return an {@code Optional} with the value present
     * @throws NullPointerException if value is null
     */
    public static <T> Optional<T> of(T value) {
        return new Optional<>(value);
    }

    /**
     * Returns an {@code Optional} describing the specified value, if non-null, otherwise returns an
     * empty {@code Optional}.
     *
     * @param <T> the class of the value
     * @param value the possibly-null value to describe
     * @return an {@code Optional} with a present value if the specified value is non-null, otherwise
     *     an empty {@code Optional}
     */
    public static <T> Optional<T> fromNullable(T value) {
        return value == null ? absent() : of(value);
    }

    /**
     * If a value is present in this {@code Optional}, returns the value, otherwise throws {@code
     * NoSuchElementException}.
     *
     * @return the non-null value held by this {@code Optional}
     * @throws NoSuchElementException if there is no value available
     * @see Optional#isAvailable()
     */
    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * Return {@code true} if there is a value available, otherwise {@code false}.
     *
     * @return {@code true} if there is a value available, otherwise {@code false}
     */
    public boolean isAvailable() {
        return value != null;
    }
}