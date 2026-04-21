package com.bot;

import com.google.gson.*;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ReceiptAnalyzer {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_KEY = "sk-ant-api03-Sq6...RwAA"; // <- вставь свой ключ
    private static final String EXPECTED_CARD = "4400430354456268";
    private static final String EXPECTED_AMOUNT = "990";

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

    // Скачиваем фото с Telegram
    public static String downloadTelegramFile(String fileId, TelegramLongPollingBot bot, String outputPath) throws Exception {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(fileId);

        org.telegram.telegrambots.meta.api.objects.File tgFile = bot.execute(getFileMethod);
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
            throw new Exception("Не удалось скачать файл: " + response.statusCode());
        }
    }

    // Анализируем чек через Claude Vision API
    public static AnalysisResult analyzeReceipt(String photoPath) throws Exception {
        byte[] imageBytes = Files.readAllBytes(Paths.get(photoPath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mediaType = photoPath.endsWith(".png") ? "image/png" : "image/jpeg";

        String prompt = "Проанализируй фото чека/перевода и ответь ТОЛЬКО в формате JSON:\n" +
            "{\"check_is_real\": boolean, \"amount\": \"строка\", \"recipient_card\": \"строка\", " +
            "\"recipient_name\": \"строка\", \"date\": \"строка\", \"purpose\": \"строка\", \"is_readable\": boolean}\n\n" +
            "Поля:\n" +
            "check_is_real — это реальный чек/скриншот перевода (не просто картинка)?\n" +
            "amount — сумма и валюта (например: 990 тг)\n" +
            "recipient_card — номер карты получателя\n" +
            "recipient_name — имя получателя\n" +
            "date — дата транзакции\n" +
            "purpose — назначение платежа\n" +
            "is_readable — фото чёткое и читаемое?";

        // Формируем JSON запрос к Claude
        JsonObject imageSource = new JsonObject();
        imageSource.addProperty("type", "base64");
        imageSource.addProperty("media_type", mediaType);
        imageSource.addProperty("data", base64Image);

        JsonObject imageBlock = new JsonObject();
        imageBlock.addProperty("type", "image");
        imageBlock.add("source", imageSource);

        JsonObject textBlock = new JsonObject();
        textBlock.addProperty("type", "text");
        textBlock.addProperty("text", prompt);

        JsonArray content = new JsonArray();
        content.add(imageBlock);
        content.add(textBlock);

        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", "user");
        messageObj.add("content", content);

        JsonArray messages = new JsonArray();
        messages.add(messageObj);

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "claude-3-5-sonnet-20241022");
        payload.add("messages", messages);
        payload.addProperty("max_tokens", 512);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(CLAUDE_API_URL))
            .header("Content-Type", "application/json")
            .header("x-api-key", API_KEY)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return new AnalysisResult(false,
                "❌ Ошибка API (код " + response.statusCode() + ")",
                "Не удалось проверить чек. Попробуйте позже.");
        }

        // Извлекаем текст ответа
        JsonObject resp = JsonParser.parseString(response.body()).getAsJsonObject();
        String analysisText = resp.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();

        // Парсим JSON из ответа
        int start = analysisText.indexOf('{');
        int end = analysisText.lastIndexOf('}');
        if (start == -1 || end == -1) {
            return new AnalysisResult(false, "❌ Не удалось разобрать ответ ИИ", "Попробуйте ещё раз.");
        }

        JsonObject data = JsonParser.parseString(analysisText.substring(start, end + 1)).getAsJsonObject();
        return validate(data);
    }

    private static AnalysisResult validate(JsonObject data) {
        boolean valid = true;
        StringBuilder issues = new StringBuilder();

        boolean readable = getBool(data, "is_readable");
        boolean isReal = getBool(data, "check_is_real");
        String amount = getStr(data, "amount");
        String card = getStr(data, "recipient_card");
        String purpose = getStr(data, "purpose").toLowerCase();
        String name = getStr(data, "recipient_name");
        String date = getStr(data, "date");

        if (!readable) { issues.append("• Фото нечёткое или низкого качества\n"); valid = false; }
        if (!isReal)   { issues.append("• Это не выглядит как реальный чек\n"); valid = false; }
        if (!amount.contains(EXPECTED_AMOUNT)) {
            issues.append("• Сумма должна быть 990 ₸, найдено: ").append(amount).append("\n");
            valid = false;
        }
        if (!card.contains(EXPECTED_CARD.substring(12))) { // последние 4 цифры: 6268
            issues.append("• Номер карты не совпадает (ожидается ...6268)\n");
            valid = false;
        }
        if (!purpose.contains("премиум") && !purpose.contains("подписка") && !purpose.contains("premium")) {
            issues.append("• Назначение должно содержать 'Премиум подписка'\n");
            valid = false;
        }

        String details = String.format(
            "📊 *Анализ чека:*\n• Сумма: %s\n• Карта: %s\n• Получатель: %s\n• Дата: %s\n• Назначение: %s",
            amount, card, name, date, purpose
        );

        if (!valid) details += "\n\n❌ *Проблемы:*\n" + issues;

        return new AnalysisResult(valid,
            valid ? "✅ Чек прошёл проверку!" : "❌ Чек не прошёл проверку",
            details
        );
    }

    private static String getStr(JsonObject obj, String key) {
        try { return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : ""; }
        catch (Exception e) { return ""; }
    }

    private static boolean getBool(JsonObject obj, String key) {
        try { return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean(); }
        catch (Exception e) { return false; }
    }
}
