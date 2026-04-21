package com.bot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FaceEditService {
    private static final Path MEDIA_ROOT = Paths.get("media");
    private static final Path TEMPLATE_DIR = MEDIA_ROOT.resolve("edit_templates");
    private static final Path PHOTO_DIR = MEDIA_ROOT.resolve("photos");
    private static final Path OUTPUT_DIR = MEDIA_ROOT.resolve("output");
    private static final String FFMPEG_PATH_PROPERTY = "bot.ffmpeg.path";
    private static final String FFMPEG_PATH_ENV = "FFMPEG_PATH";

    private static final List<String> TEMPLATE_EXTENSIONS =
        List.of(".mp4", ".mov", ".mkv", ".webm");
    private static final List<String> FFMPEG_CANDIDATES =
        List.of(
            "ffmpeg",
            "C:\\Users\\пк\\ffmpeg-8.1",
            "C:\\Users\\пк\\ffmpeg-8.1\\bin\\ffmpeg.exe",
            "C:\\Users\\пк\\ffmpeg-8.1\\ffmpeg.exe",
            "C:\\Program Files\\BlueStacks_nxt\\ffmpeg.exe"
        );
    private static final List<String> VIDEO_ENCODERS =
        List.of("libx264", "libopenh264", "mpeg4");

    private static final Pattern DIMENSION_PATTERN = Pattern.compile("(\\d{2,5})x(\\d{2,5})");
    private static final Pattern DURATION_PATTERN =
        Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2}(?:\\.\\d+)?)");
    private static final DateTimeFormatter FILE_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private static final double GREEN_SIMILARITY = 0.18;
    private static final double GREEN_BLEND = 0.08;

    public static void ensureMediaDirectories() {
        try {
            Files.createDirectories(TEMPLATE_DIR);
            Files.createDirectories(PHOTO_DIR);
            Files.createDirectories(OUTPUT_DIR);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать media-папки", e);
        }
    }

    public static boolean shouldCreateFaceEdit(String ratingType, double rating) {
        return "face".equals(ratingType) && EditVerdict.fromFaceRating(rating).requiresTemplate();
    }

    public static Path buildPhotoPath(Long chatId, String originalFilePath) {
        ensureMediaDirectories();
        String extension = getExtension(originalFilePath, ".jpg");
        return PHOTO_DIR.resolve("face_" + chatId + "_" + timestamp() + extension);
    }

    public static RenderResult renderFaceEdit(Long chatId, String gender, double rating, Path facePhoto) {
        ensureMediaDirectories();
        EditVerdict verdict = EditVerdict.fromFaceRating(rating);
        if (!verdict.requiresTemplate()) {
            return RenderResult.skipped("Нейтральный вердикт, эдит не нужен.");
        }

        Path template = findTemplate(gender, verdict);
        if (template == null) {
            return RenderResult.skipped(
                "Не найден шаблон для " + gender + "/" + verdict.getTemplateSuffix()
            );
        }

        String ffmpeg = findFfmpegExecutable();
        if (ffmpeg == null) {
            return RenderResult.skipped("ffmpeg не найден.");
        }

        try {
            VideoInfo videoInfo = readVideoInfo(ffmpeg, template);
            Path output = OUTPUT_DIR.resolve(buildOutputFileName(chatId, verdict));
            runRender(ffmpeg, facePhoto, template, output, videoInfo);
            return RenderResult.created(output);
        } catch (Exception e) {
            return RenderResult.failed("Рендер не удался: " + e.getMessage());
        }
    }

    private static Path findTemplate(String gender, EditVerdict verdict) {
        for (String baseName : getTemplateBaseNames(gender, verdict)) {
            for (String extension : TEMPLATE_EXTENSIONS) {
                Path candidate = TEMPLATE_DIR.resolve(baseName + extension);
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static List<String> getTemplateBaseNames(String gender, EditVerdict verdict) {
        ArrayList<String> names = new ArrayList<>();
        if ("male".equals(gender)) {
            names.add("male_" + verdict.getTemplateSuffix());
        } else if ("female".equals(gender)) {
            names.add("woman_" + verdict.getTemplateSuffix());
            names.add("female_" + verdict.getTemplateSuffix());
        }
        return names;
    }

    private static String findFfmpegExecutable() {
        ArrayList<String> candidates = new ArrayList<>();
        addConfiguredCandidate(candidates, System.getProperty(FFMPEG_PATH_PROPERTY));
        addConfiguredCandidate(candidates, System.getenv(FFMPEG_PATH_ENV));
        candidates.addAll(FFMPEG_CANDIDATES);

        for (String candidate : candidates) {
            String resolved = resolveExecutableCandidate(candidate);
            if (resolved != null && isRunnable(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    private static void addConfiguredCandidate(List<String> candidates, String value) {
        if (value != null && !value.isBlank()) {
            candidates.add(value.trim());
        }
    }

    private static String resolveExecutableCandidate(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        if ("ffmpeg".equals(candidate)) {
            return candidate;
        }

        Path path = Paths.get(candidate);
        if (Files.isDirectory(path)) {
            Path direct = path.resolve("ffmpeg.exe");
            if (Files.isRegularFile(direct)) {
                return direct.toString();
            }

            Path bin = path.resolve("bin").resolve("ffmpeg.exe");
            if (Files.isRegularFile(bin)) {
                return bin.toString();
            }
            return null;
        }

        return Files.isRegularFile(path) ? path.toString() : null;
    }

    private static boolean isRunnable(String command) {
        try {
            Process process = new ProcessBuilder(command, "-version")
                .redirectErrorStream(true)
                .start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static VideoInfo readVideoInfo(String ffmpeg, Path template) throws Exception {
        Process process = new ProcessBuilder(ffmpeg, "-hide_banner", "-i", template.toString())
            .redirectErrorStream(true)
            .start();

        String output;
        try (var stream = process.getInputStream()) {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        process.waitFor(10, TimeUnit.SECONDS);

        Double durationSeconds = null;
        Matcher durationMatcher = DURATION_PATTERN.matcher(output);
        if (durationMatcher.find()) {
            double hours = Double.parseDouble(durationMatcher.group(1));
            double minutes = Double.parseDouble(durationMatcher.group(2));
            double seconds = Double.parseDouble(durationMatcher.group(3));
            durationSeconds = hours * 3600 + minutes * 60 + seconds;
        }

        Matcher matcher = DIMENSION_PATTERN.matcher(output);
        while (matcher.find()) {
            int width = Integer.parseInt(matcher.group(1));
            int height = Integer.parseInt(matcher.group(2));
            if (width >= 100 && height >= 100) {
                double safeDuration = durationSeconds != null && durationSeconds > 0
                    ? durationSeconds
                    : 15.0;
                return new VideoInfo(width, height, safeDuration);
            }
        }
        throw new IllegalStateException("Не удалось определить размер шаблона: " + template);
    }

    private static void runRender(
        String ffmpeg,
        Path facePhoto,
        Path template,
        Path output,
        VideoInfo info
    ) throws Exception {
        String filter = String.format(Locale.US,
            "[0:v]scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d[face];"
                + "[1:v]chromakey=0x00FF00:%.2f:%.2f[fg];"
                + "[face][fg]overlay=0:0:format=auto,format=yuv420p[v]",
            info.width,
            info.height,
            info.width,
            info.height,
            GREEN_SIMILARITY,
            GREEN_BLEND
        );

        ArrayList<String> errors = new ArrayList<>();
        for (String encoder : VIDEO_ENCODERS) {
            Files.deleteIfExists(output);

            ArrayList<String> command = new ArrayList<>();
            command.add(ffmpeg);
            command.add("-y");
            command.add("-loop");
            command.add("1");
            command.add("-i");
            command.add(facePhoto.toString());
            command.add("-i");
            command.add(template.toString());
            command.add("-filter_complex");
            command.add(filter);
            command.add("-map");
            command.add("[v]");
            command.add("-c:v");
            command.add(encoder);
            if ("libx264".equals(encoder)) {
                command.add("-preset");
                command.add("veryfast");
            } else if ("mpeg4".equals(encoder)) {
                command.add("-q:v");
                command.add("5");
            }
            command.add("-pix_fmt");
            command.add("yuv420p");
            command.add("-an");
            command.add("-movflags");
            command.add("+faststart");
            command.add("-t");
            command.add(String.format(Locale.US, "%.2f", info.durationSeconds));
            command.add("-shortest");
            command.add(output.toString());

            Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

            String outputLog;
            try (var stream = process.getInputStream()) {
                outputLog = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }

            boolean finished = process.waitFor(2, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                errors.add(encoder + ": ffmpeg завис во время рендера");
                continue;
            }

            if (process.exitValue() == 0 && Files.isRegularFile(output)) {
                return;
            }

            String reason = outputLog.isBlank() ? "unknown ffmpeg error" : outputLog;
            errors.add(encoder + ": " + reason);
        }

        throw new IllegalStateException(String.join("\n\n", errors));
    }

    private static String buildOutputFileName(Long chatId, EditVerdict verdict) {
        return "edit_" + verdict.getTemplateSuffix() + "_" + chatId + "_" + timestamp() + ".mp4";
    }

    private static String timestamp() {
        return LocalDateTime.now().format(FILE_TIMESTAMP);
    }

    private static String getExtension(String originalFilePath, String fallback) {
        if (originalFilePath == null) {
            return fallback;
        }
        int dot = originalFilePath.lastIndexOf('.');
        if (dot == -1 || dot == originalFilePath.length() - 1) {
            return fallback;
        }
        String extension = originalFilePath.substring(dot).toLowerCase(Locale.ROOT);
        return extension.length() > 10 ? fallback : extension;
    }

    public static class RenderResult {
        private final Path output;
        private final String message;

        private RenderResult(Path output, String message) {
            this.output = output;
            this.message = message;
        }

        public static RenderResult created(Path output) {
            return new RenderResult(output, null);
        }

        public static RenderResult skipped(String message) {
            return new RenderResult(null, message);
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

    private static class VideoInfo {
        private final int width;
        private final int height;
        private final double durationSeconds;

        private VideoInfo(int width, int height, double durationSeconds) {
            this.width = width;
            this.height = height;
            this.durationSeconds = durationSeconds;
        }
    }
}
