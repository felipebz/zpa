-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-USER.html
CREATE USER ops$external_user
   IDENTIFIED EXTERNALLY
   DEFAULT TABLESPACE example
   QUOTA 5M ON example
   PROFILE app_user;