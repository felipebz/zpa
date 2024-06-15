-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check.html
CREATE DOMAIN currency AS (
  amount        AS NUMBER(10, 2)
  currency_code AS CHAR(3 CHAR)
)
CONSTRAINT supported_currencies_c 
  CHECK ( currency_code IN ( 'USD', 'GBP', 'EUR', 'JPY' ) )
  DEFERRABLE INITIALLY DEFERRED
CONSTRAINT non_negative_amounts_c 
  CHECK ( amount >= 0 )
  DEFERRABLE INITIALLY DEFERRED;