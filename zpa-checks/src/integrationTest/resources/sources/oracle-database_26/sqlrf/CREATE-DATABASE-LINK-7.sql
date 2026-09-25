-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-DATABASE-LINK.html
CREATE SYNONYM emp_table 
   FOR oe.employees@remote.us.example.com;