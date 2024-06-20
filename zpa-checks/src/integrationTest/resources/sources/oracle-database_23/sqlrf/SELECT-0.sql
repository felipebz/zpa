-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT * FROM
  sales_external EXTERNAL MODIFY (LOCATION 'sales_9.csv’ REJECT LIMIT UNLIMITED);