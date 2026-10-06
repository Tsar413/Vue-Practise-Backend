package com.study.vuePractiseBackend.util;

import com.study.vuePractiseBackend.config.RepairFileProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
public class RepairFileUtil {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000L;
    private final RepairFileProperties properties;
    private Path root;

    public RepairFileUtil(RepairFileProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        try {
            root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
            Files.createDirectories(root);
            root = root.toRealPath();
            log.info("维修图片存储目录：{}", root);
            if (!ImageIO.getImageReadersByFormatName("webp").hasNext()) {
                throw new IllegalStateException("缺少WebP ImageIO插件，请添加imageio-webp依赖");
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化维修图片目录", e);
        }
    }

    public record SavedFile(String imageUrl, String originalName,
                            String contentType, long fileSize) {
    }

    public SavedFile save(Long workspaceId, MultipartFile file) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("工作空间ID必须为正整数");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "单张图片不能超过5MB");
        }

        byte[] bytes;
        try (InputStream input = file.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException e) {
            throw new IllegalStateException("读取上传图片失败", e);
        }
        if (bytes.length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "单张图片不能超过5MB");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("图片文件不能为空");
        }

        String format = validateImage(bytes);
        String extension = "jpeg".equals(format) ? "jpg" : format;
        String contentType = "image/" + format;
        String relative = workspaceId + "/"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            // 创建父目录后再次检查，避免符号链接绕过目录限制。
            target = resolve(relative);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return new SavedFile(relative, safeName(file.getOriginalFilename(), extension),
                    contentType, bytes.length);
        } catch (IOException e) {
            deleteQuietly(relative);
            throw new IllegalStateException("保存维修图片失败", e);
        }
    }

    private String validateImage(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("无法识别图片");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("仅支持JPEG、PNG、WebP图片");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if ("jpg".equals(format)) format = "jpeg";
                if (!"jpeg".equals(format) && !"png".equals(format) && !"webp".equals(format)) {
                    throw new IllegalArgumentException("仅支持JPEG、PNG、WebP图片");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException("图片像素总数不能超过1600万，请压缩后上传");
                }
                // 实际解码，不能只根据扩展名或Content-Type判断。
                if (reader.read(0) == null) {
                    throw new IllegalArgumentException("图片内容损坏");
                }
                return format;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("图片内容损坏或格式不受支持", e);
        }
    }

    private String safeName(String original, String extension) {
        if (original == null || original.isBlank()) return "image." + extension;
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) return "image." + extension;
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private Path resolve(String relative) {
        if (relative == null || relative.isBlank()) {
            throw new IllegalStateException("图片文件地址为空");
        }
        Path value = Path.of(relative);
        Path resolved = root.resolve(value).normalize();
        if (value.isAbsolute() || !resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalStateException("图片文件地址越界");
        }
        Path current = root;
        for (Path part : root.relativize(resolved)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalStateException("图片文件地址包含符号链接");
            }
        }
        return resolved;
    }

    public byte[] read(String relative) {
        Path path = resolve(relative);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片文件不存在");
        }
        try (InputStream input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IllegalStateException("存储的图片超过大小限制");
            return bytes;
        } catch (NoSuchFileException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片文件不存在");
        } catch (IOException e) {
            throw new IllegalStateException("读取图片文件失败", e);
        }
    }

    public void delete(String relative) {
        try {
            Files.deleteIfExists(resolve(relative));
        } catch (IOException e) {
            throw new IllegalStateException("清理图片文件失败", e);
        }
    }

    public void deleteQuietly(String relative) {
        try {
            delete(relative);
        } catch (RuntimeException e) {
            log.error("回滚时清理图片失败：{}", relative, e);
        }
    }
}
