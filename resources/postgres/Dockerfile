FROM postgres
ENV POSTGRES_PASSWORD postgres
ENV POSTGRES_DB jmh
COPY ./resources/postgres/benchmark.sql /docker-entrypoint-initdb.d/
