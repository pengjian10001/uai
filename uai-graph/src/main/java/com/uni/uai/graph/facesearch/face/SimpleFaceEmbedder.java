package com.uni.uai.graph.facesearch.face;

import com.uni.uai.graph.facesearch.model.FaceDetection;
import com.uni.uai.graph.facesearch.util.VectorMath;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

/**
 * 不依赖 ONNX 的轻量特征提取，用于模型未完整下载时的本地演示回退。
 */
final class SimpleFaceEmbedder {

    private static final int INPUT = 32;
    private static final int DIM = 128;

    float[] embed(Mat image, FaceDetection face) {
        Mat cropped = cropFace(image, face);
        Mat gray = new Mat();
        Mat resized = new Mat();
        try {
            if (cropped.channels() == 3) {
                opencv_imgproc.cvtColor(cropped, gray, opencv_imgproc.COLOR_BGR2GRAY);
            } else {
                gray = cropped.clone();
            }
            opencv_imgproc.resize(gray, resized, new Size(INPUT, INPUT));
            float[] raw = new float[INPUT * INPUT];
            int idx = 0;
            for (int y = 0; y < INPUT; y++) {
                for (int x = 0; x < INPUT; x++) {
                    raw[idx++] = resized.ptr(y, x).get() & 0xFF;
                }
            }
            float[] embedding = poolToDim(raw, DIM);
            VectorMath.l2Normalize(embedding);
            return embedding;
        } finally {
            cropped.close();
            gray.close();
            resized.close();
        }
    }

    int dimension() {
        return DIM;
    }

    private static float[] poolToDim(float[] raw, int dim) {
        float[] out = new float[dim];
        int bucket = Math.max(1, raw.length / dim);
        for (int i = 0; i < dim; i++) {
            int start = i * bucket;
            int end = Math.min(raw.length, start + bucket);
            float sum = 0;
            for (int j = start; j < end; j++) {
                sum += raw[j];
            }
            out[i] = sum / (end - start);
        }
        return out;
    }

    private static Mat cropFace(Mat image, FaceDetection face) {
        int x = Math.max(0, Math.round(face.x()));
        int y = Math.max(0, Math.round(face.y()));
        int w = Math.min(Math.round(face.width()), image.cols() - x);
        int h = Math.min(Math.round(face.height()), image.rows() - y);
        return new Mat(image, new org.bytedeco.opencv.opencv_core.Rect(x, y, w, h)).clone();
    }
}
