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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Inserts a given number of records.
 *
 * Captures benchmark results for different SQL queries made against the data set.
 *
 * View results captured in the project root here: jmh-query-results.txt
 */
@SpringBootTest(classes = TestConfiguration.class)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@RunWith(SpringRunner.class)
// Select one of the following active profiles for the required database type:
@ActiveProfiles("test-h2") // H2
//@ActiveProfiles("test-postgres") // Postgres
//@ActiveProfiles("test-mysql") // MySQL
//@ActiveProfiles("test-sqlserver") // SQLServer
public class SqlQueryBenchmarkTest extends BenchmarkBase {

    private static final Logger LOG = LoggerFactory.getLogger(SqlQueryBenchmarkTest.class);

    // The number of events to use for the test.
    private static final int EVENT_COUNT = 100000;

    // The number of times to run the SQL query.
    private static final int SELECT_COUNT = 100;

    // Queries for H2, Postgres and MySQL.
    private final static String DEFAULT_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP = "select * from jmh.event WHERE emitted = 'false' ORDER BY timestamp ASC LIMIT 500";
    private final static String DEFAULT_SELECT_WHERE_EMITTED = "select * from jmh.event WHERE emitted = 'false' LIMIT 500";
    private final static String DEFAULT_SELECT = "select * from jmh.event LIMIT 500";

    // Equivalent queries for SQLServer.
    private final static String SQLSERVER_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP = "select TOP 500 * from jmh.event WHERE emitted = 'false' ORDER BY timestamp ASC";
    private final static String SQLSERVER_SELECT_WHERE_EMITTED = "select TOP 500 * from jmh.event WHERE emitted = 'false'";
    private final static String SQLSERVER_SELECT = "select TOP 500 * from jmh.event";

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
     *
     * Uncomment any index(es) required for the test run.
     *
     * The default Trial level of the @Setup annotation means this method is run only once before all the iterations.
     */
    @Setup
    public void setUp() {
        SqlUtils.dropIndexes(jdbcTemplate, databaseUrl, "idx_timestamp_on_event", "idx_emitted_on_event", "idx_timestamp_and_emitted_on_event");

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
        final String query = databaseUrl.contains("sqlserver")?SQLSERVER_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP:DEFAULT_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP;
        bh.consume(executeQuery(query, SELECT_COUNT));
    }

    @Benchmark
    public void pollEventsSelectWhereEmitted(final Blackhole bh) {
        final String query = databaseUrl.contains("sqlserver")?SQLSERVER_SELECT_WHERE_EMITTED:DEFAULT_SELECT_WHERE_EMITTED;
        bh.consume(executeQuery(query, SELECT_COUNT));
    }

    @Benchmark
    public void pollEventsSelected(final Blackhole bh) {
        final String query = databaseUrl.contains("sqlserver")?SQLSERVER_SELECT:DEFAULT_SELECT;
        bh.consume(executeQuery(query, SELECT_COUNT));
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
