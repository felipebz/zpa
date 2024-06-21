-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQL_MACRO-clause.html
SELECT deptno,
            emp_doc(first_name => ename, hire_date => hiredate, doc_type => 'xml') doc
FROM scott.emp
ORDER BY ename;