-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NCHR.html
SELECT NCHR(187)
  FROM DUAL;

N
-
> 

SELECT CHR(187 USING NCHAR_CS)
  FROM DUAL;

C
-
>