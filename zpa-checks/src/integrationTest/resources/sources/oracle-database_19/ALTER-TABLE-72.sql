-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
ALTER TABLE employees
   MODIFY (salary ENCRYPT USING 'AES256' 'NOMAC');