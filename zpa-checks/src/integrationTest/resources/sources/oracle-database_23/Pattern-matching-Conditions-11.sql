-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Pattern-matching-Conditions.html
SELECT first_name, last_name
FROM employees
WHERE REGEXP_LIKE (first_name, '^Ste(v|ph)en$')
ORDER BY first_name, last_name;