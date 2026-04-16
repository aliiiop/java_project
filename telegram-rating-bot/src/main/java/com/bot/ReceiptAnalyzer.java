package com.bot;

import com.google.gson.*;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ReceiptAnalyzer {
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_KEY = "sk-ant-d01_20250416_7b37f4ffa4b04fa8bb82d74acdcf79bb_banned";
    private static final String EXPECTED_CARD = "4400430354456268";
    private static final String EXPECTED_AMOUNT = "990";
    private static final String EXPECTED_CURRENCY = "тг";

    public static class AnalysisResult {
        public boolean isValid;
        public String message;
        public String detailedAnalysis;

        public AnalysisResult(boolean isValid, String message, String analysis) {
            this.isValid = isValid;
            this.message = message;
            this.detailedAnalysis = analysis;
        }
    }

    public static AnalysisResult analyzeReceipt(String photoPath) throws Exception {
        // Читаем фото и конвертируем в base64
        byte[] imageBytes = Files.readAllBytes(Paths.get(photoPath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mediaType = getMediaType(photoPath);

        // Создаём запрос к Claude Vision API
        String prompt = "Проанализируй это фото чека/транзакции и ответь на следующие вопросы в формате JSON:\n" +
            "1. Это ли фото реального банковского чека или скриншота перевода? (check_is_real: true/false)\n" +
            "2. Какая сумма указана в чеке? Напиши только число и валюту (amount: \"число валюта\")\n" +
            "3. На какую карту/счёт переведены деньги? (recipient_card: \"номер или описание\")\n" +
            "4. Какое имя получателя указано? (recipient_name: \"имя\")\n" +
            "5. Какая дата транзакции? (date: \"дата\")\n" +
            "6. Какое назначение платежа? (purpose: \"назначение\")\n" +
            "7. Читаемо ли фото? (is_readable: true/false)\n\n" +
            "Ответь ТОЛЬКО JSON без дополнительного текста:\n" +
            "{\"check_is_real\": boolean, \"amount\": \"string\", \"recipient_card\": \"string\", \"recipient_name\": \"string\", \"date\": \"string\", \"purpose\": \"string\", \"is_readable\": boolean}";

        String jsonPayload = createVisionRequest(base64Image, mediaType, prompt);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(CLAUDE_API_URL))
            .header("Content-Type", "application/json")
            .header("x-api-key", API_KEY)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return new AnalysisResult(false,
                    "❌ Ошибка при анализе чека (код " + response.statusCode() + ")",
                    "API ответил ошибкой: " + response.body());
            }

            // Парсим ответ
            JsonObject responseObj = JsonParser.parseString(response.body()).getAsJsonObject();
            String analysisText = extractTextFromResponse(responseObj);

            // Парсим JSON из ответа Claude
            JsonObject analysisJson = parseAnalysisJson(analysisText);
            return validateReceipt(analysisJson);

        } catch (Exception e) {
            return new AnalysisResult(false,
                "❌ Ошибка при обработке чека: " + e.getMessage(),
                e.toString());
        }
    }

    private static String createVisionRequest(String base64Image, String mediaType, String prompt) {
        JsonObject imageSource = new JsonObject();
        imageSource.addProperty("type", "base64");
        imageSource.addProperty("media_type", mediaType);
        imageSource.addProperty("data", base64Image);

        JsonObject imageContent = new JsonObject();
        imageContent.addProperty("type", "image");
        imageContent.add("source", imageSource);

        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", prompt);

        JsonArray content = new JsonArray();
        content.add(imageContent);
        content.add(textContent);

        JsonObject request = new JsonObject();
        request.addProperty("model", "claude-3-5-sonnet-20241022");
        request.add("messages", new JsonArray());
        request.getAsJsonArray("messages").add(createMessage(content));
        request.addProperty("max_tokens", 1024);

        return request.toString();
    }

    private static JsonObject createMessage(JsonArray content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.add("content", content);
        return message;
    }

    private static String extractTextFromResponse(JsonObject responseObj) {
        try {
            JsonArray content = responseObj.getAsJsonArray("content");
            if (content != null && content.size() > 0) {
                JsonObject firstContent = content.get(0).getAsJsonObject();
                return firstContent.get("text").getAsString();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при извлечении текста: " + e.getMessage());
        }
        return "{}";
    }

    private static JsonObject parseAnalysisJson(String analysisText) {
        try {
            // Ищем JSON в ответе
            int jsonStart = analysisText.indexOf('{');
            int jsonEnd = analysisText.lastIndexOf('}');
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonStr = analysisText.substring(jsonStart, jsonEnd + 1);
                return JsonParser.parseString(jsonStr).getAsJsonObject();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при парсе JSON: " + e.getMessage());
        }
        return new JsonObject();
    }

    private static AnalysisResult validateReceipt(JsonObject analysis) {
        StringBuilder issues = new StringBuilder();
        boolean isValid = true;

        // Проверка читаемости
        if (!getBoolean(analysis, "is_readable", false)) {
            issues.append("• Чек нечитаемый или низкого качества\n");
            isValid = false;
        }

        // Проверка что это реальный чек
        if (!getBoolean(analysis, "check_is_real", false)) {
            issues.append("• Это не выглядит как реальный чек\n");
            isValid = false;
        }

        // Проверка суммы
        String amount = getString(analysis, "amount", "");
        if (!amount.toLowerCase().contains(EXPECTED_AMOUNT) || !amount.toLowerCase().contains("тг")) {
            issues.append("• Сумма должна быть ").append(EXPECTED_AMOUNT).append(" тг, найдено: ").append(amount).append("\n");
            isValid = false;
        }

        // Проверка номера карты (последние 4 цифры)
        String recipientCard = getString(analysis, "recipient_card", "");
        if (!recipientCard.toLowerCase().contains(EXPECTED_CARD.substring(12))) { // последние 4 цифры
            issues.append("• Номер карты не совпадает (ожидается ...6268)\n");
            isValid = false;
        }

        // Проверка назначения
        String purpose = getString(analysis, "purpose", "").toLowerCase();
        if (!purpose.contains("премиум") && !purpose.contains("подписка") && !purpose.contains("premium")) {
            issues.append("• Назначение платежа должно содержать 'Премиум подписка'\n");
            isValid = false;
        }

        String detailedInfo = String.format(
            "📊 Анализ чека:\n" +
            "• Сумма: %s\n" +
            "• Получатель (карта): %s\n" +
            "• Имя получателя: %s\n" +
            "• Дата: %s\n" +
            "• Назначение: %s\n",
            amount,
            recipientCard,
            getString(analysis, "recipient_name", "?"),
            getString(analysis, "date", "?"),
            purpose
        );

        if (!isValid) {
            detailedInfo += "\n❌ Проблемы:\n" + issues;
        }

        String message = isValid
            ? "✅ Чек прошёл проверку!"
            : "❌ Чек не прошёл проверку";

        return new AnalysisResult(isValid, message, detailedInfo);
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при получении строки " + key + ": " + e.getMessage());
        }
        return defaultValue;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsBoolean();
            }
        } catch (Exception e) {
            System.err.println("Ошибка при получении boolean " + key + ": " + e.getMessage());
        }
        return defaultValue;
    }

    private static String getMediaType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    // Метод для скачивания файла с Telegram
    public static String downloadTelegramFile(String fileId, TelegramLongPollingBot bot, String outputPath) throws Exception {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(fileId);

        File tgFile = bot.execute(getFileMethod);
        String downloadUrl = "https://api.telegram.org/file/bot" + bot.getBotToken() + "/" + tgFile.getFilePath();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .GET()
            .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            Files.write(Paths.get(outputPath), response.body());
            return outputPath;
        } else {
            throw new Exception("Не удалось скачать файл с Telegram: " + response.statusCode());
        }
    }
}
