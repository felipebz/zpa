-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Using-Extensible-Indexing.html
CREATE INDEX salary_index ON employees(salary) 
   INDEXTYPE IS position_indextype;