-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/EXTRACT-datetime.html
SELECT EXTRACT(month FROM order_date) "Month", COUNT(order_date) "No. of Orders"
  FROM orders
  GROUP BY EXTRACT(month FROM order_date)
  ORDER BY "No. of Orders" DESC, "Month";