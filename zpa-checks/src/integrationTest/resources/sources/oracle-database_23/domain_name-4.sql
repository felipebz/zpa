-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_name.html
CREATE DOMAIN co.currency AS (
  amount        AS NUMBER(10, 2)
  currency_code AS CHAR(3 CHAR)
);