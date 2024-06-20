-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_display.html
CREATE TABLE order_items (
  order_id      INTEGER,
  product_id    INTEGER,
  amount        NUMBER(10, 2),
  currency_code CHAR(3 CHAR),
  DOMAIN currency(amount, currency_code)
);