-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_display.html
CREATE DOMAIN currency AS (
  amount        AS NUMBER(10, 2)
  currency_code AS CHAR(3 CHAR)
)
DISPLAY CASE currency_code
  WHEN 'USD' THEN '$'
  WHEN 'GBP' THEN '£'
  WHEN 'EUR' THEN '€'
  WHEN 'JPY' THEN '¥'
END || TO_CHAR(amount, '999,999,999.00');