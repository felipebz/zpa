-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_NUMBER.html
SELECT TO_NUMBER('-AusDollars100','L9G999D99',
   ' NLS_NUMERIC_CHARACTERS = '',.''
     NLS_CURRENCY            = ''AusDollars''
   ') "Amount"
     FROM DUAL;