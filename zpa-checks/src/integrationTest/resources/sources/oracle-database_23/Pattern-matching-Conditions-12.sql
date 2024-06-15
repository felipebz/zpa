-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Pattern-matching-Conditions.html
SELECT last_name
FROM employees
WHERE REGEXP_LIKE (last_name, '([aeiou])\1', 'i')
ORDER BY last_name;