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

public class SessionStorage {
    private static final String FILE_PATH = "user_sessions.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<Long, UserSession>>() {}.getType();

    public static synchronized Map<Long, UserSession> loadAll() {
        File f = new File(FILE_PATH);
        if (f.exists()) {
            try (Reader r = new FileReader(f)) {
                Map<Long, UserSession> data = GSON.fromJson(r, TYPE);
                return data == null ? new HashMap<>() : data;
            } catch (Exception e) {
                System.err.println("Не удалось загрузить сессии: " + e.getMessage());
            }
        }
        return migrateFromHistory();
    }

    public static synchronized void saveAll(Map<Long, UserSession> data) {
        try {
            Path tmp = Paths.get(FILE_PATH + ".tmp");
            Files.writeString(tmp, GSON.toJson(data, TYPE));
            Files.move(tmp, Paths.get(FILE_PATH), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Не удалось сохранить сессии: " + e.getMessage());
        }
    }

    private static Map<Long, UserSession> migrateFromHistory() {
        Map<Long, List<UserSession.RatingHistoryEntry>> history = HistoryStorage.loadAll();
        Map<Long, UserSession> sessions = new HashMap<>();
        for (Map.Entry<Long, List<UserSession.RatingHistoryEntry>> entry : history.entrySet()) {
            UserSession session = new UserSession();
            session.setHistory(entry.getValue());
            sessions.put(entry.getKey(), session);
        }
        return sessions;
    }
}
