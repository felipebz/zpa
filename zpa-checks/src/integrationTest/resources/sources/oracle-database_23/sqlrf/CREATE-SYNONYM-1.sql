-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-SYNONYM.html
CREATE PUBLIC SYNONYM emp_table 
   FOR hr.employees@remote.us.example.com;