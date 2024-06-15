-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/DELETE.html
CREATE TABLE product_price_history ( 
  product_id          INTEGER NOT NULL, 
  price               INTEGER NOT NULL, 
  currency_code       VARCHAR2(3 CHAR) NOT NULL, 
  effective_from_date DATE NOT NULL, 
  effective_to_date   DATE, 
  CONSTRAINT product_price_history_pk 
    PRIMARY KEY (product_id, currency_code, effective_from_date) 
) PARTITION BY RANGE (effective_from_date) ( 
    PARTITION p0 VALUES less than (DATE'2015-01-02'), 
    PARTITION p1 VALUES less than (DATE'2015-01-03'), 
    PARTITION p2 VALUES less than (DATE'2015-01-04') 
);

INSERT INTO product_price_history 
  WITH prices AS ( 
    SELECT 1, 100, 'USD', DATE'2015-01-01', DATE'2015-01-02' 
    FROM   dual UNION ALL 
    SELECT 1, 60, 'GBP', DATE'2015-01-01', DATE'2015-01-02' 
    FROM   dual UNION ALL 
    SELECT 1, 110, 'EUR', DATE'2015-01-01', DATE'2015-01-02' 
    FROM   dual UNION ALL 
    SELECT 1, 101, 'USD', DATE'2015-01-02', DATE'2015-01-03' 
    FROM   dual UNION ALL 
    SELECT 1, 62, 'GBP', DATE'2015-01-02', DATE'2015-01-03' 
    FROM   dual UNION ALL 
    SELECT 1, 109, 'EUR', DATE'2015-01-02', DATE'2015-01-03' 
    FROM   dual UNION ALL 
    SELECT 1, 105, 'USD', DATE'2015-01-03', NULL 
    FROM   dual UNION ALL 
    SELECT 1, 61, 'GBP', DATE'2015-01-03', NULL 
    FROM   dual UNION ALL 
    SELECT 1, 107, 'EUR', DATE'2015-01-03', NULL 
    FROM   dual UNION ALL 
    SELECT 2, 30, 'USD', DATE'2015-01-01', DATE'2015-01-03' 
    FROM   dual UNION ALL 
    SELECT 2, 33, 'USD', DATE'2015-01-03', NULL 
    FROM   dual UNION ALL 
    SELECT 3, 100, 'GBP', DATE'2015-01-03', NULL 
    FROM   dual 
  ) 
SELECT * 
FROM   prices;