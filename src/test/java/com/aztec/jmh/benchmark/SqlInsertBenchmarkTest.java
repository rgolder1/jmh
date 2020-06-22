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
    private static final int EVENT_COUNT = 50000;

    private static JdbcTemplate jdbcTemplate;
    private static EventRepository eventRepository;
    private static String databaseUrl;
    private List<Event> events = new ArrayList<>(EVENT_COUNT);

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
     * Uncomment any index(es) required for the test run.
     */
    @Setup
    public void setUp() {
        clearIndexes();

        LOG.info("Deleting existing events.");
        eventRepository.deleteAllInBatch();

        // Uncomment to add index(es) for a benchmark run.
//        jdbcTemplate.execute("CREATE INDEX idx_timestamp_on_event ON jmh.event (timestamp)");
//        jdbcTemplate.execute("CREATE INDEX idx_emitted_on_event ON jmh.event (emitted)");
//        jdbcTemplate.execute("CREATE INDEX idx_timestamp_and_emitted_on_event ON jmh.event (timestamp, emitted)");

        if(events.size() == EVENT_COUNT) {
            LOG.info("There are the required {} events ready to insert.", EVENT_COUNT);
            // Null out the IDs so that the events are written as new.
            events.stream().parallel().forEach(event -> { event.setId(null); });
            LOG.info("todo");
        } else {
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
    }

    @Benchmark
    public void insertEvents(final Blackhole bh) {
        // Null out the IDs so that the events are written as new.
        events.stream().parallel().forEach(event -> { event.setId(null); });
        bh.consume(executeInsert(events));
    }

    /**
     * Execute the given SQL the given number of times.
     */
    private boolean executeInsert(final List<Event> events) {
        eventRepository.saveAll(events);
        return true;
    }

    /**
     * Clear indexes at the outset of a run (and electively add them as required in Setup()).
     */
    private void clearIndexes() {
        if(databaseUrl.contains("mysql")) {
            // MySQL.
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
        } else if(databaseUrl.contains("sqlserver")) {
            // SQL Server.
            jdbcTemplate.execute("DROP INDEX IF EXISTS jmh.event.idx_timestamp_on_event");
            jdbcTemplate.execute("DROP INDEX IF EXISTS jmh.event.idx_emitted_on_event");
            jdbcTemplate.execute("DROP INDEX IF EXISTS jmh.event.idx_timestamp_and_emitted_on_event");
        } else {
            // H2 and Postgres.
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_timestamp_on_event");
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_emitted_on_event");
            jdbcTemplate.execute("DROP INDEX IF EXISTS idx_timestamp_and_emitted_on_event");
        }
    }
}
