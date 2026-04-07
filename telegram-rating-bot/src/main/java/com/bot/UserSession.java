package com.bot;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
public class UserSession {
    private UserState state = UserState.SELECTING_GENDER;
    private String gender;
    private String ratingType;
    private boolean isPremium;
    private double lastRating;
    private Map<String, Double> bodyMeasurements = new HashMap<>();
    private List<RatingHistoryEntry> history = new ArrayList<>();
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
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public double getLastRating() { return lastRating; }
    public void setLastRating(double lastRating) { this.lastRating = lastRating; }
    public Map<String, Double> getBodyMeasurements() { return bodyMeasurements; }

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
}
