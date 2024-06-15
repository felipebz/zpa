-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE countries
   ENABLE PRIMARY KEY
   EXCEPTIONS INTO except_table;