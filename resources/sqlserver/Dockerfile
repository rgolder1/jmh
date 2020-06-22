FROM microsoft/mssql-server-linux:2017-latest
ENV SA_PASSWORD SqlServer1!
ENV ACCEPT_EULA "Y"

# Create work directory
RUN mkdir -p /usr/work
WORKDIR /usr/work

# Copy all scripts into working directory
COPY ./resources/sqlserver/scripts/ /usr/work/

# Grant permissions for the import-data script to be executable
RUN chmod +x /usr/work/create-schema.sh

EXPOSE 1433

CMD /bin/bash /usr/work/entrypoint.sh
