package com.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryStorage {
    private static final String FILE_PATH = "user_history.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE =
        new TypeToken<Map<Long, List<UserSession.RatingHistoryEntry>>>() {}.getType();

    public static synchronized Map<Long, List<UserSession.RatingHistoryEntry>> loadAll() {
        File f = new File(FILE_PATH);
        if (!f.exists()) {
            return new HashMap<>();
        }

        try (Reader r = new FileReader(f)) {
            Map<Long, List<UserSession.RatingHistoryEntry>> data = GSON.fromJson(r, TYPE);
            return data == null ? new HashMap<>() : data;
        } catch (Exception e) {
            System.err.println("Не удалось загрузить историю: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public static synchronized void saveAll(Map<Long, List<UserSession.RatingHistoryEntry>> data) {
        try {
            Path tmp = Paths.get(FILE_PATH + ".tmp");
            Map<Long, List<UserSession.RatingHistoryEntry>> historyToSave = new HashMap<>(data);
            Files.writeString(tmp, GSON.toJson(historyToSave, TYPE));
            Files.move(tmp, Paths.get(FILE_PATH), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Не удалось сохранить историю: " + e.getMessage());
        }
    }
}
