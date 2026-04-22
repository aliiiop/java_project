package com.bot;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;

public class FacePlusPlusAnalyzer {

    // 🔁 ЗАМЕНИ НА СВОИ КЛЮЧИ!
    private static final String API_KEY = "OaYVpQCCGCKCCz-aRuucehTNpR8J5BfX";
    private static final String API_SECRET = "J-UmTHAQnJ7Qt39ZZr1TG31J81wN2iN9";
    
    private static final String DETECT_URL = "https://api-us.faceplusplus.com/facepp/v3/detect";

    /**
     * Анализирует фото и возвращает оценку PSL (1-10)
     */
    public static double analyzeFace(String photoUrl) {
        try {
            // 1. Скачиваем фото
            byte[] imageBytes = downloadImage(photoUrl);
            
            // 2. Отправляем в Face++ API
            JsonObject response = callFacePlusPlus(imageBytes);
            
            // 3. Парсим результат и считаем оценку
            double rating = calculateRatingFromResponse(response);
            
            System.out.println("🤖 Face++ вернул оценку: " + rating);
            return rating;
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка Face++: " + e.getMessage());
            return 5.0;
        }
    }
    
    private static JsonObject callFacePlusPlus(byte[] imageBytes) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(DETECT_URL);
            
            // Формируем multipart запрос с ключами и фото
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("api_key", API_KEY);
            builder.addTextBody("api_secret", API_SECRET);
            builder.addTextBody("return_attributes", "gender,age,smiling,beauty,eyestatus,emotion"); // запрашиваем атрибуты
            builder.addBinaryBody("image_file", imageBytes, ContentType.IMAGE_JPEG, "face.jpg");
            
            post.setEntity(builder.build());
            
            try (CloseableHttpResponse response = client.execute(post)) {
                String json = EntityUtils.toString(response.getEntity());
                System.out.println("📡 Face++ ответ: " + json);
                return JsonParser.parseString(json).getAsJsonObject();
            }
        }
    }
    
    private static double calculateRatingFromResponse(JsonObject response) {
        // Проверяем ошибки
        if (response.has("error_message")) {
            System.err.println("Face++ ошибка: " + response.get("error_message").getAsString());
            return 5.0;
        }
        
        // Проверяем, есть ли лица
        if (!response.has("faces") || response.getAsJsonArray("faces").isEmpty()) {
            System.err.println("Лицо не найдено!");
            return 5.0;
        }
        
        // Берём первое лицо
        JsonObject face = response.getAsJsonArray("faces").get(0).getAsJsonObject();
        JsonObject attributes = face.getAsJsonObject("attributes");
        
        // 1. Оценка красоты от Face++ (0-100 → 1-10)
        double beautyScore = 5.0;
        if (attributes.has("beauty")) {
            JsonObject beauty = attributes.getAsJsonObject("beauty");
            if (beauty.has("female_score")) {
                beautyScore = beauty.get("female_score").getAsDouble() / 10.0;
            } else if (beauty.has("male_score")) {
                beautyScore = beauty.get("male_score").getAsDouble() / 10.0;
            }
        }
        
        // 2. Бонус за улыбку
        double smileBonus = 0;
        if (attributes.has("smile") && attributes.getAsJsonObject("smile").has("value")) {
            double smile = attributes.getAsJsonObject("smile").get("value").getAsDouble();
            smileBonus = smile * 0.5; // макс +0.5
        }
        
        // 3. Возрастной бонус (18-35 лет - оптимум)
        double ageBonus = 0;
        if (attributes.has("age") && attributes.getAsJsonObject("age").has("value")) {
            int age = attributes.getAsJsonObject("age").get("value").getAsInt();
            if (age >= 18 && age <= 35) {
                ageBonus = 0.5;
            } else if (age > 50) {
                ageBonus = -0.5;
            }
        }
        
        // Итоговая оценка
        double rating = beautyScore + smileBonus + ageBonus;
        rating = Math.max(1.0, Math.min(10.0, rating));
        rating = Math.round(rating * 10.0) / 10.0;
        
        System.out.println("📊 Beauty: " + beautyScore + ", SmileBonus: " + smileBonus + ", AgeBonus: " + ageBonus);
        
        return rating;
    }
    
    private static byte[] downloadImage(String imageUrl) throws Exception {
        try (InputStream in = new java.net.URL(imageUrl).openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return out.toByteArray();
        }
    }
}
