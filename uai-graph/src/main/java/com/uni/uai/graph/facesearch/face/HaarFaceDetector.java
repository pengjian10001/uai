package com.uni.uai.graph.facesearch.face;

import com.uni.uai.graph.facesearch.model.FaceDetection;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenCV 内置 Haar Cascade 检测器，不依赖外部 ONNX，保证本地 demo 可运行。
 */
final class HaarFaceDetector implements AutoCloseable {

    private final CascadeClassifier classifier;

    HaarFaceDetector() throws IOException {
        Path cascade = resolveCascade();
        classifier = new CascadeClassifier(cascade.toString());
        if (classifier.empty()) {
            throw new IOException("加载 Haar Cascade 失败: " + cascade);
        }
    }

    List<FaceDetection> detect(Mat bgrImage) {
        Mat gray = new Mat();
        RectVector faces = new RectVector();
        try {
            opencv_imgproc.cvtColor(bgrImage, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.equalizeHist(gray, gray);
            classifier.detectMultiScale(
                    gray,
                    faces,
                    1.1,
                    3,
                    0,
                    new org.bytedeco.opencv.opencv_core.Size(80, 80),
                    new org.bytedeco.opencv.opencv_core.Size());

            List<FaceDetection> result = new ArrayList<>();
            for (long i = 0; i < faces.size(); i++) {
                Rect rect = faces.get(i);
                result.add(new FaceDetection(rect.x(), rect.y(), rect.width(), rect.height(), 1.0f));
            }
            return result;
        } finally {
            gray.close();
            faces.close();
        }
    }

    private static Path resolveCascade() throws IOException {
        Path target = Path.of("models", "haarcascade_frontalface_default.xml");
        if (Files.exists(target) && Files.size(target) > 1000) {
            return target;
        }
        Files.createDirectories(target.getParent());
        String url = "https://raw.githubusercontent.com/opencv/opencv/4.x/data/haarcascades/haarcascade_frontalface_default.xml";
        try {
            java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build()
                    .send(
                            java.net.http.HttpRequest.newBuilder()
                                    .uri(java.net.URI.create(url))
                                    .GET()
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofFile(target))
                    .body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("下载 Haar Cascade 被中断", e);
        }
        return target;
    }

    @Override
    public void close() {
        if (classifier != null) {
            classifier.close();
        }
    }
}
