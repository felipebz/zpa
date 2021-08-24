SELECT hire_date, hire_date + TO_YMINTERVAL('01-02') "14 months"
   FROM employees;