package com.uni.uai.graph.facesearch.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 首次运行时自动下载 OpenCV Zoo 的 YuNet / SFace ONNX 模型。
 */
public final class ModelDownloader {

    public static final String YUNET_FILE = "face_detection_yunet_2023mar.onnx";
    public static final String SFACE_FILE = "face_recognition_sface_2021dec.onnx";

    private static final String YUNET_URL =
            "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx";
    private static final String SFACE_URL =
            "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/face_recognition_sface_2021dec.onnx";

    private ModelDownloader() {
    }

    public static Path ensureModels(Path modelDir) throws IOException, InterruptedException {
        Files.createDirectories(modelDir);
        downloadIfMissing(modelDir.resolve(YUNET_FILE), YUNET_URL);
        downloadIfMissing(modelDir.resolve(SFACE_FILE), SFACE_URL);
        return modelDir;
    }

    private static void downloadIfMissing(Path target, String url) throws IOException, InterruptedException {
        if (Files.exists(target) && Files.size(target) > 0) {
            System.out.println("模型已存在: " + target);
            return;
        }
        System.out.println("正在下载模型: " + target.getFileName());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("下载失败 " + url + " status=" + response.statusCode());
        }
        try (InputStream in = response.body()) {
            Files.copy(in, target);
        }
        System.out.println("下载完成: " + target);
    }
}
