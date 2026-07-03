package com.uni.uai.graph.facesearch.util;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class ImageUtils {

    private static final Java2DFrameConverter JAVA_2D = new Java2DFrameConverter();
    private static final OpenCVFrameConverter.ToMat TO_MAT = new OpenCVFrameConverter.ToMat();

    private ImageUtils() {
    }

    public static Mat frameToMat(Frame frame) {
        Mat mat = TO_MAT.convert(frame);
        if (mat == null || mat.empty()) {
            BufferedImage image = JAVA_2D.getBufferedImage(frame, 1.0);
            Frame converted = JAVA_2D.convert(image);
            mat = TO_MAT.convert(converted);
        }
        return ensureBgr(mat.clone());
    }

    public static BufferedImage readImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("无法读取图片: " + path);
        }
        return image;
    }

    public static Mat readMat(Path path) throws IOException {
        BufferedImage image = readImage(path);
        Frame frame = JAVA_2D.convert(image);
        return ensureBgr(TO_MAT.convert(frame).clone());
    }

    private static Mat ensureBgr(Mat image) {
        if (image == null || image.empty()) {
            throw new IllegalArgumentException("无效图像矩阵");
        }
        Mat bgr = new Mat();
        if (image.channels() == 4) {
            opencv_imgproc.cvtColor(image, bgr, opencv_imgproc.COLOR_BGRA2BGR);
            image.close();
            return bgr;
        }
        if (image.channels() == 1) {
            opencv_imgproc.cvtColor(image, bgr, opencv_imgproc.COLOR_GRAY2BGR);
            image.close();
            return bgr;
        }
        if (image.channels() == 3) {
            opencv_imgproc.cvtColor(image, bgr, opencv_imgproc.COLOR_RGB2BGR);
            image.close();
            return bgr;
        }
        throw new IllegalArgumentException("不支持的通道数: " + image.channels());
    }
}
