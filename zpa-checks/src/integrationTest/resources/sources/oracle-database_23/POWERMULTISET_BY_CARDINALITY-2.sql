-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/POWERMULTISET_BY_CARDINALITY.html
SELECT CAST(POWERMULTISET_BY_CARDINALITY(cust_address_ntab, 2)
         AS cust_address_tab_tab_typ)
  FROM customers_demo;