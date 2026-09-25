-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SELECT.html
SELECT COUNT(*) * 10 FROM orders SAMPLE(10) SEED (1);
SELECT COUNT(*) * 10 FROM orders SAMPLE(10) SEED(4);
SELECT COUNT(*) * 10 FROM orders SAMPLE(10) SEED (1);