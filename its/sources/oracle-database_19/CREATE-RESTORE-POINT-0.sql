-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-RESTORE-POINT.html
CREATE RESTORE POINT good_data;

SELECT salary FROM employees WHERE employee_id = 108;