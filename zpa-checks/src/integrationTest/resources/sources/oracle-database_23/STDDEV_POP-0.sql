-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/STDDEV_POP.html
SELECT STDDEV_POP(amount_sold) "Pop", 
   STDDEV_SAMP(amount_sold) "Samp"
   FROM sales;