package com.bot;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class FaceAnalyzer {
    
    /**
     * Анализирует лицо и возвращает структурированный разбор
     */
    public static FaceAnalysisResult analyzeFaceAttributes(JsonObject faceResponse) {
        FaceAnalysisResult result = new FaceAnalysisResult();
        
        if (!faceResponse.has("faces") || faceResponse.getAsJsonArray("faces").isEmpty()) {
            result.error = "Лицо не найдено";
            return result;
        }
        
        JsonObject face = faceResponse.getAsJsonArray("faces").get(0).getAsJsonObject();
        JsonObject attributes = face.getAsJsonObject("attributes");
        
        // 1. Анализ эмоций и выражения
        if (attributes.has("emotion")) {
            JsonObject emotion = attributes.getAsJsonObject("emotion");
            double happiness = emotion.get("happiness").getAsDouble();
            double neutral = emotion.get("neutral").getAsDouble();
            
            if (happiness > 70) {
                result.plus.add("😊 Отличная улыбка! Это сильно повышает привлекательность.");
            } else if (happiness < 30 && neutral > 70) {
                result.minus.add("😐 Нейтральное выражение лица. Улыбка добавит +0.5-1 балл.");
            }
        }
        
        // 2. Анализ глаз (мешки, темные круги)
        if (attributes.has("eyestatus")) {
            JsonObject eyes = attributes.getAsJsonObject("eyestatus");
            
            if (eyes.has("left_eye_status")) {
                double leftDark = eyes.getAsJsonObject("left_eye_status").get("dark_circle").getAsDouble();
                double rightDark = eyes.getAsJsonObject("right_eye_status").get("dark_circle").getAsDouble();
                
                if (leftDark > 0.5 || rightDark > 0.5) {
                    result.minus.add("👁️ Заметны тёмные круги под глазами. Высыпайся и пей воду.");
                }
            }
        }
        
        // 3. Анализ возраста
        if (attributes.has("age")) {
            int age = attributes.getAsJsonObject("age").get("value").getAsInt();
            if (age < 25) {
                result.plus.add("🌟 Молодость — твоё преимущество.");
            } else if (age > 40) {
                result.minus.add("👴 Возрастные изменения заметны. Уход за кожей поможет.");
            }
        }
        
        // 4. Анализ кожи
        if (attributes.has("skinstatus")) {
            JsonObject skin = attributes.getAsJsonObject("skinstatus");
            
            if (skin.has("acne")) {
                double acne = skin.get("acne").getAsDouble();
                if (acne > 0.3) {
                    result.minus.add("🧴 Есть проблемы с кожей (акне). Уход за кожей — приоритет №1.");
                }
            }
            
            if (skin.has("health")) {
                double health = skin.get("health").getAsDouble();
                if (health > 0.7) {
                    result.plus.add("✨ Отличное состояние кожи! Это большой плюс.");
                }
            }
        }
        
        // 5. Анализ очков
        if (attributes.has("glass")) {
            JsonObject glass = attributes.getAsJsonObject("glass");
            double value = glass.get("value").getAsDouble();
            if (value > 0.8) {
                result.minus.add("👓 Очки могут скрывать черты лица. Попробуй контактные линзы.");
            }
        }
        
        // 6. Анализ макияжа (для женщин)
        if (attributes.has("makeup")) {
            JsonObject makeup = attributes.getAsJsonObject("makeup");
            
            if (makeup.has("lip_makeup")) {
                double lip = makeup.get("lip_makeup").getAsDouble();
                if (lip < 0.3) {
                    result.minus.add("💄 Добавь акцент на губы — это привлекает внимание.");
                }
            }
        }
        
        // 7. Общие выводы
        if (result.plus.isEmpty() && result.minus.isEmpty()) {
            result.neutral = "Средние показатели. Для улучшения нужен детальный анализ черт лица.";
        }
        
        return result;
    }
    
    public static class FaceAnalysisResult {
        public java.util.List<String> plus = new java.util.ArrayList<>();
        public java.util.List<String> minus = new java.util.ArrayList<>();
        public String neutral = "";
        public String error = "";
        
        public boolean hasData() {
            return error.isEmpty();
        }
        
        public String formatForPremium() {
            if (!error.isEmpty()) {
                return "❌ " + error;
            }
            
            StringBuilder sb = new StringBuilder();
            
            if (!plus.isEmpty()) {
                sb.append("✅ *Твои сильные стороны:*\n");
                for (String p : plus) {
                    sb.append("• ").append(p).append("\n");
                }
                sb.append("\n");
            }
            
            if (!minus.isEmpty()) {
                sb.append("⚠️ *Что можно улучшить:*\n");
                for (String m : minus) {
                    sb.append("• ").append(m).append("\n");
                }
                sb.append("\n");
            }
            
            if (!neutral.isEmpty()) {
                sb.append("ℹ️ ").append(neutral).append("\n");
            }
            
            if (plus.isEmpty() && minus.isEmpty() && neutral.isEmpty()) {
                sb.append("📊 Нет явных сильных или слабых сторон. Работай над общей подачей.");
            }
            
            return sb.toString();
        }
    }
}
