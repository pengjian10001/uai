package com.uni.uai.graph.facesearch;

import com.uni.uai.graph.facesearch.util.ImageUtils;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 无 ffmpeg 依赖时，用 JavaCV 合成演示视频与目标人物图。
 */
public final class DemoDataGenerator {

    private static final String PERSON_A =
            "https://raw.githubusercontent.com/opencv/opencv/4.x/samples/data/lena.jpg";
    private static final String PERSON_B =
            "https://raw.githubusercontent.com/opencv/opencv/4.x/samples/data/baboon.jpg";

    public static void main(String[] args) throws Exception {
        Path demoDir = Path.of(args.length > 0 ? args[0] : "demo-data");
        Files.createDirectories(demoDir);

        Path personA = demoDir.resolve("person_a.jpg");
        Path personB = demoDir.resolve("person_b.jpg");
        Path target = demoDir.resolve("target.jpg");
        Path sampleVideo = demoDir.resolve("sample.mp4");

        download(PERSON_A, personA);
        download(PERSON_B, personB);
        Files.copy(personA, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        createSampleVideo(sampleVideo, personA, personB, personA, 4, 25);
        System.out.println("演示素材已生成:");
        System.out.println("  视频: " + sampleVideo.toAbsolutePath());
        System.out.println("  目标图: " + target.toAbsolutePath());
    }

    private static void download(String url, Path target) throws Exception {
        if (Files.exists(target) && Files.size(target) > 0) {
            return;
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("下载失败: " + url);
        }
        try (InputStream in = response.body()) {
            Files.copy(in, target);
        }
    }

    private static void createSampleVideo(
            Path output,
            Path segment1,
            Path segment2,
            Path segment3,
            int secondsPerSegment,
            int fps) throws Exception {
        BufferedImage img1 = ImageUtils.readImage(segment1);
        BufferedImage img2 = ImageUtils.readImage(segment2);
        BufferedImage img3 = ImageUtils.readImage(segment3);

        int width = 640;
        int height = 480;
        Java2DFrameConverter converter = new Java2DFrameConverter();

        try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output.toString(), width, height)) {
            recorder.setFormat("mp4");
            recorder.setFrameRate(fps);
            recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
            recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);
            recorder.start();

            writeSegment(recorder, converter, scale(img1, width, height), secondsPerSegment, fps);
            writeSegment(recorder, converter, scale(img2, width, height), secondsPerSegment, fps);
            writeSegment(recorder, converter, scale(img3, width, height), secondsPerSegment, fps);
            recorder.stop();
        }
    }

    private static void writeSegment(
            FFmpegFrameRecorder recorder,
            Java2DFrameConverter converter,
            BufferedImage image,
            int seconds,
            int fps) throws Exception {
        Frame frame = converter.convert(image);
        int totalFrames = seconds * fps;
        for (int i = 0; i < totalFrames; i++) {
            recorder.record(frame);
        }
    }

    private static BufferedImage scale(BufferedImage src, int width, int height) {
        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        var g = dst.createGraphics();
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return dst;
    }
}
