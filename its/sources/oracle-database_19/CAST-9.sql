CREATE TABLE projects 
  (employee_id NUMBER, project_name VARCHAR2(10));

CREATE TABLE emps_short 
  (employee_id NUMBER, last_name VARCHAR2(10));

CREATE TYPE project_table_typ AS TABLE OF VARCHAR2(10);
/