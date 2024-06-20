-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT * 
            FROM ( VALUES (1,'SCOTT'), 
                          (2,'SMITH'), 
                          (3,'JOHN' ) 
                 ) t1 (employee_id, first_name);