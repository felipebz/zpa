-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/INSERT.html
CREATE TABLE raises (emp_id NUMBER, sal NUMBER 
   CONSTRAINT check_sal CHECK(sal > 8000));

EXECUTE DBMS_ERRLOG.CREATE_ERROR_LOG('raises', 'errlog');