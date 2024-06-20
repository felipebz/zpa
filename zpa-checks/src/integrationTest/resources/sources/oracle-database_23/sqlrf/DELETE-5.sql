-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/DELETE.html
DELETE FROM sales PARTITION (sales_q1_1998)
   WHERE amount_sold > 1000;