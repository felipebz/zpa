-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-DIMENSION.html
ALTER DIMENSION customers_dim
   DROP ATTRIBUTE country;
ALTER DIMENSION customers_dim
   ADD LEVEL zone IS customers.cust_postal_code
   ADD ATTRIBUTE zone DETERMINES (cust_city);