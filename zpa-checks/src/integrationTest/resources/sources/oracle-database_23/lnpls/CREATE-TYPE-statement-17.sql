-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-TYPE-statement.html
CREATE TABLE emptab OF employee_t;
INSERT INTO emptab
   VALUES (employee_t.construct_emp('John Smith', NULL));