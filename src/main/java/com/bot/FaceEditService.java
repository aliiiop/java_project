package com.bot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FaceEditService {
    private static final Path MEDIA_ROOT = Paths.get("media");
    private static final Path SOURCE_PHOTOS_DIR = MEDIA_ROOT.resolve("source-photos");
    private static final Path RENDERED_EDITS_DIR = MEDIA_ROOT.resolve("face-edits");

    private FaceEditService() {
    }

    public static void ensureMediaDirectories() {
        try {
            Files.createDirectories(SOURCE_PHOTOS_DIR);
            Files.createDirectories(RENDERED_EDITS_DIR);
        } catch (IOException e) {
            System.err.println("Не удалось создать media-директории: " + e.getMessage());
        }
    }

    public static boolean shouldCreateFaceEdit(String ratingType, double rating) {
        // В проекте пока нет полноценного рендера face edit, поэтому
        // отключаем автогенерацию, чтобы бот не падал на незавершённой функции.
        return false;
    }

    public static Path buildPhotoPath(Long chatId, String telegramFilePath) {
        String extension = getExtension(telegramFilePath);
        String safeFileName = "chat-" + chatId + extension;
        return SOURCE_PHOTOS_DIR.resolve(safeFileName);
    }

    public static RenderResult renderFaceEdit(Long chatId, String gender, double rating, Path facePhoto) {
        return RenderResult.failed("Face edit renderer is not configured");
    }

    private static String getExtension(String filePath) {
        if (filePath == null) {
            return ".jpg";
        }

        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filePath.length() - 1) {
            return ".jpg";
        }

        String extension = filePath.substring(dotIndex);
        if (extension.length() > 10) {
            return ".jpg";
        }

        return extension;
    }

    public static final class RenderResult {
        private final Path output;
        private final String message;

        private RenderResult(Path output, String message) {
            this.output = output;
            this.message = message;
        }

        public static RenderResult ready(Path output) {
            return new RenderResult(output, null);
        }

        public static RenderResult failed(String message) {
            return new RenderResult(null, message);
        }

        public boolean isReady() {
            return output != null;
        }

        public Path getOutput() {
            return output;
        }

        public String getMessage() {
            return message;
        }
    }
}
