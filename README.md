# JMH Benchmark Library Demo

## Overview

Demonstrate using the JMH benchmarking library with running SQL queries.

Compare the effect on performance of:

- different database indexes
- different database types
- different WHERE and ORDER BY clauses

## Running The Benchmark Test

This demo uses SpringBoot tests such that the benchmarks execute against the application running as it would be normally.  Typically benchmark tests are not Springboot tests, which require autowiring of components such as the JdbcTemplate.   As such the usual suggested approach of using a main method in the BenchmarkBase class is not compatible with running as a SpringBoot test.

Indeed the benchmark profile defined in the pom is not compatible with running as a SpringBoot test, but is left as reference as a standard approach for non-SpringBoot benchmark tests.

Therefore run via the IDE.  Right click on the test class and select Run test.  Alternatively right click on the executeJmhRunner() @Test method in BenchmarkBase, and select the actual test that is defined in a subclass to run.

It can be run via maven with mvn clean test, however this takes a lot longer. 

Configure properties in benchmark.properties, such as the number of iterations and number of concurrent threads to use.

Change the database indexes to use in the test setup to compare performance.

## Running Against Different Database Types

The application test properties files define the database connection properties for H2, Postgres and MySQL.  Uncomment the active-profile corresponding to the required database type that is defined at the top of the test class.  e.g. for Postgres use the profile "test-postgres". 

To start a Postgres instance running locally in a docker container, with Docker itself running locally, run the script in the root ./dockerBuildAndStartPostgres.sh

This will pull a base Postgres docker image, build, and start, with the resources/postgres/benchmark.sql script included.  This script creates the schema and the event table that are required for the test.

Likewise for a MySQL instance, run ./dockerBuildAndStartMySql.sh

H2 is an in-memory database, so no docker container is applicable.

At the end of the test the ./dockerStopPostgres.sh script can be used to stop and remove the Postgres docker container.  Likewise use ./dockerStopMySql.sh for MySQL. 

### Viewing The Results

Scroll to the bottom of the test run log output to view the benchmark results.  Or view the json file generated in the target dir.

For a comparison of the SQL Selects test runs performed against different database types, with different indexes in place, see the results commented in the test class:
[SqlQueryBenchmarkTest.java](./src/test/java/com/aztec/jmh/benchmark/SqlQueryBenchmarkTest.java)

### Benchmarking Other Databases

To benchmark performance against other databases:

1. Create a Dockerfile and start up SQL file under the root resources/ dir for the database.
1. Create a script in the root dir to pull and start a suitable docker container (similar to dockerBuildAndStartPostgres.sql).
2. Create an application test properties file with the required connection properties (similar to application-test-postgres.yml).
3. Set the active profile in the SpringBoot test class to pull in this properties file (similar to @ActiveProfiles("test-postgres")).
4. Ensure the syntax for the create/drop indexes is correct in the test class. 
