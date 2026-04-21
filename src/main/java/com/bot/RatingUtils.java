package com.bot;

import java.util.Map;

public class RatingUtils {
    public static String generateRatingMessage(double rating, boolean premium, String gender) {
        return generateRatingMessage(rating, premium, gender, null, "face");
    }

    public static String generateMeasurementsMessage(Map<String, Double> measurements, double rating, boolean premium, String gender) {
        return generateMeasurementsMessage(measurements, rating, premium, gender, null);
    }

    public static double evaluateFace(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        int hash = photoId.hashCode();
        double rating = 3.5 + (Math.abs(hash % 50) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }

    public static double evaluateBodyByPhoto(String photoId, String gender) {
        if (photoId == null || photoId.isEmpty()) return 0;
        int hash = photoId.hashCode();
        double rating = 3.0 + (Math.abs(hash % 60) / 10.0);
        return Math.max(1.0, Math.min(10.0, rating));
    }

    public static double evaluateBodyByMeasurements(Map<String, Double> m, String gender) {
        boolean male = gender.equals("male");
        double height = m.getOrDefault("height", male ? 175.0 : 168.0);
        double waist = m.getOrDefault("waist", male ? 80.0 : 65.0);
        double weight = m.getOrDefault("weight", male ? 75.0 : 60.0);
        double chest = m.getOrDefault("chest", male ? 100.0 : 90.0);
        double hips = m.getOrDefault("hips", 90.0);
        double shoulder = m.getOrDefault("shoulder", waist * 1.6);
        double neck = m.getOrDefault("neck", 41.0);

        double rating = 5.0;

        if (male) {
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 18.5 && bmi <= 25.0) {
                rating += 1.5;
            } else if (bmi >= 25.0 && bmi <= 27.0) {
                rating += 0.8;
            } else if (bmi >= 17.0 && bmi < 18.5) {
                rating += 0.5;
            } else {
                rating -= Math.abs(bmi - 23.0) * 0.3;
            }

            double shoulderWaistRatio = shoulder / waist;
            if (shoulderWaistRatio >= 1.5 && shoulderWaistRatio <= 1.7) {
                rating += 1.5;
            } else if (shoulderWaistRatio >= 1.4 && shoulderWaistRatio <= 1.8) {
                rating += 0.8;
            } else {
                rating -= Math.abs(shoulderWaistRatio - 1.6) * 0.8;
            }

            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 1.20 && chestWaistRatio <= 1.35) {
                rating += 1.2;
            } else {
                rating -= Math.abs(chestWaistRatio - 1.27) * 1.5;
            }

            if (height >= 175 && height <= 185) {
                rating += 1.0;
            } else if (height >= 170 && height < 175) {
                rating += 0.5;
            } else if (height > 185 && height <= 195) {
                rating += 0.3;
            } else {
                rating -= Math.abs(height - 177.0) * 0.04;
            }

            if (neck >= 39 && neck <= 43) {
                rating += 0.8;
            } else {
                rating -= Math.abs(neck - 41.0) * 0.1;
            }

            if (waist >= 75 && waist <= 85) {
                rating += 0.8;
            } else {
                rating -= Math.abs(waist - 80.0) * 0.10;
            }

            if (chest >= 95 && chest <= 110) {
                rating += 0.6;
            } else {
                rating -= Math.abs(chest - 100.0) * 0.08;
            }
        } else {
            double bmi = weight / ((height / 100.0) * (height / 100.0));
            if (bmi >= 19.0 && bmi <= 24.0) {
                rating += 1.5;
            } else if (bmi >= 24.0 && bmi <= 26.0) {
                rating += 0.7;
            } else if (bmi >= 18.0 && bmi < 19.0) {
                rating += 0.8;
            } else {
                rating -= Math.abs(bmi - 21.5) * 0.3;
            }

            double waistHipsRatio = waist / hips;
            if (waistHipsRatio >= 0.65 && waistHipsRatio <= 0.75) {
                rating += 2.0;
            } else if (waistHipsRatio >= 0.60 && waistHipsRatio <= 0.80) {
                rating += 1.0;
            } else {
                rating -= Math.abs(waistHipsRatio - 0.70) * 1.5;
            }

            double chestWaistRatio = chest / waist;
            if (chestWaistRatio >= 1.30 && chestWaistRatio <= 1.45) {
                rating += 1.2;
            } else {
                rating -= Math.abs(chestWaistRatio - 1.38) * 1.2;
            }

            if (height >= 162 && height <= 175) {
                rating += 1.0;
            } else if (height >= 158 && height < 162) {
                rating += 0.4;
            } else if (height > 175 && height <= 183) {
                rating += 0.6;
            } else {
                rating -= Math.abs(height - 168.0) * 0.03;
            }

            if (chest >= 85 && chest <= 95) {
                rating += 0.8;
            } else {
                rating -= Math.abs(chest - 90.0) * 0.08;
            }

            if (waist >= 60 && waist <= 70) {
                rating += 0.8;
            } else {
                rating -= Math.abs(waist - 65.0) * 0.10;
            }
        }

        return Math.max(1.0, Math.min(10.0, rating));
    }

    public static String generateRatingMessage(double rating, boolean premium, String gender, Double previousRating, String ratingType) {
        StringBuilder msg = new StringBuilder();
        msg.append("🌟 *Ваша оценка: ").append(String.format("%.1f", rating)).append(" PSL* 🌟\n\n");
        appendProgressLine(msg, rating, previousRating);
        msg.append("📈 *Потенциал:* ").append(getPotentialLabel(rating)).append("\n");
        if ("face".equals(ratingType)) {
            msg.append("🎭 *Вердикт:* ").append(getEditLabel(rating)).append("\n");
        }
        msg.append("🧭 *Уровень:* ").append(getLevelLabel(rating)).append("\n");

        if (!premium) {
            msg.append("\n💰 Premium: /premium\n");
            msg.append("Откроет детальный разбор, рекомендации и сравнение с прошлым результатом.");
            if (rating < 4) {
                msg.append("\n\n⚪ *WHITE PILL* ⚪\nВнешность не главное. Развивайся и собирай апгрейд поэтапно.");
            }
            return msg.toString();
        }

        msg.append("\n🔍 *Детальный разбор:*\n");
        msg.append(buildPhotoBreakdown(rating, gender, ratingType));
        msg.append("\n🎯 *Главный фокус:* ").append(getPrimaryFocus(rating, ratingType)).append("\n");
        msg.append("📌 *Потолок при нормальном апгрейде:* ").append(getPotentialCeiling(rating)).append("\n");
        msg.append("📝 Это не компьютерное зрение по чертам лица, а расширенная интерпретация итогового балла.\n");

        if (rating < 4) {
            msg.append("\n⚪ *WHITE PILL* ⚪\nНе пытайтесь менять все сразу. Сначала база: сон, форма, стиль, свет и качество фото.");
        }
        return msg.toString();
    }

    public static String generateMeasurementsMessage(Map<String, Double> measurements, double rating, boolean premium, String gender, Double previousRating) {
        double height = measurements.getOrDefault("height", 175.0);
        double weight = measurements.getOrDefault("weight", 75.0);
        double chest = measurements.getOrDefault("chest", 100.0);
        double waist = measurements.getOrDefault("waist", 80.0);
        double hips = measurements.getOrDefault("hips", 90.0);
        double shoulder = measurements.getOrDefault("shoulder", 45.0);

        double bmi = weight / ((height / 100.0) * (height / 100.0));
        StringBuilder msg = new StringBuilder();
        msg.append("🌟 *АНАЛИЗ ПО ЗАМЕРАМ: ").append(String.format("%.1f", rating)).append(" PSL* 🌟\n\n");
        appendProgressLine(msg, rating, previousRating);

        if (gender.equals("male")) {
            double shoulderWaistRatio = shoulder / waist;
            double chestWaistRatio = chest / waist;

            msg.append("📊 *Параметры*\n");
            msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см ").append(getStatusIcon(height >= 175 && height <= 185)).append("\n");
            msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
            msg.append("• BMI: ").append(String.format("%.1f", bmi)).append(" ").append(getStatusIcon(bmi >= 18.5 && bmi <= 25.0)).append("\n");
            msg.append("• Плечи: ").append(String.format("%.1f", shoulder)).append(" см\n");
            msg.append("• Грудь: ").append(String.format("%.1f", chest)).append(" см\n");
            msg.append("• Талия: ").append(String.format("%.1f", waist)).append(" см\n\n");

            msg.append("💪 *Соотношения*\n");
            msg.append("• Плечи/Талия: ").append(String.format("%.2f", shoulderWaistRatio)).append(" ").append(getStatusIcon(shoulderWaistRatio >= 1.5 && shoulderWaistRatio <= 1.7)).append("\n");
            msg.append("• Грудь/Талия: ").append(String.format("%.2f", chestWaistRatio)).append(" ").append(getStatusIcon(chestWaistRatio >= 1.20 && chestWaistRatio <= 1.35)).append("\n");

            msg.append("\n🎯 *Итог:* ").append(getMeasurementLevel(rating)).append("\n");

            if (!premium) {
                msg.append("\n💰 Premium разбор: /premium");
                return msg.toString();
            }

            msg.append("\n\n🔍 *Детальный разбор:*\n");
            msg.append("• Идеальный BMI: 20.0-25.0, у вас ").append(String.format("%.1f", bmi)).append("\n");
            msg.append("• Идеальные плечи/талия: 1.50-1.70, у вас ").append(String.format("%.2f", shoulderWaistRatio)).append("\n");
            msg.append("• Идеальные грудь/талия: 1.20-1.35, у вас ").append(String.format("%.2f", chestWaistRatio)).append("\n");
            msg.append("• Приоритет номер 1: ").append(getMalePriority(bmi, shoulderWaistRatio, chestWaistRatio, waist)).append("\n");
            msg.append("• Что сильнее всего поднимет балл: ").append(getMaleUpgrade(bmi, shoulderWaistRatio, waist)).append("\n");
            msg.append("• Реалистичный потолок при работе над формой: ").append(getMeasurementsPotentialCeiling(rating, bmi)).append("\n");
            return msg.toString();
        }

        double waistHipsRatio = waist / hips;
        double chestWaistRatio = chest / waist;

        msg.append("📊 *Параметры*\n");
        msg.append("• Рост: ").append(String.format("%.0f", height)).append(" см ").append(getStatusIcon(height >= 162 && height <= 175)).append("\n");
        msg.append("• Вес: ").append(String.format("%.1f", weight)).append(" кг\n");
        msg.append("• BMI: ").append(String.format("%.1f", bmi)).append(" ").append(getStatusIcon(bmi >= 19.0 && bmi <= 24.0)).append("\n");
        msg.append("• Грудь: ").append(String.format("%.1f", chest)).append(" см ").append(getStatusIcon(chest >= 85 && chest <= 95)).append("\n");
        msg.append("• Талия: ").append(String.format("%.1f", waist)).append(" см\n");
        msg.append("• Бёдра: ").append(String.format("%.1f", hips)).append(" см\n\n");

        msg.append("✨ *Пропорции*\n");
        msg.append("• Талия/Бёдра: ").append(String.format("%.2f", waistHipsRatio)).append(" ").append(getStatusIcon(waistHipsRatio >= 0.65 && waistHipsRatio <= 0.75)).append("\n");
        msg.append("• Грудь/Талия: ").append(String.format("%.2f", chestWaistRatio)).append(" ").append(getStatusIcon(chestWaistRatio >= 1.30 && chestWaistRatio <= 1.45)).append("\n");

        msg.append("\n🎯 *Итог:* ").append(getMeasurementLevel(rating)).append("\n");

        if (!premium) {
            msg.append("\n💰 Premium разбор: /premium");
            return msg.toString();
        }

        msg.append("\n\n🔍 *Детальный разбор:*\n");
        msg.append("• Идеальный BMI: 19.0-24.0, у вас ").append(String.format("%.1f", bmi)).append("\n");
        msg.append("• Идеальные талия/бёдра: 0.65-0.75, у вас ").append(String.format("%.2f", waistHipsRatio)).append("\n");
        msg.append("• Идеальные грудь/талия: 1.30-1.45, у вас ").append(String.format("%.2f", chestWaistRatio)).append("\n");
        msg.append("• Приоритет номер 1: ").append(getFemalePriority(bmi, waistHipsRatio, chestWaistRatio, waist)).append("\n");
        msg.append("• Что сильнее всего поднимет балл: ").append(getFemaleUpgrade(bmi, waistHipsRatio, waist)).append("\n");
        msg.append("• Реалистичный потолок при работе над формой: ").append(getMeasurementsPotentialCeiling(rating, bmi)).append("\n");
        return msg.toString();
    }

    public static String compareToCelebrity(double rating, String gender) {
        String[] celebs = gender.equals("male")
            ? new String[]{"Генри Кавилл (9.5)", "Тимоти Шаламе (8.0)"}
            : new String[]{"Марго Робби (9.5)", "Зендая (8.5)"};
        String celeb = celebs[(int) (Math.random() * celebs.length)];
        return "⭐ *Сравнение:*\nВаш рейтинг: " + String.format("%.1f", rating) + "\nЗнаменитость: " + celeb;
    }

    public static String getRatingTypeLabel(String type) {
        switch (type) {
            case "face":
                return "Лицо";
            case "body":
                return "Тело";
            case "measure":
                return "Замеры";
            default:
                return type;
        }
    }

    private static void appendProgressLine(StringBuilder msg, double rating, Double previousRating) {
        if (previousRating == null) {
            msg.append("🆕 Это ваш первый результат в этом режиме.\n");
            return;
        }

        double delta = rating - previousRating;
        if (Math.abs(delta) < 0.05) {
            msg.append("➖ Почти без изменений относительно прошлого раза: ")
                .append(String.format("%.1f", previousRating)).append("\n");
        } else if (delta > 0) {
            msg.append("📈 Рост относительно прошлого раза: +")
                .append(String.format("%.1f", delta)).append("\n");
        } else {
            msg.append("📉 Просадка относительно прошлого раза: ")
                .append(String.format("%.1f", delta)).append("\n");
        }
    }

    private static String getPotentialLabel(double rating) {
        if (rating < 4) return "Низкий";
        if (rating < 7) return "Средний";
        return "Высокий";
    }

    private static String getLevelLabel(double rating) {
        if (rating < 3.5) return "Ниже среднего";
        if (rating < 5.5) return "Средний";
        if (rating < 7.5) return "Выше среднего";
        return "Сильный";
    }

    private static String getEditLabel(double rating) {
        return EditVerdict.fromFaceRating(rating).getLabel();
    }

    private static String buildPhotoBreakdown(double rating, String gender, String ratingType) {
        String typeLabel = "face".equals(ratingType) ? "лицо" : "body".equals(ratingType) ? "тело" : "внешность";
        if (rating < 3.5) {
            return "• Текущий " + typeLabel + "-результат выглядит сыровато.\n"
                + "• Нужна база: качество фото, свет, ухоженность, форма, подача.\n"
                + "• Быстрый буст даст не радикальная смена внешности, а устранение слабых мест в подаче.";
        }
        if (rating < 5.5) {
            return "• База уже читается, но пока без вау-эффекта.\n"
                + "• Есть запас за счет более сильной подачи, стиля и качества кадров.\n"
                + "• Для режима " + typeLabel + " ключевой буст даст более аккуратный визуальный образ.";
        }
        if (rating < 7.5) {
            return "• Результат уже уверенный, визуально есть сильные стороны.\n"
                + "• Основная задача сейчас - убрать мелкие минусы и усилить консистентность.\n"
                + "• " + ("male".equals(gender) ? "Мужской" : "Женский") + " образ можно заметно усилить грамотной подачей.";
        }
        return "• Результат сильный, образ уже цепляет и выглядит конкурентно.\n"
            + "• Дальше работает полировка: стиль, ракурсы, качество фото и сохранение формы.\n"
            + "• У вас уже не режим выживания, а режим тонкой настройки.";
    }

    private static String getPrimaryFocus(double rating, String ratingType) {
        if (rating < 3.5) return "собрать базу: сон, уход, форма и нормальные фото";
        if (rating < 5.5) return "улучшить подачу и убрать 1-2 самых заметных минуса";
        if (rating < 7.5) return "дожать стиль и консистентность результата";
        if ("face".equals(ratingType)) return "не портить сильную базу случайной подачей";
        return "держать форму и усиливать визуальную подачу";
    }

    private static String getPotentialCeiling(double rating) {
        if (rating < 3.5) return "около 5.0-5.5 при сильной базе";
        if (rating < 5.5) return "около 6.5-7.0 при хорошем апгрейде";
        if (rating < 7.5) return "около 7.8-8.4 при грамотной полировке";
        return "вы уже близко к своему верхнему диапазону";
    }

    private static String getMeasurementLevel(double rating) {
        if (rating < 3.5) return "🔴 Нужна плотная работа над базой";
        if (rating < 5.0) return "🟡 Средний уровень, но есть заметный запас";
        if (rating < 7.0) return "🟢 Хороший уровень";
        if (rating < 8.5) return "🟢 Очень сильный уровень";
        return "👑 Элитный уровень";
    }

    private static String getStatusIcon(boolean ok) {
        return ok ? "✅" : "⚠️";
    }

    private static String getMalePriority(double bmi, double shoulderWaistRatio, double chestWaistRatio, double waist) {
        if (bmi > 25.0) return "сушить талию и вернуть BMI ближе к 22-24";
        if (shoulderWaistRatio < 1.5) return "сделать V-форму сильнее: талия вниз, плечи вверх";
        if (chestWaistRatio < 1.20) return "добрать верх тела и не расширять талию";
        if (waist > 85) return "снизить талию хотя бы на 4-8 см";
        return "удерживать форму и шлифовать мелкие отклонения";
    }

    private static String getMaleUpgrade(double bmi, double shoulderWaistRatio, double waist) {
        if (bmi > 25.0 || waist > 85) return "уменьшение талии даст самый быстрый прирост";
        if (shoulderWaistRatio < 1.5) return "рост плечи/талия к зоне 1.50+ поднимет оценку сильнее всего";
        return "вы уже близко к сильному диапазону, дальше работает только полировка";
    }

    private static String getFemalePriority(double bmi, double waistHipsRatio, double chestWaistRatio, double waist) {
        if (bmi > 24.0) return "вернуть BMI ближе к 20-23";
        if (waistHipsRatio > 0.75) return "собрать талию, это даст главный визуальный буст";
        if (chestWaistRatio < 1.30) return "усилить контраст грудь/талия";
        if (waist > 75) return "снизить талию хотя бы на 4-7 см";
        return "удерживать пропорции и шлифовать подачу";
    }

    private static String getFemaleUpgrade(double bmi, double waistHipsRatio, double waist) {
        if (waistHipsRatio > 0.75 || waist > 75) return "уменьшение талии сильнее всего поднимет результат";
        if (bmi > 24.0) return "нормализация BMI даст самый заметный эффект";
        return "вы уже в хорошем диапазоне, дальше рост будет точечным";
    }

    private static String getMeasurementsPotentialCeiling(double rating, double bmi) {
        if (rating < 4.0) return bmi > 26 ? "около 6.0 после нормализации формы" : "около 5.8-6.3";
        if (rating < 6.0) return "около 7.0-7.5 при хорошем апгрейде";
        if (rating < 8.0) return "около 8.2-8.7 при тонкой настройке";
        return "вы уже рядом с верхней зоной";
    }
}
