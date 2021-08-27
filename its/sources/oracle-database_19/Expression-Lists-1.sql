-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Expression-Lists.html
SELECT * FROM employees 
  WHERE (first_name, last_name, email) IN 
  (('Guy', 'Himuro', 'GHIMURO'),('Karen', 'Colmenares', 'KCOLMENA'))