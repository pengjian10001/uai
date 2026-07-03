package com.uni.uai.graph.facesearch.pipeline;

import com.uni.uai.graph.facesearch.model.MatchHit;
import com.uni.uai.graph.facesearch.model.TimeSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 将离散匹配时间戳合并为连续出现片段。
 */
public final class TimeSegmentMerger {

    private final double gapSec;

    public TimeSegmentMerger(double gapSec) {
        this.gapSec = gapSec;
    }

    public List<TimeSegment> merge(List<MatchHit> hits) {
        Map<String, List<MatchHit>> grouped = hits.stream()
                .collect(Collectors.groupingBy(MatchHit::videoId));

        List<TimeSegment> segments = new ArrayList<>();
        for (Map.Entry<String, List<MatchHit>> entry : grouped.entrySet()) {
            List<MatchHit> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparingDouble(MatchHit::timeSec))
                    .toList();
            if (sorted.isEmpty()) {
                continue;
            }

            double start = sorted.get(0).timeSec();
            double end = sorted.get(0).timeSec();
            double maxScore = sorted.get(0).score();

            for (int i = 1; i < sorted.size(); i++) {
                MatchHit hit = sorted.get(i);
                if (hit.timeSec() - end <= gapSec) {
                    end = hit.timeSec();
                    maxScore = Math.max(maxScore, hit.score());
                } else {
                    segments.add(new TimeSegment(entry.getKey(), start, end, maxScore));
                    start = hit.timeSec();
                    end = hit.timeSec();
                    maxScore = hit.score();
                }
            }
            segments.add(new TimeSegment(entry.getKey(), start, end, maxScore));
        }

        segments.sort(Comparator.comparing(TimeSegment::videoId).thenComparingDouble(TimeSegment::startSec));
        return segments;
    }
}
