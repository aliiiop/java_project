package com.bot;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
public class UserSession {
    private static final DateTimeFormatter PREMIUM_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private UserState state = UserState.SELECTING_GENDER;
    private String gender;
    private String ratingType;
    private boolean isPremium;
    private String premiumUntil;
    private double lastRating;
    private String lastRatingGender;
    private String lastRatingType;
    private String lastPhotoFileId;
    private Map<String, Double> bodyMeasurements = new HashMap<>();
    private List<RatingHistoryEntry> history = new ArrayList<>();
    private LinkedList<UndoAction> undoActions = new LinkedList<>();
    public List<RatingHistoryEntry> getHistory() { return history; }
    public void setHistory(List<RatingHistoryEntry> history) { this.history = history; }
    public void addHistoryEntry(String ratingType, String gender, double rating) {
        history.add(new RatingHistoryEntry(ratingType, gender, rating));
    }
    public UserState getState() { return state; }
    public void setState(UserState state) { this.state = state; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getRatingType() { return ratingType; }
    public void setRatingType(String ratingType) { this.ratingType = ratingType; }
    public boolean isPremium() {
        if (isPremium) return true;
        LocalDateTime expiry = getPremiumUntilDateTime();
        return expiry != null && expiry.isAfter(LocalDateTime.now());
    }
    public void setPremium(boolean premium) {
        isPremium = premium;
        if (!premium) premiumUntil = null;
    }
    public String getPremiumUntil() { return premiumUntil; }
    public void setPremiumUntil(String premiumUntil) { this.premiumUntil = premiumUntil; }
    public void activatePremiumDays(int days) {
        isPremium = false;
        premiumUntil = LocalDateTime.now().plusDays(days).format(PREMIUM_FORMATTER);
    }
    public LocalDateTime getPremiumUntilDateTime() {
        if (premiumUntil == null || premiumUntil.isBlank()) return null;
        try {
            return LocalDateTime.parse(premiumUntil, PREMIUM_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
    public String getPremiumUntilLabel() {
        LocalDateTime expiry = getPremiumUntilDateTime();
        if (expiry == null && isPremium) return "без срока";
        return expiry == null ? null : expiry.format(PREMIUM_FORMATTER);
    }
    public double getLastRating() { return lastRating; }
    public void setLastRating(double lastRating) { this.lastRating = lastRating; }
    public String getLastRatingGender() { return lastRatingGender; }
    public void setLastRatingGender(String lastRatingGender) { this.lastRatingGender = lastRatingGender; }
    public String getLastRatingType() { return lastRatingType; }
    public void setLastRatingType(String lastRatingType) { this.lastRatingType = lastRatingType; }
    public String getLastPhotoFileId() { return lastPhotoFileId; }
    public void setLastPhotoFileId(String lastPhotoFileId) { this.lastPhotoFileId = lastPhotoFileId; }
    public Map<String, Double> getBodyMeasurements() { return bodyMeasurements; }
    public void pushUndoAction(UserState previousState, String previousGender, String previousRatingType, String measurementKeyToRemove) {
        undoActions.addLast(new UndoAction(previousState, previousGender, previousRatingType, measurementKeyToRemove));
    }
    public boolean undoLastAction() {
        if (undoActions.isEmpty()) return false;
        UndoAction action = undoActions.removeLast();
        state = action.previousState;
        gender = action.previousGender;
        ratingType = action.previousRatingType;
        if (action.measurementKeyToRemove != null) {
            bodyMeasurements.remove(action.measurementKeyToRemove);
        }
        return true;
    }
    public void clearUndoActions() { undoActions.clear(); }
    public boolean hasPersistentData() {
        return isPremium() || !history.isEmpty();
    }
    public Double getPreviousRating(String type) {
        for (int i = history.size() - 1; i >= 0; i--) {
            RatingHistoryEntry entry = history.get(i);
            if (type == null || type.equals(entry.getRatingType())) {
                return entry.getRating();
            }
        }
        return null;
    }

    public static class RatingHistoryEntry {
        private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        private String ratingType;
        private String gender;
        private double rating;
        private String timestamp;

        public RatingHistoryEntry() {}

        public RatingHistoryEntry(String ratingType, String gender, double rating) {
            this.ratingType = ratingType;
            this.gender = gender;
            this.rating = rating;
            this.timestamp = LocalDateTime.now().format(FORMATTER);
        }

        public String getRatingType() { return ratingType; }
        public void setRatingType(String ratingType) { this.ratingType = ratingType; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getTypeLabel() {
            switch (ratingType) {
                case "face":    return "📸 Лицо по фото";
                case "body":    return "🏃 Тело по фото";
                case "measure": return "📏 Тело по замерам";
                default:        return ratingType;
            }
        }

        public String getGenderLabel() {
            if ("male".equals(gender))   return "👨";
            if ("female".equals(gender)) return "👩";
            return "";
        }

        public String format(int index) {
            return String.format("%d. %s %s — *%.1f/10*\n   _%s_",
                index, getGenderLabel(), getTypeLabel(), rating, timestamp);
        }
    }

    public static class UndoAction {
        private UserState previousState;
        private String previousGender;
        private String previousRatingType;
        private String measurementKeyToRemove;

        public UndoAction() {}

        public UndoAction(UserState previousState, String previousGender, String previousRatingType, String measurementKeyToRemove) {
            this.previousState = previousState;
            this.previousGender = previousGender;
            this.previousRatingType = previousRatingType;
            this.measurementKeyToRemove = measurementKeyToRemove;
        }

        public UserState getPreviousState() { return previousState; }
        public void setPreviousState(UserState previousState) { this.previousState = previousState; }
        public String getPreviousGender() { return previousGender; }
        public void setPreviousGender(String previousGender) { this.previousGender = previousGender; }
        public String getPreviousRatingType() { return previousRatingType; }
        public void setPreviousRatingType(String previousRatingType) { this.previousRatingType = previousRatingType; }
        public String getMeasurementKeyToRemove() { return measurementKeyToRemove; }
        public void setMeasurementKeyToRemove(String measurementKeyToRemove) { this.measurementKeyToRemove = measurementKeyToRemove; }
    }
}
