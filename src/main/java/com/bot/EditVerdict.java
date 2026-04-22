package com.bot;

public enum EditVerdict {
    MOGGED("РўР•Р‘РЇ РњРћР“Р“РђР®Рў", "mogged"),
    NEUTRAL("РќР•Р™РўР РђР›Р¬РќРћ", "neutral"),
    MOGGER("РўР« РњРћР“Р“РђР•РЁР¬", "mogger");

    private static final double MOGGED_MAX_RATING = 3.5;
    private static final double MOGGER_MIN_RATING = 5.5;

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
        if (rating < MOGGED_MAX_RATING) {
            return MOGGED;
        }
        if (rating > MOGGER_MIN_RATING) {
            return MOGGER;
        }
        return NEUTRAL;
    }
}
