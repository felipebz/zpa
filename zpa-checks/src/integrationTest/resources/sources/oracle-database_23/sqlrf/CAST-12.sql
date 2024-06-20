-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CAST.html
SELECT 
    CAST ( 'yes' AS BOOLEAN ),
    CAST ( true AS NUMBER ),
    CAST ( false AS VARCHAR2(10) );