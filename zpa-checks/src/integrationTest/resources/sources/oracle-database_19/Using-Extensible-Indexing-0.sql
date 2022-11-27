-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Using-Extensible-Indexing.html
SELECT last_name, salary FROM
   (SELECT last_name, DENSE_RANK() OVER
      (ORDER BY salary DESC) rank_val, salary FROM employees)
   WHERE rank_val BETWEEN 10 AND 20;