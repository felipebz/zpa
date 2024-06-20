-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_NUMBER.html
UPDATE employees SET salary = salary + 
   TO_NUMBER('100.00', '9G999D99')
   WHERE last_name = 'Perkins';