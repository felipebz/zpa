UPDATE employees 
    SET salary = 7000 
    WHERE last_name = 'Banda';
SAVEPOINT banda_sal;

UPDATE employees 
    SET salary = 12000 
    WHERE last_name = 'Greene';
SAVEPOINT greene_sal;

SELECT SUM(salary) FROM employees;

ROLLBACK TO SAVEPOINT banda_sal;
 
UPDATE employees 
    SET salary = 11000 
    WHERE last_name = 'Greene';
 
COMMIT;