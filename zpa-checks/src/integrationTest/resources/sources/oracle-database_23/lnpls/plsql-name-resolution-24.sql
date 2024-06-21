-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-name-resolution.html
UPDATE tb1 SET col1.x = 10 WHERE col1.x = 20;
UPDATE tb1 SET tb1.col1.x = 10 WHERE tb1.col1.x = 20;
UPDATE hr.tb1 SET hr.tb1.col1.x = 10 WHERE hr.tb1.col1.x = 20;
DELETE FROM tb1 WHERE tb1.col1.x = 10;