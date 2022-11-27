-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/COMPOSE.html
SELECT COMPOSE( 'o' || UNISTR('\0308') )
  FROM DUAL; 

CO 
-- 
ö