package com.bot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatingUtilsTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("РўРµСЃС‚ 1: РћС†РµРЅРєР° Р»РёС†Р° РІ РґРёР°РїР°Р·РѕРЅРµ 1-10")
    void testEvaluateFace() {
        for (int i = 0; i < 50; i++) {
            double rating = RatingUtils.evaluateFace("test", "male");
            assertTrue(rating >= 1 && rating <= 10, "РћС†РµРЅРєР° РґРѕР»Р¶РЅР° Р±С‹С‚СЊ РѕС‚ 1 РґРѕ 10, РїРѕР»СѓС‡РµРЅРѕ: " + rating);
        }
    }

    @Test
    @DisplayName("РўРµСЃС‚ 2: РРґРµР°Р»СЊРЅС‹Рµ РїР°СЂР°РјРµС‚СЂС‹ РјСѓР¶С‡РёРЅС‹ = 10 Р±Р°Р»Р»РѕРІ")
    void testPerfectMaleMeasurements() {
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("chest", 100.0);
        measurements.put("waist", 80.0);
        double rating = RatingUtils.evaluateBodyByMeasurements(measurements, "male");
        assertEquals(10.0, rating, 0.01, "РРґРµР°Р» РјСѓР¶С‡РёРЅС‹ = 10 Р±Р°Р»Р»РѕРІ");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 3: РРґРµР°Р»СЊРЅС‹Рµ РїР°СЂР°РјРµС‚СЂС‹ Р¶РµРЅС‰РёРЅС‹ = 10 Р±Р°Р»Р»РѕРІ")
    void testPerfectFemaleMeasurements() {
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("chest", 90.0);
        measurements.put("waist", 65.0);
        measurements.put("hips", 90.0);
        double rating = RatingUtils.evaluateBodyByMeasurements(measurements, "female");
        assertEquals(10.0, rating, 0.01, "РРґРµР°Р» Р¶РµРЅС‰РёРЅС‹ = 10 Р±Р°Р»Р»РѕРІ");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 4: Р­РєСЃС‚СЂРµРјР°Р»СЊРЅС‹Рµ РїР°СЂР°РјРµС‚СЂС‹ = 1 Р±Р°Р»Р»")
    void testExtremeMeasurements() {
        Map<String, Double> measurements = new HashMap<>();
        measurements.put("chest", 200.0);
        measurements.put("waist", 200.0);
        double rating = RatingUtils.evaluateBodyByMeasurements(measurements, "male");
        assertEquals(1.0, rating, 0.1, "Р­РєСЃС‚СЂРёРј = 1 Р±Р°Р»Р»");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 5: РџСЂРµРјРёСѓРј СЃРѕРѕР±С‰РµРЅРёРµ СЃРѕРґРµСЂР¶РёС‚ РґРµС‚Р°Р»СЊРЅС‹Р№ СЂР°Р·Р±РѕСЂ")
    void testPremiumMessage() {
        String message = RatingUtils.generateRatingMessage(8.5, true, "male");
        assertTrue(message.contains("Р”РµС‚Р°Р»СЊРЅС‹Р№ СЂР°Р·Р±РѕСЂ"), "РџСЂРµРјРёСѓРј РґРѕР»Р¶РµРЅ СЃРѕРґРµСЂР¶Р°С‚СЊ РґРµС‚Р°Р»СЊРЅС‹Р№ СЂР°Р·Р±РѕСЂ");
        assertTrue(message.contains("8.5") || message.contains("8,5") || message.contains("8"), "Р”РѕР»Р¶РЅР° Р±С‹С‚СЊ РѕС†РµРЅРєР°");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 6: Р‘РµСЃРїР»Р°С‚РЅРѕРµ СЃРѕРѕР±С‰РµРЅРёРµ РќР• СЃРѕРґРµСЂР¶РёС‚ РґРµС‚Р°Р»СЊРЅС‹Р№ СЂР°Р·Р±РѕСЂ")
    void testFreeMessage() {
        String message = RatingUtils.generateRatingMessage(6.0, false, "female");
        assertFalse(message.contains("Р”РµС‚Р°Р»СЊРЅС‹Р№ СЂР°Р·Р±РѕСЂ"), "Р‘РµСЃРїР»Р°С‚РЅС‹Р№ РЅРµ РґРѕР»Р¶РµРЅ РёРјРµС‚СЊ РґРµС‚Р°Р»СЊРЅС‹Р№ СЂР°Р·Р±РѕСЂ");
        assertNotNull(message);
        assertTrue(message.length() > 0);
    }

    @Test
    @DisplayName("РўРµСЃС‚ 7: Р­РґРёС‚ 'С‚РµР±СЏ РјРѕРіРіР°СЋС‚' РґР»СЏ РѕС†РµРЅРєРё < 3.5")
    void testEditMoggat() {
        String message = RatingUtils.generateRatingMessage(3.4, true, "male");
        assertTrue(message.contains("РњРћР“Р“РђР®Рў") || message.contains("РјРѕРіРіР°СЋС‚"), "РћС†РµРЅРєР° 3.4 = РјРѕРіРіР°СЋС‚");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 8: Р­РґРёС‚ 'РЅРµР№С‚СЂР°Р»СЊРЅРѕ' РґР»СЏ РѕС†РµРЅРєРё 3.5-5.5")
    void testEditNeutral() {
        String message = RatingUtils.generateRatingMessage(4.5, true, "male");
        assertTrue(message.contains("РќР•Р™РўР РђР›Р¬РќРћ") || message.contains("РЅРµР№С‚СЂР°Р»СЊРЅРѕ"), "РћС†РµРЅРєР° 4.5 = РЅРµР№С‚СЂР°Р»СЊРЅРѕ");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 9: Р­РґРёС‚ 'С‚С‹ РјРѕРіРіР°РµС€СЊ' РґР»СЏ РѕС†РµРЅРєРё > 5.5")
    void testEditMoggaesh() {
        String message = RatingUtils.generateRatingMessage(8.0, true, "male");
        assertTrue(message.contains("РњРћР“Р“РђР•РЁР¬") || message.contains("РјРѕРіРіР°РµС€СЊ"), "РћС†РµРЅРєР° 8.0 = РјРѕРіРіР°РµС€СЊ");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 10: White Pill РґР»СЏ РѕС†РµРЅРєРё < 4.0")
    void testWhitePill() {
        String message = RatingUtils.generateRatingMessage(3.9, true, "male");
        assertTrue(message.contains("WHITE PILL") || message.contains("White"), "РћС†РµРЅРєР° < 4 РґРѕР»Р¶РЅР° РёРјРµС‚СЊ White Pill");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 11: РЎСЂР°РІРЅРµРЅРёРµ СЃРѕ Р·РЅР°РјРµРЅРёС‚РѕСЃС‚СЊСЋ РІРѕР·РІСЂР°С‰Р°РµС‚ РЅРµРїСѓСЃС‚СѓСЋ СЃС‚СЂРѕРєСѓ")
    void testCompareToCelebrityMale() {
        String result = RatingUtils.compareToCelebrity(9.0, "male");
        assertNotNull(result, "Р РµР·СѓР»СЊС‚Р°С‚ РЅРµ РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ null");
        assertTrue(result.length() > 0, "Р РµР·СѓР»СЊС‚Р°С‚ РЅРµ РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ РїСѓСЃС‚С‹Рј");
        assertTrue(result.contains("*") || result.contains("в­ђ"), "Р”РѕР»Р¶РЅРѕ Р±С‹С‚СЊ С„РѕСЂРјР°С‚РёСЂРѕРІР°РЅРёРµ");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 12: РЎСЂР°РІРЅРµРЅРёРµ СЃРѕ Р·РЅР°РјРµРЅРёС‚РѕСЃС‚СЊСЋ - Р¶РµРЅС‰РёРЅР°")
    void testCompareToCelebrityFemale() {
        String result = RatingUtils.compareToCelebrity(7.0, "female");
        assertNotNull(result, "Р РµР·СѓР»СЊС‚Р°С‚ РЅРµ РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ null");
        assertTrue(result.length() > 0, "Р РµР·СѓР»СЊС‚Р°С‚ РЅРµ РґРѕР»Р¶РµРЅ Р±С‹С‚СЊ РїСѓСЃС‚С‹Рј");
        assertTrue(result.contains("*") || result.contains("в­ђ"), "Р”РѕР»Р¶РЅРѕ Р±С‹С‚СЊ С„РѕСЂРјР°С‚РёСЂРѕРІР°РЅРёРµ");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 13: Р“РµРЅРµСЂР°С†РёСЏ СЃРѕРѕР±С‰РµРЅРёСЏ РІСЃРµРіРґР° РІРѕР·РІСЂР°С‰Р°РµС‚ РЅРµРїСѓСЃС‚СѓСЋ СЃС‚СЂРѕРєСѓ")
    void testMessageNotNull() {
        String message = RatingUtils.generateRatingMessage(5.0, false, "male");
        assertNotNull(message);
        assertTrue(message.length() > 50, "РЎРѕРѕР±С‰РµРЅРёРµ РґРѕР»Р¶РЅРѕ Р±С‹С‚СЊ РґРѕСЃС‚Р°С‚РѕС‡РЅРѕ РґР»РёРЅРЅС‹Рј");
    }

    @Test
    @DisplayName("РўРµСЃС‚ 14: Р Р°Р·РЅС‹Рµ РїРѕР»С‹ СЂР°Р±РѕС‚Р°СЋС‚ РєРѕСЂСЂРµРєС‚РЅРѕ")
    void testDifferentGenders() {
        String maleMsg = RatingUtils.generateRatingMessage(7.0, true, "male");
        String femaleMsg = RatingUtils.generateRatingMessage(7.0, true, "female");
        assertNotNull(maleMsg);
        assertNotNull(femaleMsg);
        assertTrue(maleMsg.length() > 0);
        assertTrue(femaleMsg.length() > 0);
    }

    @Test
    @DisplayName("РўРµСЃС‚ 15: РџСЂРѕРІРµСЂРєР° С‡С‚Рѕ РјРµС‚РѕРґ generateRatingMessage РЅРµ РїР°РґР°РµС‚ СЃ РѕС€РёР±РєРѕР№")
    void testNoExceptions() {
        assertDoesNotThrow(() -> {
            RatingUtils.generateRatingMessage(5.0, true, "male");
            RatingUtils.generateRatingMessage(5.0, false, "female");
            RatingUtils.compareToCelebrity(8.0, "male");
            RatingUtils.compareToCelebrity(8.0, "female");
        });
    }

    @Test
    @DisplayName("РўРµСЃС‚ 16: Р’РµСЂРґРёРєС‚ mogger/mogged РµСЃС‚СЊ С‚РѕР»СЊРєРѕ Сѓ face-РѕС†РµРЅРєРё")
    void testEditVerdictOnlyForFaceRating() {
        String bodyMessage = RatingUtils.generateRatingMessage(8.0, true, "male", null, "body");

        assertFalse(bodyMessage.contains("РњРћР“Р“РђР®Рў"));
        assertFalse(bodyMessage.contains("РњРћР“Р“РђР•РЁР¬"));
        assertFalse(bodyMessage.contains("РќР•Р™РўР РђР›Р¬РќРћ"));
    }

    @Test
    @DisplayName("РўРµСЃС‚ 17: Р­РґРёС‚ СЃРѕР·РґР°РµС‚СЃСЏ С‚РѕР»СЊРєРѕ РґР»СЏ face Рё С‚РѕР»СЊРєРѕ РЅРµ РЅР° neutral")
    void testFaceEditCreationRules() {
        assertTrue(FaceEditService.shouldCreateFaceEdit("face", 3.4));
        assertFalse(FaceEditService.shouldCreateFaceEdit("face", 4.5));
        assertTrue(FaceEditService.shouldCreateFaceEdit("face", 8.0));
        assertFalse(FaceEditService.shouldCreateFaceEdit("body", 8.0));
    }

    @Test
    @DisplayName("РўРµСЃС‚ 18: Р“СЂР°РЅРёС†С‹ mogged neutral mogger СЃРѕРІРїР°РґР°СЋС‚ СЃ РѕС†РµРЅРєРѕР№")
    void testEditVerdictBoundaries() {
        assertEquals(EditVerdict.MOGGED, EditVerdict.fromFaceRating(3.49));
        assertEquals(EditVerdict.NEUTRAL, EditVerdict.fromFaceRating(3.5));
        assertEquals(EditVerdict.NEUTRAL, EditVerdict.fromFaceRating(5.5));
        assertEquals(EditVerdict.MOGGER, EditVerdict.fromFaceRating(5.51));
    }

    @Test
    @DisplayName("РўРµСЃС‚ 19: Шаблон мужского эдита ищется внутри папки male")
    void testMaleTemplateLookupInsideGenderFolder() throws Exception {
        Path maleDir = Files.createDirectories(tempDir.resolve("male"));
        Path expected = Files.createFile(maleDir.resolve("mogged.MOV"));

        assertEquals(expected, FaceEditService.findTemplate(tempDir, "male", EditVerdict.MOGGED));
    }

    @Test
    @DisplayName("РўРµСЃС‚ 20: Женский шаблон ищется и по woman, и по uppercase MOV")
    void testFemaleTemplateLookupSupportsWomanAlias() throws Exception {
        Path expected = Files.createFile(tempDir.resolve("woman_mogger.MOV"));

        assertEquals(expected, FaceEditService.findTemplate(tempDir, "female", EditVerdict.MOGGER));
    }
}
