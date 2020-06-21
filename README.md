# JMH benchmark library

Work in progress...

## Overview

Demonstrate using the JMH benchmarking library with running SQL queries.

Compare the effect on performance of:

- different database indexes
- different database types

## Running Benchmark Test

Typically the benchmark test is not a Springboot test requiring autowiring of components such as the JdbcTemplate.  The suggested approach of using a main method in the BenchmarkBase class is not compatible with this.

Indeed the benchmark profile defined in the pom is not compatible with running as a SpringBoot test, but is left as reference as a typical approach to non-SpringBoot benchmark tests.

Therefore run via the IDE.  Right click on the executeJmhRunner() @Test method, and select the actual test that is defined in a subclass to run.

It can be run via maven with mvn clean test, however this takes a lot longer. 
