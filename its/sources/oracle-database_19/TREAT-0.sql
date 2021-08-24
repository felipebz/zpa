SELECT name, TREAT(VALUE(p) AS employee_t).salary salary 
   FROM persons p;