package com.uni.uai.vec.example.milvus.service;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * 基于预计算距离矩阵的密度聚类（DBSCAN 实现，接口与 Milvus HDBSCAN 教程一致）。
 * 生产环境可替换为 Python hdbscan(metric='precomputed') 或 Tribuo HDBSCAN*。
 */
public class PrecomputedDensityClusterer {

    public int[] cluster(DistanceMatrix distanceMatrix,
                         int minSamples,
                         int minClusterSize) {
        double[][] matrix = distanceMatrix.getValues();
        int n = matrix.length;
        int[] labels = new int[n];
        Arrays.fill(labels, -1);

        double eps = estimateEps(matrix, minSamples);
        boolean[] visited = new boolean[n];
        int clusterId = 0;

        for (int point = 0; point < n; point++) {
            if (visited[point]) {
                continue;
            }
            visited[point] = true;
            int[] neighbors = regionQuery(matrix, point, eps);

            if (neighbors.length < minSamples) {
                labels[point] = -1;
                continue;
            }

            expandCluster(matrix, labels, visited, point, neighbors, clusterId, eps, minSamples, minClusterSize);
            clusterId++;
        }

        return relabelSmallClusters(labels, minClusterSize);
    }

    private void expandCluster(double[][] matrix, int[] labels, boolean[] visited, int point,
                               int[] neighbors, int clusterId, double eps, int minSamples, int minClusterSize) {
        labels[point] = clusterId;
        Queue<Integer> queue = new ArrayDeque<>();
        for (int neighbor : neighbors) {
            queue.add(neighbor);
        }

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (!visited[current]) {
                visited[current] = true;
                int[] currentNeighbors = regionQuery(matrix, current, eps);
                if (currentNeighbors.length >= minSamples) {
                    for (int neighbor : currentNeighbors) {
                        if (!contains(queue, neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
            if (labels[current] == -1) {
                labels[current] = clusterId;
            }
        }
    }

    private int[] regionQuery(double[][] matrix, int point, double eps) {
        int n = matrix.length;
        int[] temp = new int[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (matrix[point][i] <= eps) {
                temp[count++] = i;
            }
        }
        return Arrays.copyOf(temp, count);
    }

    /**
     * 使用 k-距离曲线启发式估计 eps，与 HDBSCAN 中 min_samples 参数作用类似。
     */
    private double estimateEps(double[][] matrix, int minSamples) {
        int n = matrix.length;
        PriorityQueue<Double> allKDistances = new PriorityQueue<>();

        for (int i = 0; i < n; i++) {
            PriorityQueue<Double> distances = new PriorityQueue<>();
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    continue;
                }
                double distance = matrix[i][j];
                if (Double.isInfinite(distance)) {
                    continue;
                }
                distances.add(distance);
            }
            for (int k = 0; k < minSamples && !distances.isEmpty(); k++) {
                allKDistances.add(distances.poll());
            }
        }

        if (allKDistances.isEmpty()) {
            return 0.35;
        }

        int pick = Math.max(0, (int) (allKDistances.size() * 0.75) - 1);
        for (int i = 0; i < pick; i++) {
            allKDistances.poll();
        }
        Double eps = allKDistances.poll();
        return eps == null ? 0.35 : Math.max(0.08, Math.min(0.45, eps));
    }

    private int[] relabelSmallClusters(int[] labels, int minClusterSize) {
        int maxCluster = -1;
        for (int label : labels) {
            maxCluster = Math.max(maxCluster, label);
        }
        if (maxCluster < 0) {
            return labels;
        }

        int[] sizes = new int[maxCluster + 1];
        for (int label : labels) {
            if (label >= 0) {
                sizes[label]++;
            }
        }

        int[] remap = new int[maxCluster + 1];
        Arrays.fill(remap, -1);
        int next = 0;
        for (int cluster = 0; cluster <= maxCluster; cluster++) {
            if (sizes[cluster] >= minClusterSize) {
                remap[cluster] = next++;
            }
        }

        int[] normalized = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            int label = labels[i];
            if (label < 0) {
                normalized[i] = -1;
            } else if (remap[label] < 0) {
                normalized[i] = -1;
            } else {
                normalized[i] = remap[label];
            }
        }
        return normalized;
    }

    private boolean contains(Queue<Integer> queue, int value) {
        return queue.contains(value);
    }
}
