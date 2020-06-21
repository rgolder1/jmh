package com.aztec.jmh.benchmark;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.aztec.jmh.domain.Event;
import com.aztec.jmh.repository.EventRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.runner.RunWith;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Inserts a given number of records.
 *
 * Captures benchmark results for different SQL queries made against the data set.
 */
@SpringBootTest
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.aztec.jmh.domain")
@EnableJpaRepositories("com.aztec.jmh.repository")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@RunWith(SpringRunner.class)
// Select the active profile for the required database type.
@ActiveProfiles("test-h2") // H2
//@ActiveProfiles("test-postgres") // Postgres
//@ActiveProfiles("test-mysql") // MySQL
public class SqlQueryBenchmarkTest extends BenchmarkBase {

    /*************************************************************************************************************
     *
     * ------------
     * RESULTS:
     * ------------
     *
     * ---------------
     * 100,000 records
     * ---------------
     *
     * Benchmark                                                            Mode  Cnt   Score   Error  Units
     *
     * No Index
     * ---------
     * Postgres:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  3157.594 ± 5204.767  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  6113.633 ± 2552.801  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  3051.708 ± 3292.223  ms/op
     * MySQL:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3    3424.811 ±  6530.701  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  114706.894 ± 39915.799  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3    3899.692 ± 14866.878  ms/op
     *
     * Index on timestamp
     * ------------------
     * Postgres:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  3068.102 ± 4472.961  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  3081.664 ±  665.427  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  3123.418 ± 3617.896  ms/op
     * MySQL:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  4118.322 ±  9367.398  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  4091.860 ± 16355.127  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  3569.499 ±  8474.834  ms/op
     *
     * Index on emitted
     * ----------------
     * Postgres:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  3766.831 ± 4311.084  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  6706.881 ± 6019.995  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  3667.853 ± 4711.994  ms/op
     * MySQL:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3    3712.268 ±  15330.502  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  121423.434 ± 201579.898  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3    4273.896 ±   3520.437  ms/op
     *
     * Index on timestamp and emitted
     * ------------------------------
     * Postgres:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  3622.921 ±  3696.388  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  4171.187 ± 16121.616  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  4364.324 ± 17217.475  ms/op
     * MySQL:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  6814.164 ± 12083.257  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  7055.684 ± 12303.446  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  6077.220 ± 15267.097  ms/op
     *
     * Composite index on timestamp and emitted
     * ----------------------------------------
     * Postgres:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  4608.411 ± 16583.127  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  3689.193 ±  7773.964  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  3248.135 ±  4586.935  ms/op
     * MySQL:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  6194.516 ± 14395.790  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  4947.075 ± 11177.631  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  4115.813 ±  3585.003  ms/op
     *
     * -----------------
     * 1,000,000 records
     * -----------------
     *
     * No Index
     * --------
     * Postgres:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3   3364.005 ± 4000.857  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  32403.022 ± 1074.306  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3   3211.027 ± 2874.014  ms/op
     *
     * Index on timestamp:
     * Postgres:
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3  3314.176 ± 6644.598  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  3268.049 ± 2436.178  ms/op
     * SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3  3089.936 ±  933.160  ms/op
     *
     *************************************************************************************************************/

    private static final Logger LOG = LoggerFactory.getLogger(SqlQueryBenchmarkTest.class);

    // The number of events to use for the test.
    private static final int EVENT_COUNT = 100000;

    private final static String SQL_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP = "select * from jmh.event WHERE emitted = 'false' ORDER BY timestamp ASC LIMIT 500 FOR UPDATE";
    private final static String SQL_SELECT_WHERE_EMITTED = "select * from jmh.event WHERE emitted = 'false' LIMIT 500 FOR UPDATE";
    private final static String SQL_SELECT = "select * from jmh.event LIMIT 500 FOR UPDATE";

    private static JdbcTemplate jdbcTemplate;
    private static EventRepository eventRepository;
    private static String databaseUrl;
    private ColumnMapRowMapper mapper = new ColumnMapRowMapper();

    /**
     * We can check the URL to determine the db we are using for SQL syntax specific differences.
     */
    @Value("${spring.datasource.url}")
    public void setJdbTemplate(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    @Autowired
    public void setJdbTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public void setEventRepository(final EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Set up events in the event table.  Half are marked as emitted, and half not.
     */
    @Setup
    public void setUp() {
        clearIndexes();

        // Uncomment to add index(es) for a benchmark run.
        jdbcTemplate.execute("CREATE INDEX idx_timestamp_on_event ON jmh.event (timestamp)");
//        jdbcTemplate.execute("CREATE INDEX idx_emitted_on_event ON jmh.event (emitted)");
//        jdbcTemplate.execute("CREATE INDEX idx_timestamp_and_emitted_on_event ON jmh.event (timestamp, emitted)");

        if(eventRepository.count() == EVENT_COUNT) {
            LOG.info("There are the required {} events in the database.", EVENT_COUNT);
        } else {
            LOG.info("Deleting existing events.");
            eventRepository.deleteAllInBatch();
            LOG.info("Creating {} new events.", EVENT_COUNT);
            long timestamp = System.currentTimeMillis();
            IntStream.range(0, EVENT_COUNT)
                    .parallel()
                    .forEach(i -> {
                        Event event = new Event();
                        event.setTimestamp(timestamp + 1);
                        event.setEmitted(i % 2 == 0 ? true : false);
                        event.setPayload(RandomStringUtils.randomAlphanumeric(4000));
                        event.setDestination("topic_jmh-test");
                        eventRepository.save(event);
                    });
            LOG.info("Created {} new events in the database.", EVENT_COUNT);
            long count = eventRepository.count();
            LOG.info("Total event count is {}.", count);
            assert count == EVENT_COUNT;
        }
    }

    @Benchmark
    public void pollEventsSelectWhereEmittedOrderByTimestamp(final Blackhole bh) {
        bh.consume(executeQuery(SQL_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP, 100));
    }

    @Benchmark
    public void pollEventsSelectWhereEmitted(final Blackhole bh) {
        bh.consume(executeQuery(SQL_SELECT_WHERE_EMITTED, 100));
    }

    @Benchmark
    public void pollEventsSelected(final Blackhole bh) {
        bh.consume(executeQuery(SQL_SELECT, 100));
    }

    /**
     * Execute the given SQL the given number of times.
     */
    private boolean executeQuery(final String sql, final int times) {
        IntStream.range(1, times)
                .forEach(i -> {
                    jdbcTemplate.query(sql, mapper);
                });
        return true;
    }

    /**
     * Clear indexes at the outset of a run (and electively add them as required in Setup()).
     */
    private void clearIndexes() {
        // MySQL.
        if(databaseUrl.contains("mysql")) {
            List<Map<String, Object>> results = jdbcTemplate.queryForList("SHOW INDEX FROM event");
            if(results.stream().anyMatch(r -> r.get("Key_name").equals("idx_timestamp_on_event"))) {
                jdbcTemplate.execute("ALTER TABLE event DROP INDEX idx_timestamp_on_event");
            }
            if(results.stream().anyMatch(r -> r.get("Key_name").equals("idx_emitted_on_event"))) {
                jdbcTemplate.execute("ALTER TABLE event DROP INDEX idx_emitted_on_event");
            }
            if(results.stream().anyMatch(r -> r.get("Key_name").equals("idx_timestamp_and_emitted_on_event"))) {
                jdbcTemplate.execute("ALTER TABLE event DROP INDEX idx_timestamp_and_emitted_on_event");
            }
        } else {
            // H2 and Postgres.
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_timestamp_on_event");
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_emitted_on_event");
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_timestamp_and_emitted_on_event");
        }
    }
}
