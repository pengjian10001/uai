package com.uni.uai.vec.example.milvus.util;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static float[] normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return vector;
        }
        float norm = (float) Math.sqrt(sum);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }

    public static double cosineDistance(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 1.0;
        }
        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
