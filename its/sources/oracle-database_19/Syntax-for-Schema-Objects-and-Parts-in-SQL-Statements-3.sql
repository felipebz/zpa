-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Syntax-for-Schema-Objects-and-Parts-in-SQL-Statements.html
CREATE VIEW Q1_2000_sales AS
  SELECT *
    FROM sales PARTITION (SALES_Q1_2000);

DELETE FROM Q1_2000_sales
  WHERE amount_sold < 0;