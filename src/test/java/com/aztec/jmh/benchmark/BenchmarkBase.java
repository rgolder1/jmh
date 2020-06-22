package com.aztec.jmh.benchmark;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.junit.Test;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.springframework.core.io.support.PropertiesLoaderUtils;

abstract class BenchmarkBase {

    /**
     * Any benchmark, by extending this class, inherits this single @Test method for JUnit to run.
     */
    @Test
    public void executeJmhRunner() throws RunnerException, IOException {

        Properties properties = PropertiesLoaderUtils.loadAllProperties("benchmark.properties");

        int warmup = Integer.parseInt(properties.getProperty("benchmark.warmup.iterations", "5"));
        int iterations = Integer.parseInt(properties.getProperty("benchmark.test.iterations", "5"));
        int threads = Integer.parseInt(properties.getProperty("benchmark.test.threads", "1"));
        String resultFilePrefix = properties.getProperty("benchmark.global.resultfileprefix", "jmh-");

        ResultFormatType resultsFileOutputType = ResultFormatType.JSON;

        Options opt = new OptionsBuilder()
                .include("\\." + this.getClass().getSimpleName() + "\\.")
                .warmupIterations(warmup)
                .measurementIterations(iterations)
                // single shot for each iteration:
                .warmupTime(TimeValue.NONE)
                .measurementTime(TimeValue.NONE)
                // do not use forking or the benchmark methods will not see references stored within its class
                .forks(0)
                .threads(threads)
                .shouldDoGC(true)
                .shouldFailOnError(true)
                .resultFormat(resultsFileOutputType)
                .result(buildResultsFileName(resultFilePrefix, resultsFileOutputType))
                .shouldFailOnError(true)
                .jvmArgs("-server")
                .build();

        new Runner(opt).run();
    }

    private static String buildResultsFileName(String resultFilePrefix, ResultFormatType resultType) {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("mm-dd-yyyy-hh-mm-ss");

        String suffix;
        switch (resultType) {
            case CSV:
                suffix = ".csv";
                break;
            case SCSV:
                // Semi-colon separated values
                suffix = ".scsv";
                break;
            case LATEX:
                suffix = ".tex";
                break;
            case JSON:
            default:
                suffix = ".json";
                break;

        }

        return String.format("target/%s%s%s", resultFilePrefix, date.format(formatter), suffix);
    }

}
