# wait for the SQL Server to come up
sleep 5s

#run the setup script to create the DB and the schema in the DB
/opt/mssql-tools/bin/sqlcmd -S localhost -U SA -P "SqlServer1!" -i benchmark.sql
