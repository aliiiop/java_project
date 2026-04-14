package com.bot;

import java.util.HashMap;
import java.util.Map;

public class UserSession {
    private UserState state;
    private String gender;
    private String ratingType;
    private boolean isPremium;
    private double lastRating;
    private Map<String, Double> bodyMeasurements;

    public UserSession() {
        this.state = UserState.SELECTING_GENDER;
        this.isPremium = false;
        this.bodyMeasurements = new HashMap<>();
    }

    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getRatingType() {
        return ratingType;
    }

    public void setRatingType(String ratingType) {
        this.ratingType = ratingType;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }

    public double getLastRating() {
        return lastRating;
    }

    public void setLastRating(double lastRating) {
        this.lastRating = lastRating;
    }

    public Map<String, Double> getBodyMeasurements() {
        return bodyMeasurements;
    }

    public void setBodyMeasurements(Map<String, Double> bodyMeasurements) {
        this.bodyMeasurements = bodyMeasurements;
    }
}
