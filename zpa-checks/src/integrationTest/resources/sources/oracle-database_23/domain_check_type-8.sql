-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check_type.html
CREATE TABLE order_items (
  order_id      INTEGER,
  product_id    INTEGER,
  amount        NUMBER(10, 2),
  currency_code CHAR(3 CHAR),
  DOMAIN currency(amount, currency_code)
);
INSERT INTO order_items
VALUES (1, 1,    9.99, 'USD'),
       (2, 2, 1234.56, 'GBP'),
       (3, 3, -999999, 'JPY'),
       (4, 4, 3141592, 'XXX') ,
       (5, 5, 2718281, '123');