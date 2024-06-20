-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/MERGE.html
CREATE TABLE bonuses (employee_id NUMBER, bonus NUMBER DEFAULT 100);
INSERT INTO bonuses(employee_id)
   (SELECT e.employee_id FROM hr.employees e, oe.orders o
   WHERE e.employee_id = o.sales_rep_id
   GROUP BY e.employee_id); 

SELECT * FROM bonuses ORDER BY employee_id;