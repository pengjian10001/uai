package com.uni.uai.graph.facesearch.face;

import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import com.uni.uai.graph.facesearch.model.FaceDetection;
import com.uni.uai.graph.facesearch.util.ImageUtils;
import com.uni.uai.graph.facesearch.util.ModelDownloader;
import com.uni.uai.graph.facesearch.util.VectorMath;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Haar 人脸检测 + SFace/DJL 或轻量回退特征提取。
 */
public final class OpenCvFaceService implements Closeable {

    private static final int SFACE_INPUT = 112;
    private static final int MIN_FACE_PIXELS = 80 * 80;
    private static final long MIN_SFACE_BYTES = 30_000_000L;

    private final HaarFaceDetector detector;
    private final ZooModel<NDList, NDList> embeddingModel;
    private final NDManager manager;
    private final SimpleFaceEmbedder fallbackEmbedder;
    private final boolean useFallback;
    private final int embeddingDim;

    public OpenCvFaceService(Path modelDir) throws Exception {
        this.detector = new HaarFaceDetector();
        this.fallbackEmbedder = new SimpleFaceEmbedder();
        this.manager = NDManager.newBaseManager();

        Path sface = modelDir.resolve(ModelDownloader.SFACE_FILE);
        ZooModel<NDList, NDList> loadedModel = null;
        boolean fallback = true;
        int dim = fallbackEmbedder.dimension();
        if (Files.exists(sface) && Files.size(sface) >= MIN_SFACE_BYTES) {
            try {
                Criteria<NDList, NDList> criteria = Criteria.builder()
                        .setTypes(NDList.class, NDList.class)
                        .optModelPath(sface)
                        .optEngine("OnnxRuntime")
                        .build();
                loadedModel = criteria.loadModel();
                dim = probeDimension(loadedModel);
                fallback = false;
                System.out.println("已加载 SFace ONNX 模型，特征维度=" + dim);
            } catch (Exception ex) {
                System.out.println("SFace 模型加载失败，启用轻量回退特征: " + ex.getMessage());
                if (loadedModel != null) {
                    loadedModel.close();
                    loadedModel = null;
                }
            }
        } else {
            System.out.println("SFace 模型未完整下载，启用轻量回退特征（demo 可用，生产请下载完整 ONNX）");
        }

        this.embeddingModel = loadedModel;
        this.useFallback = fallback;
        this.embeddingDim = dim;
    }

    public List<FaceDetection> detectFaces(Mat image) {
        return detector.detect(image).stream()
                .filter(face -> face.isLargeEnough(MIN_FACE_PIXELS))
                .toList();
    }

    public float[] extractEmbedding(Mat image, FaceDetection face) throws Exception {
        if (useFallback) {
            return fallbackEmbedder.embed(image, face);
        }
        Mat cropped = cropFace(image, face);
        try {
            NDArray input = toSfaceInput(cropped);
            try (Predictor<NDList, NDList> predictor = embeddingModel.newPredictor()) {
                NDList output = predictor.predict(new NDList(input));
                float[] embedding = output.singletonOrThrow().toFloatArray();
                VectorMath.l2Normalize(embedding);
                return embedding;
            } finally {
                input.close();
            }
        } finally {
            cropped.close();
        }
    }

    public float[] extractPrimaryEmbedding(Path imagePath) throws Exception {
        Mat image = ImageUtils.readMat(imagePath);
        try {
            List<FaceDetection> faces = detectFaces(image);
            if (faces.isEmpty()) {
                throw new IllegalStateException("目标图片未检测到人脸: " + imagePath);
            }
            FaceDetection best = faces.stream()
                    .max(Comparator.comparingDouble(FaceDetection::score))
                    .orElseThrow();
            return extractEmbedding(image, best);
        } finally {
            image.close();
        }
    }

    public int embeddingDimension() {
        return embeddingDim;
    }

    private int probeDimension(ZooModel<NDList, NDList> model) throws Exception {
        Mat dummy = new Mat(SFACE_INPUT, SFACE_INPUT, org.bytedeco.opencv.global.opencv_core.CV_8UC3);
        try (Predictor<NDList, NDList> predictor = model.newPredictor()) {
            NDArray input = toSfaceInput(dummy);
            try {
                NDList output = predictor.predict(new NDList(input));
                return (int) output.singletonOrThrow().getShape().dimension();
            } finally {
                input.close();
            }
        } finally {
            dummy.close();
        }
    }

    private NDArray toSfaceInput(Mat bgr) {
        Mat resized = new Mat();
        opencv_imgproc.resize(bgr, resized, new Size(SFACE_INPUT, SFACE_INPUT));
        int channels = 3;
        float[] data = new float[SFACE_INPUT * SFACE_INPUT * channels];
        int idx = 0;
        for (int c = 0; c < channels; c++) {
            for (int y = 0; y < SFACE_INPUT; y++) {
                for (int x = 0; x < SFACE_INPUT; x++) {
                    int value = resized.ptr(y, x).get(c) & 0xFF;
                    data[idx++] = (value - 127.5f) / 127.5f;
                }
            }
        }
        resized.close();
        return manager.create(data, new Shape(1, channels, SFACE_INPUT, SFACE_INPUT));
    }

    private static Mat cropFace(Mat image, FaceDetection face) {
        int x = Math.max(0, Math.round(face.x()));
        int y = Math.max(0, Math.round(face.y()));
        int w = Math.min(Math.round(face.width()), image.cols() - x);
        int h = Math.min(Math.round(face.height()), image.rows() - y);
        return new Mat(image, new org.bytedeco.opencv.opencv_core.Rect(x, y, w, h)).clone();
    }

    @Override
    public void close() {
        if (detector != null) {
            detector.close();
        }
        if (embeddingModel != null) {
            embeddingModel.close();
        }
        if (manager != null) {
            manager.close();
        }
    }
}
