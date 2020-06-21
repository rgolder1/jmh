# JMH benchmark library

Work in progress...

## Overview

Demonstrate using the JMH benchmarking library with running SQL queries.

Compare the effect on performance of:

- different database indexes
- different database types

## Running Benchmark Test

This demo uses SpringBoot tests such that the benchmarks execute against the application running as it would be normally.  Typically benchmark tests are not Springboot tests, which require autowiring of components such as the JdbcTemplate.   As such the usual suggested approach of using a main method in the BenchmarkBase class is not compatible with running as a SpringBoot test.

Indeed the benchmark profile defined in the pom is not compatible with running as a SpringBoot test, but is left as reference as a standard approach for non-SpringBoot benchmark tests.

Therefore run via the IDE.  Right click on the executeJmhRunner() @Test method, and select the actual test that is defined in a subclass to run.

It can be run via maven with mvn clean test, however this takes a lot longer. 

## Running Against Postgres

The SpringBoot test defines the database connection property overrides.  Comment these out to run against the default in-memory H2 database.  Leave the properties commented in to run against a Postgres instance.

To start a Postgres instance running locally in a docker container, with Docker itself running locally, run the script in the root ./dockerBuildAndStart.sh

This will pull a base Postgres docker image, build, and start, with the benchmark.sql script included.  This script creates the schema and the event table that are required for the test.

At the end of the test runs the ./dockerStop.sh script can be used to stop and remove the Postgres docker container. 
