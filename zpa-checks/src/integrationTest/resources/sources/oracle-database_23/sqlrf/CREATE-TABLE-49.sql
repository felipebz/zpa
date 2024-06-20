-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
SELECT partition_name, high_value FROM user_tab_partitions  WHERE table_name = 'CUSTOMERS_DEMO';