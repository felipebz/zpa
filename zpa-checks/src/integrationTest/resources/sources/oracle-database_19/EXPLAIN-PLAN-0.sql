-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/EXPLAIN-PLAN.html
EXPLAIN PLAN 
    SET STATEMENT_ID = 'Raise in Tokyo' 
    INTO plan_table 
    FOR UPDATE employees 
        SET salary = salary * 1.10 
        WHERE department_id =  
           (SELECT department_id FROM departments
               WHERE location_id = 1700);