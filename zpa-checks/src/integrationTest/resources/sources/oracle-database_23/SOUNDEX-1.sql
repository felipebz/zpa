-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SOUNDEX.html
SELECT last_name, first_name
     FROM hr.employees
     WHERE SOUNDEX(last_name)
         = SOUNDEX('SMYTHE')
     ORDER BY last_name, first_name;