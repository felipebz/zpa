-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/loop-statements.html
DECLARE
   cursor_str VARCHAR2(500) := 'SELECT last_name, employee_id FROM hr.employees ORDER BY last_name';
   TYPE rec_t IS RECORD (last_name VARCHAR2(25),
                         employee_id NUMBER);
BEGIN
   FOR r rec_t IN VALUES OF (EXECUTE IMMEDIATE cursor_str) WHEN r.employee_id < 103 LOOP
      DBMS_OUTPUT.PUT_LINE(r.last_name || ', ' || r.employee_id);
   END LOOP;
END;
/