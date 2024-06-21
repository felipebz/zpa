-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-program-limits.html
SELECT * FROM user_object_size WHERE name = 'PKG1' ORDER BY type;