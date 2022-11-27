-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
SELECT * FROM
  sales_external EXTERNAL MODIFY (LOCATION 'sales_9.csv’ REJECT LIMIT UNLIMITED);