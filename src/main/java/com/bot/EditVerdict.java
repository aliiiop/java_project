package com.bot;

public enum EditVerdict {
    MOGGED("ТЕБЯ МОГГАЮТ", "mogged"),
    NEUTRAL("НЕЙТРАЛЬНО", "neutral"),
    MOGGER("ТЫ МОГГАЕШЬ", "mogger");

    private final String label;
    private final String templateSuffix;

    EditVerdict(String label, String templateSuffix) {
        this.label = label;
        this.templateSuffix = templateSuffix;
    }

    public String getLabel() {
        return label;
    }

    public String getTemplateSuffix() {
        return templateSuffix;
    }

    public boolean requiresTemplate() {
        return this != NEUTRAL;
    }

    public static EditVerdict fromFaceRating(double rating) {
        if (rating < 3.5) {
            return MOGGED;
        }
        if (rating <= 5.5) {
            return NEUTRAL;
        }
        return MOGGER;
    }
}
