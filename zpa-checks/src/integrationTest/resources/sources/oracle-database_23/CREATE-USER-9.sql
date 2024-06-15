-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-USER.html
CREATE USER c##comm_user
   IDENTIFIED BY comm_pwd
   DEFAULT TABLESPACE example
   QUOTA 20M ON example
   TEMPORARY TABLESPACE temp_tbs;