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

    private static final String API_KEY = "OaYVpQCCGCKCCz-aRuucehTNpR8J5BfX";
    private static final String API_SECRET = "J-UmTHAQnJ7Qt39ZZr1TG31J81wN2iN9";
    private static final String DETECT_URL = "https://api-us.faceplusplus.com/facepp/v3/detect";
    
    private static String lastAnalysis = "";

    public static double analyzeFace(String photoUrl) {
        FaceAnalysisResult result = analyzeFaceFull(photoUrl);
        return result.rating;
    }
    
    public static String getPremiumAnalysis() {
        if (lastAnalysis == null || lastAnalysis.isEmpty()) {
            return "❌ Нет данных для анализа. Сначала получи оценку.";
        }
        return lastAnalysis;
    }
    
    public static FaceAnalysisResult analyzeFaceFull(String photoUrl) {
        FaceAnalysisResult result = new FaceAnalysisResult();

        try {
            byte[] imageBytes = downloadImage(photoUrl);
            JsonObject response = callFacePlusPlus(imageBytes);

            result.rating = getRawBeautyScore(response);

            if (result.rating < 0) {
                result.faceDetected = false;
                result.analysisText = "😶 На фото не обнаружено лицо.\n\nПожалуйста, скиньте фотографию, где чётко видно лицо анфас (без маски, солнечных очков или сильного наклона).";
            } else {
                result.analysisText = parseAnalysis(response);
            }

            lastAnalysis = result.analysisText;
            return result;
        } catch (Exception e) {
            System.err.println("❌ Ошибка Face++: " + e.getMessage());
            result.faceDetected = false;
            result.rating = -1.0;
            result.analysisText = "❌ Не удалось проанализировать фото. Попробуй другое изображение.";
            lastAnalysis = result.analysisText;
            return result;
        }
    }
    
    private static JsonObject callFacePlusPlus(byte[] imageBytes) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(DETECT_URL);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("api_key", API_KEY);
            builder.addTextBody("api_secret", API_SECRET);
            builder.addTextBody("return_attributes", "gender,age,smiling,beauty");
            builder.addBinaryBody("image_file", imageBytes, ContentType.IMAGE_JPEG, "face.jpg");
            post.setEntity(builder.build());
            
            try (CloseableHttpResponse response = client.execute(post)) {
                String json = EntityUtils.toString(response.getEntity());
                System.out.println("📡 Face++ ответ: " + json);
                return JsonParser.parseString(json).getAsJsonObject();
            }
        }
    }
    
    private static double getRawBeautyScore(JsonObject response) {
        if (response.has("error_message") || !response.has("faces") || response.getAsJsonArray("faces").isEmpty()) {
            return -1.0;
        }

        JsonObject face = response.getAsJsonArray("faces").get(0).getAsJsonObject();
        JsonObject attributes = face.getAsJsonObject("attributes");

        if (attributes.has("beauty")) {
            JsonObject beauty = attributes.getAsJsonObject("beauty");
            double score;
            if (beauty.has("female_score")) {
                score = beauty.get("female_score").getAsDouble();
            } else {
                score = beauty.get("male_score").getAsDouble();
            }
            return Math.max(1.0, Math.min(10.0, score / 10.0));
        }
        return -1.0;
    }
    
    private static String parseAnalysis(JsonObject response) {
        if (!response.has("faces") || response.getAsJsonArray("faces").isEmpty()) {
            return "❌ Лицо не найдено на фото. Отправь чёткое фото анфас.";
        }
        
        JsonObject face = response.getAsJsonArray("faces").get(0).getAsJsonObject();
        JsonObject attributes = face.getAsJsonObject("attributes");
        
        StringBuilder sb = new StringBuilder();
        
        // Оценка красоты
        if (attributes.has("beauty")) {
            JsonObject beauty = attributes.getAsJsonObject("beauty");
            double score = beauty.has("female_score") ? beauty.get("female_score").getAsDouble() : beauty.get("male_score").getAsDouble();
            sb.append("📊 *Оценка Face++:* ").append(String.format("%.1f", score)).append("/100\n\n");
        }
        
        // Возраст
        if (attributes.has("age")) {
            int age = attributes.getAsJsonObject("age").get("value").getAsInt();
            sb.append("📅 *Возраст:* ~").append(age).append(" лет\n");
        }
        
        // Улыбка
        if (attributes.has("smiling")) {
            double smile = attributes.getAsJsonObject("smiling").get("value").getAsDouble();
            sb.append("😊 *Улыбка:* ").append(String.format("%.0f", smile)).append("%\n");
        }
        
        // Пол
        if (attributes.has("gender")) {
            String gender = attributes.getAsJsonObject("gender").get("value").getAsString();
            sb.append("🧬 *Пол:* ").append(gender).append("\n");
        }
        
        return sb.toString();
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
    
    public static class FaceAnalysisResult {
        public double rating;
        public String analysisText;
        public boolean faceDetected = true;
    }
}
