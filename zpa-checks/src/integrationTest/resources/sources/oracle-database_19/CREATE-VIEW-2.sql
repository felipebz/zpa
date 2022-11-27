-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-VIEW.html
CREATE VIEW emp_sal (emp_id, last_name, 
      email UNIQUE RELY DISABLE NOVALIDATE,
   CONSTRAINT id_pk PRIMARY KEY (emp_id) RELY DISABLE NOVALIDATE)
   AS SELECT employee_id, last_name, email FROM employees;