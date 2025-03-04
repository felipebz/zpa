-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
SELECT JSON_VALUE(document, '$.ENAME') ename, 
       JSON_VALUE(document, '$.COMM')  comm 
FROM   to_doc(scott.emp)
WHERE  JSON_VALUE(document, '$.DEPTNO') = 30;