SELECT RANK(15500) WITHIN GROUP 
   (ORDER BY salary DESC) "Rank of 15500" 
   FROM employees;