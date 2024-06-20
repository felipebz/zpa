-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE DOMAIN year_of_birth AS NUMBER(4)
      CONSTRAINT CHECK ( (trunc(year_of_birth) = year_of_birth) and (year_of_birth >= 1900) ) 
      DISPLAY (CASE WHEN year_of_birth < 2000 THEN '19-' ELSE '20-' END) || MOD(year_of_birth, 100)
      ORDER year_of_birth-1900 ;