package com.bot;

import org.junit.jupiter.api.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RatingUtilsTest {

    @Test
    @DisplayName("Тест 1: Оценка лица в диапазоне 1-10")
    void testEvaluateFace() {
        for (int i = 0; i < 50; i++) {
            double rating = RatingUtils.evaluateFace("test", "male");
            assertTrue(rating >= 1 && rating <= 10, "Оценка должна быть от 1 до 10, получено: " + rating);
        }
    }

    @Test
    @DisplayName("Тест 2: Идеальные параметры мужчины = 10 баллов")
    void testPerfectMaleMeasurements() {
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("chest", 100.0);
        measurements.put("waist", 80.0);
        double rating = RatingUtils.evaluateBodyByMeasurements(measurements, "male");
        assertEquals(10.0, rating, 0.01, "Идеал мужчины = 10 баллов");
    }

    @Test
    @DisplayName("Тест 3: Идеальные параметры женщины = 10 баллов")
    void testPerfectFemaleMeasurements() {
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("chest", 90.0);
        measurements.put("waist", 65.0);
        measurements.put("hips", 90.0);
        double rating = RatingUtils.evaluateBodyByMeasurements(measurements, "female");
        assertEquals(10.0, rating, 0.01, "Идеал женщины = 10 баллов");
    }

    @Test
    @DisplayName("Тест 4: Экстремальные параметры = 1 балл")
    void testExtremeMeasurements() {
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("chest", 200.0);
        measurements.put("waist", 200.0);
        double rating = RatingUtils.evaluateBodyByMeasurements(measurements, "male");
        assertEquals(1.0, rating, 0.1, "Экстрим = 1 балл");
    }

    @Test
    @DisplayName("Тест 5: Премиум сообщение содержит детальный разбор")
    void testPremiumMessage() {
        String message = RatingUtils.generateRatingMessage(8.5, true, "male");
        assertTrue(message.contains("Детальный разбор"), "Премиум должен содержать детальный разбор");
        // Проверяем наличие цифры 8 (может быть без десятых)
        assertTrue(message.contains("8.5") || message.contains("8,5") || message.contains("8"), "Должна быть оценка");
    }

    @Test
    @DisplayName("Тест 6: Бесплатное сообщение НЕ содержит детальный разбор")
    void testFreeMessage() {
        String message = RatingUtils.generateRatingMessage(6.0, false, "female");
        // Проверяем что нет детального разбора
        assertFalse(message.contains("Детальный разбор"), "Бесплатный не должен иметь детальный разбор");
        // Проверяем что сообщение не пустое
        assertNotNull(message);
        assertTrue(message.length() > 0);
    }

    @Test
    @DisplayName("Тест 7: Эдит 'тебя моггают' для оценки < 3.5")
    void testEditMoggat() {
        String message = RatingUtils.generateRatingMessage(3.4, true, "male");
        assertTrue(message.contains("МОГГАЮТ") || message.contains("моггают"), "Оценка 3.4 = моггают");
    }

    @Test
    @DisplayName("Тест 8: Эдит 'нейтрально' для оценки 3.5-5.5")
    void testEditNeutral() {
        String message = RatingUtils.generateRatingMessage(4.5, true, "male");
        assertTrue(message.contains("НЕЙТРАЛЬНО") || message.contains("нейтрально"), "Оценка 4.5 = нейтрально");
    }

    @Test
    @DisplayName("Тест 9: Эдит 'ты моггаешь' для оценки > 5.5")
    void testEditMoggaesh() {
        String message = RatingUtils.generateRatingMessage(8.0, true, "male");
        assertTrue(message.contains("МОГГАЕШЬ") || message.contains("моггаешь"), "Оценка 8.0 = моггаешь");
    }

    @Test
    @DisplayName("Тест 10: White Pill для оценки < 4.0")
    void testWhitePill() {
        String message = RatingUtils.generateRatingMessage(3.9, true, "male");
        assertTrue(message.contains("WHITE PILL") || message.contains("White"), "Оценка < 4 должна иметь White Pill");
    }

    @Test
    @DisplayName("Тест 11: Сравнение со знаменитостью возвращает непустую строку")
    void testCompareToCelebrityMale() {
        String result = RatingUtils.compareToCelebrity(9.0, "male");
        assertNotNull(result, "Результат не должен быть null");
        assertTrue(result.length() > 0, "Результат не должен быть пустым");
        // Проверяем что есть звездочки (markdown форматирование)
        assertTrue(result.contains("*") || result.contains("⭐"), "Должно быть форматирование");
    }

    @Test
    @DisplayName("Тест 12: Сравнение со знаменитостью - женщина")
    void testCompareToCelebrityFemale() {
        String result = RatingUtils.compareToCelebrity(7.0, "female");
        assertNotNull(result, "Результат не должен быть null");
        assertTrue(result.length() > 0, "Результат не должен быть пустым");
        assertTrue(result.contains("*") || result.contains("⭐"), "Должно быть форматирование");
    }

    @Test
    @DisplayName("Тест 13: Генерация сообщения всегда возвращает непустую строку")
    void testMessageNotNull() {
        String message = RatingUtils.generateRatingMessage(5.0, false, "male");
        assertNotNull(message);
        assertTrue(message.length() > 50, "Сообщение должно быть достаточно длинным");
    }

    @Test
    @DisplayName("Тест 14: Разные полы работают корректно")
    void testDifferentGenders() {
        String maleMsg = RatingUtils.generateRatingMessage(7.0, true, "male");
        String femaleMsg = RatingUtils.generateRatingMessage(7.0, true, "female");
        assertNotNull(maleMsg);
        assertNotNull(femaleMsg);
        assertTrue(maleMsg.length() > 0);
        assertTrue(femaleMsg.length() > 0);
    }
    
    @Test
    @DisplayName("Тест 15: Проверка что метод generateRatingMessage не падает с ошибкой")
    void testNoExceptions() {
        assertDoesNotThrow(() -> {
            RatingUtils.generateRatingMessage(5.0, true, "male");
            RatingUtils.generateRatingMessage(5.0, false, "female");
            RatingUtils.compareToCelebrity(8.0, "male");
            RatingUtils.compareToCelebrity(8.0, "female");
        });
    }

    @Test
    @DisplayName("Тест 16: Вердикт mogger/mogged есть только у face-оценки")
    void testEditVerdictOnlyForFaceRating() {
        String bodyMessage = RatingUtils.generateRatingMessage(8.0, true, "male", null, "body");

        assertFalse(bodyMessage.contains("МОГГАЮТ"));
        assertFalse(bodyMessage.contains("МОГГАЕШЬ"));
        assertFalse(bodyMessage.contains("НЕЙТРАЛЬНО"));
    }

    @Test
    @DisplayName("Тест 17: Эдит создается только для face и только не на neutral")
    void testFaceEditCreationRules() {
        assertTrue(FaceEditService.shouldCreateFaceEdit("face", 3.4));
        assertFalse(FaceEditService.shouldCreateFaceEdit("face", 4.5));
        assertTrue(FaceEditService.shouldCreateFaceEdit("face", 8.0));
        assertFalse(FaceEditService.shouldCreateFaceEdit("body", 8.0));
    }
}
