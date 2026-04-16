package com.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class HistoryStorage {
    private static final String FILE_PATH = "user_history.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE =
        new TypeToken<Map<Long, List<UserSession.RatingHistoryEntry>>>(){}.getType();

    public static synchronized Map<Long, List<UserSession.RatingHistoryEntry>> loadAll() {
        File f = new File(FILE_PATH);
        if (!f.exists()) return new HashMap<>();
        try (Reader r = new FileReader(f)) {
            Map<Long, List<UserSession.RatingHistoryEntry>> data = GSON.fromJson(r, TYPE);
            return normalizeHistory(data);
        } catch (Exception e) {
            System.err.println("Не удалось загрузить историю: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static synchronized void saveAll(Map<Long, List<UserSession.RatingHistoryEntry>> data) {
        try {
            Path tmp = Paths.get(FILE_PATH + ".tmp");
            Files.writeString(tmp, GSON.toJson(data, TYPE));
            Files.move(tmp, Paths.get(FILE_PATH), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Не удалось сохранить историю: " + e.getMessage());
        }
    }
    private static Map<Long, List<UserSession.RatingHistoryEntry>> normalizeHistory(
        Map<Long, List<UserSession.RatingHistoryEntry>> data
    ) {
        HashMap<Long, List<UserSession.RatingHistoryEntry>> normalized = new HashMap<>();
        if (data == null) {
            return normalized;
        }

        for (Map.Entry<Long, List<UserSession.RatingHistoryEntry>> entry : data.entrySet()) {
            ArrayList<UserSession.RatingHistoryEntry> historyEntries = new ArrayList<>();
            if (entry.getValue() != null) {
                historyEntries.addAll(entry.getValue());
            }
            normalized.put(entry.getKey(), historyEntries);
        }
        return normalized;
    }
}
