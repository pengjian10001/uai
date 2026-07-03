package com.uni.uai.graph.facesearch.model;

/**
 * 人物在视频中的连续出现时间段。
 */
public record TimeSegment(String videoId, double startSec, double endSec, double maxScore) {

    public String formatRange() {
        return formatTime(startSec) + " ~ " + formatTime(endSec);
    }

    private static String formatTime(double sec) {
        int total = (int) Math.round(sec);
        int m = total / 60;
        int s = total % 60;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (score=%.3f)", videoId, formatRange(), maxScore);
    }
}
