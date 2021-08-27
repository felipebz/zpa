-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/POWERMULTISET.html
SELECT CAST(POWERMULTISET(cust_address_ntab) AS cust_address_tab_tab_typ)
  FROM customers_demo;
