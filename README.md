# Microbenchmarking with JMH

## Overview

The JMH Microbenchmarking library provides a harness with which to run benchmark performance tests against your code.  The library is well documented with many good examples, so here I look at with a few specific goals in mind.

First it is useful to be able to run benchmarks from within the context of a Spring Boot test.  This enables testing particular parts of the application as they run deployed in Production.  We get the full benefits of Spring's autowiring to pull in components just as the application does, such as a fully configured datasource, and no need to write all that boilerplate code ourselves.

The second goal is to create a project that uses JMH to enable me to easily test the performance of SQL queries, in order to see the effect of small adjustments to the queries such as adding altering the WHERE or ORDER BY clauses.  And importantly, combining these adjustments with adding and removing indexes to the database.  While the impact of changes is usually obvious, being able to understand the precise cost can be very informative.  Adding an index to improve the performance of a read comes at the cost of the write to the same table, so it is often a trade off.  Then there are subtler differences that can be examined, such as the impact of adding an index on two columns compared to adding a single composite key index on the columns.

The final goal was to create a project that enables comparison of SQL running against different database types.  By default the project runs against the H2 database.  H2 is an open source lightweight in-memory database that can be embedded in Java applications, and therefore makes it the first choice for testing against as no extra infrastructure is required.  However the value comes when benchmarking performance against external running databases.  In this project I am pulling Postgres, MySQL and SQLServer database docker images, spinning these up in Docker, and hitting directly from the test.  The project is extensible making it easy to swap in further database types if desired.

In summary this demonstration project provides a framework for capturing benchmarks to compare the effect on performance of:

- different database indexes
- different database types
- different SELECT statement clauses

## A Tour Of The Project

The benchmark test classes themselves are annotated with the usual @SpringBootTest / @RunWith(SpringRunner.class) annotations, and each pulls in the TestConfiguration which includes the test annotations such as @EnableJpaRepositories that are used to wire up our Spring components.  To that end there is a JpaRepository class, and a domain object, defined in src/main/java.  In these tests we are simulating storing and querying events in the database, and observing the performance of the queries and inserts.

The @ActiveProfiles annotation is used to swap in the required database configuration properties.  For example, setting to @ActiveProfiles("test-postgres") will load the properties from src/test/resources/application-test-postgres.yml

We also have a number of JMH specific class annotations such as @BenchmarkMode / @State.  Their usage is well documented in the JMH documentation so I will not cover that here.

The tests extend the BenchmarkBase abstract class which is responsible for executing the JMH runner, which captures the results on the performance run.  This class enables properties to be overridden from the src/test/resources/benchmark.properties file, such as the number of warmup iterations, the number of actual test iterations, and the number of concurrent threads to use.  This is the standard JMH configuration which again is well documented in the JMH documentation.

By default the tests run against H2, but more interesting is to run against external dockerised databases.  To this end there are scripts provided to build and start Postgres, MySQL and SQLServer.  With Docker running, in the root of the project run for example dockerBuildAndStartPostgres.sh.  This pulls a base Postgres docker image, builds, and starts, inserting an initialistion SQL script that runs when the database starts.  This script creates the schema and any tables that are required for the test.  The Dockerfile and SQL scripts for Postgres live under /resources/postgres/.  Likewise for MySQL, run ./dockerBuildAndStartMySql.sh, and for SQLServer run ./dockerBuildAndStartSqlServer.sh.  

Then configure the active profile for the respective database in the annotation at the top of the test class.

At the end of the test the ./dockerStopPostgres.sh script can be used to stop and optionally remove the Postgres docker container.  There are similar scripts for MySQL and SQLServer.

To view the SQL being run by the test configure the following in the respective application properties file (noting that this will slow the test run down):

    show_sql: true

## A Note On Autowiring

In order for the JMH framework to access the Spring classes they must be defined as static.  This means that when Spring autowires them for us, a static instance of the class must be instantiated.  Hence we define an @Autowired setter methods that do this:

	private static EventRepository eventRepository;

    @Autowired
    public void setEventRepository(final EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    } 

## Running The Tests

The easiest way to run the tests is to run  via the IDE.  Right click on the test class and select Run test.  The test can also be debugged this way if required.  Alternatively right click on the executeJmhRunner() @Test method in BenchmarkBase, and select the actual subclass test to run.

The tests can also be run via `mvn clean test`

## Viewing The Results

Scroll to the bottom of the test run log output to view the benchmark results, and observe output similar to the following:

    Benchmark                                                           Mode  Cnt      Score     Error  Units
    SqlQueryBenchmarkTest.pollEventsSelectWhereEmitted                  avgt    3    101.367 ±  16.779  ms/op
    SqlQueryBenchmarkTest.pollEventsSelectWhereEmittedOrderByTimestamp  avgt    3  12059.538 ± 142.716  ms/op
    SqlQueryBenchmarkTest.pollEventsSelected                            avgt    3     93.879 ±   8.189  ms/op

Alternatively view the json file generated in the target dir.  For example the output reports:

    Benchmark result is saved to target/jmh-benchmark-result-24-22-2020-10-24-07.json

This file can be published by a Jenkins build job, and so could be used to track whether any key areas being benchmarked have improved or degraded in performance following code changes.

## The Tests

As described above, choose the @ActiveProfile associated with the required database, and ensure the requisite docker container is running (unless using H2).

The first thing each test does in the setup method is to clear any indexes from the database, as we will decide for the benchmark run which index(es) to use.  Different syntax is required to achieve this for different database types (e.g. 'IF NOT EXISTS' is not valid for MySQL indexes), and this is taken care of in the clearIndexes() method.  Note that this is a JMH @Setup annotation, not JUnit.

We decide which index if any to use by uncommenting the appropriates one(s), such as adding an index on timestamp.

### SqlQueryBenchmarkTest
 
The test inserts 100,000 events in the database.  Change the constant at the top of the class to decide if fewer or more events are required.

    private static final int EVENT_COUNT = 100000;

If there are already the required number of events in the database it will skip this insert step.

Now the three methods annotated @Benchmark will execute in turn.  These perform each of the different SQL selects and capture the benchmark results against each.  For this test there are three variations on the SQL query to compare.  A SELECT all, a SELECT with a WHERE clause, and a SELECT with WHERE and ORDER BY clauses against different columns:

	private final static String DEFAULT_SELECT_WHERE_EMITTED_ORDER_BY_TIMESTAMP = "select * from jmh.event WHERE emitted = 'false' ORDER BY timestamp ASC LIMIT 500";
    private final static String DEFAULT_SELECT_WHERE_EMITTED = "select * from jmh.event WHERE emitted = 'false' LIMIT 500";
    private final static String DEFAULT_SELECT = "select * from jmh.event LIMIT 500";

For SQLServer the SQL syntax is slightly different, using 'TOP 500' rather than 'LIMIT 500' to limit the number of rows queried.  The test checks the database connection URL at runtime to swap in the associated query.

Each select is run a configurable number of times, based on the constant defined at the top of the class.

    private static final int SELECT_COUNT = 100;

If we want to benchmark with a different index, uncomment as required, and run the test again.

The benchmark results captured running the test against different databases with different indexed in place can be viewed in the file [jmh-query-results.txt](jmh-query-results.txt) in the root of the project.

### SqlInsertBenchmarkTest

This test first creates a list of events based on the configured event count:

    private static final int EVENT_COUNT = 100000;

These events are then inserted into the database using the JPA repository. 

    eventRepository.saveAll(events);
    
This list is reused for each benchmark iteration.  As the insert operation results in each event Id being set, the Ids are nulled so that new records are inserted, rather than existing records being updated. 

The benchmark results captured running the test against different databases with different indexed in place can be viewed in the file [jmh-insert-results.txt](jmh-insert-results.txt) in the root of the project.

## Benchmarking Other Databases

As mentioned the project can be extended to benchmark performance against other databases.  These are the steps to add a new database:

1. Create a Dockerfile and start up scripts under the project root resources/ dir for the database.
2. Create a script in the project root dir to pull and start a suitable docker container (similar to dockerBuildAndStartPostgres.sql).
3. Create an application test properties file with the required connection properties (similar to application-test-postgres.yml).
4. Set the active profile in the SpringBoot test class to pull in this properties file (similar to @ActiveProfiles("test-postgres")).
5. Ensure the syntax for the create/drop indexes is correct in the test class.

Of course the standard SQL syntax being run may differ for different database types too, as it does for SQLServer.

## Viewing The Source

The source code is available on GitHub here:

https://github.com/rgolder1/jmh
