-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-USER.html
CREATE USER global_user
   IDENTIFIED GLOBALLY AS 'CN=analyst, OU=division1, O=oracle, C=US'
   DEFAULT TABLESPACE example
   QUOTA 5M ON example;