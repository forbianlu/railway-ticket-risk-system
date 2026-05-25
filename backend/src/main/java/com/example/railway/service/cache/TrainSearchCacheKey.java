package com.example.railway.service.cache;

import java.time.LocalDate;
import java.util.Objects;

public final class TrainSearchCacheKey {

    private final String departureCode;
    private final String arrivalCode;
    private final LocalDate travelDate;

    private TrainSearchCacheKey(String departureCode, String arrivalCode, LocalDate travelDate) {
        this.departureCode = normalize(departureCode);
        this.arrivalCode = normalize(arrivalCode);
        this.travelDate = travelDate;
    }

    public static TrainSearchCacheKey of(String departureCode, String arrivalCode, LocalDate travelDate) {
        return new TrainSearchCacheKey(departureCode, arrivalCode, travelDate);
    }

    public String redisKey() {
        return "railway:cache:train-search:" + departureCode + ":" + arrivalCode + ":" + travelDate;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TrainSearchCacheKey)) {
            return false;
        }
        TrainSearchCacheKey that = (TrainSearchCacheKey) object;
        return Objects.equals(departureCode, that.departureCode)
                && Objects.equals(arrivalCode, that.arrivalCode)
                && Objects.equals(travelDate, that.travelDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departureCode, arrivalCode, travelDate);
    }
}
