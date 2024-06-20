-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ORA_DM_PARTITION_NAME.html
SELECT prediction(mymodel using *) pred, ora_dm_partition_name(mymodel USING *) pname FROM customers;