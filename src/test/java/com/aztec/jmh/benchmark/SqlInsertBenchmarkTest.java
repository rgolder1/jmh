package com.aztec.jmh.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.openjdk.jmh.annotations.Level.Iteration;

/**
 * Captures benchmark results for SQL inserts.
 *
 * View results captured in the project root here: jmh-insert-results.txt
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
public class SqlInsertBenchmarkTest extends BenchmarkBase {

    private static final Logger LOG = LoggerFactory.getLogger(SqlInsertBenchmarkTest.class);

    // The number of events to use for the test.
    private static final int EVENT_COUNT = 20000;

    private static JdbcTemplate jdbcTemplate;
    private static EventRepository eventRepository;
    private static String databaseUrl;
    private List<Event> events;

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
     * Set up a list of events ready to insert in the event table.
     *
     * Clears down the table and creates a new list for each iteration to ensure the previously inserted events are not
     * in fact being updated rather than newly inserted.
     *
     * Uncomment any index(es) required for the test run.
     *
     * The Iteration level of the @Setup annotation means this method is run before each iteration.
     */
    @Setup(value = Iteration)
    public void setUp() {
        LOG.info("Deleting existing events.");
        eventRepository.deleteAllInBatch();
        events = new ArrayList<>(EVENT_COUNT);

        SqlUtils.dropIndexes(jdbcTemplate, databaseUrl, "idx_timestamp_on_event", "idx_emitted_on_event", "idx_timestamp_and_emitted_on_event");

        // Uncomment to add index(es) for a benchmark run.
//        jdbcTemplate.execute("CREATE INDEX idx_timestamp_on_event ON jmh.event (timestamp)");
//        jdbcTemplate.execute("CREATE INDEX idx_emitted_on_event ON jmh.event (emitted)");
//        jdbcTemplate.execute("CREATE INDEX idx_timestamp_and_emitted_on_event ON jmh.event (timestamp, emitted)");

        LOG.info("Creating list of {} events to insert.", EVENT_COUNT);
        long timestamp = System.currentTimeMillis();
        Random r = new Random();
        IntStream.range(0, EVENT_COUNT)
                .forEach(i -> {
                    Event event = new Event();
                    event.setTimestamp(timestamp + r.nextInt(EVENT_COUNT));
                    event.setEmitted(i % 2 == 0 ? true : false);
                    event.setPayload(RandomStringUtils.randomAlphanumeric(4000));
                    event.setDestination("topic_jmh-test");
                    events.add(event);
                });
        LOG.info("Created list of {} events ready to insert.", events.size());
        assert events.size() == EVENT_COUNT;
    }

    @Benchmark
    public void insertEvents(final Blackhole bh) {
        bh.consume(executeInsert(events));
    }

    /**
     * Execute the given SQL the given number of times.
     */
    private boolean executeInsert(final List<Event> events) {
        eventRepository.saveAll(events);
        return true;
    }
}
