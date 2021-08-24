UPDATE employees 
    SET salary = salary * 1.1
    WHERE employee_id IN (SELECT employee_id FROM job_history);