-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CORR.html
SELECT weight_class, CORR(list_price, min_price) "Correlation"
  FROM product_information
  GROUP BY weight_class
  ORDER BY weight_class, "Correlation";