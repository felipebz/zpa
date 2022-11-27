-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_NCHAR-datetime.html
SELECT TO_NCHAR(ORDER_DATE) AS order_date
   FROM ORDERS
   WHERE ORDER_STATUS > 9
   ORDER BY order_date;