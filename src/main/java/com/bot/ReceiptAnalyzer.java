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
    public static final String EXPECTED_CARD = "440043035445268";
    public static final String EXPECTED_NAME = "KENZHALIN AMIR";
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

        String prompt = "Проанализируй фото чека/квитанции банковского перевода и ответь ТОЛЬКО в формате JSON:\n" +
            "{\"check_is_real\": boolean, \"amount\": \"строка\", \"recipient_card\": \"строка\", " +
            "\"recipient_name\": \"строка\", \"date\": \"строка\", \"is_readable\": boolean}\n\n" +
            "Поля:\n" +
            "check_is_real — это реальный чек/скриншот банковского перевода (не фейк и не просто картинка)?\n" +
            "amount — сумма и валюта (например: 990 ₸). Если не видно — пустая строка.\n" +
            "recipient_card — номер карты получателя (все цифры которые видно, включая маскированные вроде ****5268). Если не видно — пустая строка.\n" +
            "recipient_name — имя получателя ЗАГЛАВНЫМИ латинскими буквами, точно как на чеке (например: KENZHALIN AMIR). Если не видно — пустая строка.\n" +
            "date — дата транзакции. Если не видно — пустая строка.\n" +
            "is_readable — фото чёткое, не обрезанное и все ключевые поля читаемы?";

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
        boolean isReal   = getBool(data, "check_is_real");
        String amount    = getStr(data, "amount");
        String card      = getStr(data, "recipient_card").replaceAll("[^0-9*]", "");
        String name      = getStr(data, "recipient_name").toUpperCase().trim();
        String date      = getStr(data, "date");

        String lastDigits = EXPECTED_CARD.substring(EXPECTED_CARD.length() - 4); // "5268"

        if (!readable) {
            issues.append("• Фото нечёткое или обрезанное — не все поля читаемы\n");
            valid = false;
        }
        if (!isReal) {
            issues.append("• Это не похоже на реальный банковский чек\n");
            valid = false;
        }
        if (!amount.contains(EXPECTED_AMOUNT)) {
            issues.append("• Сумма должна быть 990 ₸, найдено: ").append(amount.isEmpty() ? "не определено" : amount).append("\n");
            valid = false;
        }
        if (card.isEmpty() || !card.contains(lastDigits)) {
            issues.append("• Номер карты не совпадает (ожидаются последние цифры ...").append(lastDigits).append(")\n");
            valid = false;
        }
        if (name.isEmpty() || (!name.contains("KENZHALIN") && !name.contains("AMIR"))) {
            issues.append("• Имя получателя не совпадает (ожидается: ").append(EXPECTED_NAME).append(")\n");
            valid = false;
        }

        String details = String.format(
            "📋 *Анализ квитанции:*\n• Сумма: %s\n• Карта получателя: %s\n• Имя получателя: %s\n• Дата: %s",
            amount.isEmpty() ? "—" : amount,
            card.isEmpty()   ? "—" : card,
            name.isEmpty()   ? "—" : name,
            date.isEmpty()   ? "—" : date
        );

        if (!valid) details += "\n\n❌ *Проблемы:*\n" + issues;

        return new AnalysisResult(valid,
            valid ? "✅ Оплата подтверждена!" : "❌ Квитанция не прошла проверку",
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
