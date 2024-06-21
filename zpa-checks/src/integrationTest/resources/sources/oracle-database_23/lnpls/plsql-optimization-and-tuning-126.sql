-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
WITH e AS (
       SELECT ename name, sal, deptno, loc 
	     FROM scott.emp NATURAL JOIN scott.dept 
        WHERE job = 'CLERK')
	  SELECT ROWNUM doc_id, t.*
	    FROM to_doc(e) t;