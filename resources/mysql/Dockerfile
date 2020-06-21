FROM mysql
ENV MYSQL_ROOT_PASSWORD mysql
ENV MYSQL_DATABASE jmh
COPY ./resources/mysql/benchmark.sql /docker-entrypoint-initdb.d/
