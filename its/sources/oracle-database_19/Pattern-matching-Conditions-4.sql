SELECT salary 
    FROM employees 
    WHERE 'SM%' LIKE last_name
    ORDER BY salary;