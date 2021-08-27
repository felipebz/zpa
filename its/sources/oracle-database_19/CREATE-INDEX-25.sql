-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-INDEX.html
CREATE INDEX salary_i 
   ON books (TREAT(author AS employee_t).salary);