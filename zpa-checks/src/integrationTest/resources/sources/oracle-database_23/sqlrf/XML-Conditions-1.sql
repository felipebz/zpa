-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XML-Conditions.html
SELECT ANY_PATH FROM RESOURCE_VIEW
   WHERE UNDER_PATH(res, '/sys/schemas/OE/www.example.com')=1;