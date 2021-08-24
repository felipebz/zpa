CREATE INDEX salary_i 
   ON books (TREAT(author AS employee_t).salary);