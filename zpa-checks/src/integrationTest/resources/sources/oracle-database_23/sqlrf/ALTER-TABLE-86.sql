-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE customers
   MODIFY (online_acct_pw DECRYPT);