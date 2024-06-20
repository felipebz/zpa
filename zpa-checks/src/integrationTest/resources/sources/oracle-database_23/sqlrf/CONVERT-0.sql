-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CONVERT.html
SELECT CONVERT('Ä Ê Í Õ Ø A B C D E ', 'US7ASCII', 'WE8ISO8859P1') 
   FROM DUAL;