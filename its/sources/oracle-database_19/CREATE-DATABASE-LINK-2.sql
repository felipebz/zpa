-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-DATABASE-LINK.html
CREATE DATABASE LINK local 
   CONNECT TO hr IDENTIFIED BY password
   USING 'local';