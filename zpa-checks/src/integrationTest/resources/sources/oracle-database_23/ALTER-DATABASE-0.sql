-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-DATABASE.html
SELECT PROPERTY_VALUE FROM DATABASE_PROPERTIES 
  WHERE PROPERTY_NAME = 'DEFAULT_EDITION';