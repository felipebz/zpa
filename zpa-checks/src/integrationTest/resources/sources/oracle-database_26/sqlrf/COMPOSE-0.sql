-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/COMPOSE.html
SELECT COMPOSE( 'o' || UNISTR('\0308') )
  FROM DUAL;