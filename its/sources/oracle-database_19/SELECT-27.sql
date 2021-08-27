-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
SELECT COUNT(*) * 10 FROM orders SAMPLE(10) SEED (1);