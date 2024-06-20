-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Format-Models.html
UPDATE table 
  SET date_column = TO_DATE(char, 'fmt');