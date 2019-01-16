/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark;

import com.amazonaws.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;


/**
 * Process benchmark score
 */
class BenchmarkResultProcessor {

    private final Map<String, Double> benchmarkIdToBaselineScore;
    private List<String> failedBenchmarkNames = new ArrayList<>();

    BenchmarkResultProcessor(Map<String, Double> benchmarkIdToBaselineScore) {
        this.benchmarkIdToBaselineScore = benchmarkIdToBaselineScore;
    }

    List<String> processBenchmarkResult(Collection<RunResult> results) {

        for (RunResult result : results) {
            String benchmarkId = retrieveBenchmarkId(result);
            Double baseline = benchmarkIdToBaselineScore.getOrDefault(benchmarkId, Double.MAX_VALUE);

            double calibratedScore = calibrateScore(result.getPrimaryResult());
            if (calibratedScore > baseline) {
                failedBenchmarkNames.add(benchmarkId);
            }
        }
        return failedBenchmarkNames;
    }

    /**
     * Calibrate score if needed.
     */
    private double calibrateScore(Result result) {
        if (Double.isNaN(result.getScoreError())) {
            return result.getScore();
        }

        if (result.getScoreError() > result.getScore()) {
            System.out.println("Ignoring the result since it's not accurate");
            return Double.NaN;
        }

        return result.getScore() - result.getScoreError();
    }

    /**
     *  Retrieve BenchmarkId from the runResult.
     */
    private String retrieveBenchmarkId(RunResult runResult) {
        BenchmarkParams params = runResult.getParams();
        String benchmark = params.getBenchmark();

        String[] split = benchmark.split("\\.");

        String className = split[split.length - 2];
        String benchmarkMethodName = split[split.length - 1];

        StringJoiner stringJoiner = new StringJoiner(".").add(className).add(benchmarkMethodName);

        Collection<String> paramsKeys = params.getParamsKeys();

        if (!CollectionUtils.isNullOrEmpty(paramsKeys)) {
            String paramKey = paramsKeys.iterator().next();
            String paramValue = params.getParam(paramKey);
            stringJoiner.add(paramValue);
        }
        return stringJoiner.toString();
    }
}
