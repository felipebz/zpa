-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE customers ADD (cust_cell_phone_number Varchar2(12) DOMAIN phone_number);