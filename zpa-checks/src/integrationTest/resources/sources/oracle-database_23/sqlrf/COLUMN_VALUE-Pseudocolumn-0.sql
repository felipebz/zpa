-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/COLUMN_VALUE-Pseudocolumn.html
SELECT *
  FROM XMLTABLE('<a>123</a>');
SELECT COLUMN_VALUE
  FROM (XMLTable('<a>123</a>'));