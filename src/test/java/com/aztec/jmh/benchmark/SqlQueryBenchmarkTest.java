package com.aztec.jmh.benchmark;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(properties = {
        // Use this config for real Postgres:
//        "spring.datasource.url=jdbc:postgresql://localhost:5432/test?currentSchema=test",
//        "spring.datasource.driverClassName: org.postgresql.Driver",
//        "spring.datasource.username=aztec",
//        "spring.datasource.password=password",
})
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.aztec.jmh.domain")
@EnableJpaRepositories("com.aztec.jmh.repository")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class SqlQueryBenchmarkTest extends BenchmarkBase {

    /*************************************************************************************************************
     *
     * ------------
     * SETUP
     * ------------
     *
     * Sets up events in the repository and performs SQL selects.
     *
     * Configure properties in benchmark.properties, such as the number of iterations and number of concurrent threads.
     *
     * To run against Postgres:
     *
     * Configure the POSTGRES_DATABASE parameter below to switch to this DB.  If false, uses H2.
     *
     * Start the db in docker:  TODO
     * ./local-docker-compose up -d aztecdb
     *
     * Comment in the Postgres properties at the top of this class.
     *
     * SQL to run in the SQL client:  use the script src/test/resources/benchmark.sql
     *
     * ------------
     * RESULTS:
     * ------------
     *
     * H2 / 100,000 records
     *
     * Benchmark                                                        Mode  Cnt   Score   Error  Units
     *
     * 1 THREAD:
     *
     * No Index:
     * SqlQueryBenchmark.pollEventsSelectWhereEmitted                  avgt    3    167.379 ±   538.714  ms/op
     * SqlQueryBenchmark.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  12161.755 ± 11733.152  ms/op
     * SqlQueryBenchmark.pollEventsSelected                            avgt    3    130.317 ±   366.822  ms/op
     *
     * Index on timestamp:
     * SqlQueryBenchmark.pollEventsSelectWhereEmitted                  avgt    3    321.605 ± 1151.887  ms/op
     * SqlQueryBenchmark.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  15077.659 ±  863.684  ms/op
     * SqlQueryBenchmark.pollEventsSelected                            avgt    3    176.062 ±  289.724  ms/op
     *
     * Index on emitted:
     * SqlQueryBenchmark.pollEventsSelectWhereEmitted                  avgt    3    221.987 ±   713.998  ms/op
     * SqlQueryBenchmark.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  12914.534 ± 13989.875  ms/op
     * SqlQueryBenchmark.pollEventsSelected                            avgt    3    155.387 ±   115.393  ms/op
     *
     * Index on timestamp and emitted:
     * SqlQueryBenchmark.pollEventsSelectWhereEmitted                  avgt    3    329.892 ±   653.496  ms/op
     * SqlQueryBenchmark.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  15182.146 ± 24329.912  ms/op
     * SqlQueryBenchmark.pollEventsSelected                            avgt    3    216.454 ±   163.218  ms/op
     *
     * Composite index on timestamp and emitted:
     *
     *
     *************************************************************************************************************/

    private final static String SQL_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP = "select * from test.event WHERE emitted = 'false' ORDER BY timestamp ASC LIMIT 500 FOR UPDATE";
    private final static String SQL_SELECT_WHERE_EMITTED = "select * from test.event WHERE emitted = 'false' LIMIT 500 FOR UPDATE";
    private final static String SQL_SELECT = "select * from test.event LIMIT 500 FOR UPDATE";

    private static JdbcTemplate jdbcTemplate;
    private static EventRepository eventRepository;
    private ColumnMapRowMapper mapper;

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
        // Comment in to add indexes for H2.
//        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_emitted_on_event ON test.event (emitted)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_timestamp_on_event ON test.event (timestamp)");

        if(eventRepository.count()==0) {
            long timestamp = System.currentTimeMillis();
            IntStream.range(1, 100000)
                    .parallel()
                    .forEach(i -> {
                        Event event = new Event();
                        event.setTimestamp(timestamp + 1);
                        event.setEmitted(i % 2 == 0 ? true : false);
                        event.setPayload(RandomStringUtils.randomAlphanumeric(4000));
                        event.setDestination("topic_foo");
                        eventRepository.save(event);
                    });
        }
        mapper = new ColumnMapRowMapper();
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
}
