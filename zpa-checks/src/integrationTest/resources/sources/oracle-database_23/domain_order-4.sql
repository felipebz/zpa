-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_order.html
CREATE DOMAIN currency AS (
  amount        AS NUMBER(10, 2)
  currency_code AS CHAR(3 CHAR)
)
ORDER currency_code || TO_CHAR(amount, '999999999.00');