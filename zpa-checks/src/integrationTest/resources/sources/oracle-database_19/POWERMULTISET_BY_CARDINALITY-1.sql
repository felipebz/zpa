-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/POWERMULTISET_BY_CARDINALITY.html
UPDATE customers_demo
  SET cust_address_ntab = cust_address_ntab MULTISET UNION cust_address_ntab;