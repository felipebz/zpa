-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overview-polymorphic-table-functions.html
WITH e AS 
 (SELECT * FROM emp NATURAL JOIN dept)
SELECT t.* FROM noop(e) t;