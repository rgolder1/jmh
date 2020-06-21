FROM postgres
ENV POSTGRES_PASSWORD postgres
ENV POSTGRES_DB test
COPY benchmark.sql /docker-entrypoint-initdb.d/
