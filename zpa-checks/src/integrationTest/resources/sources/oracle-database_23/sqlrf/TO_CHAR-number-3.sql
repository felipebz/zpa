-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_CHAR-number.html
SELECT TO_CHAR(-10000,'L99G999D99MI',
   'NLS_NUMERIC_CHARACTERS = '',.''
   NLS_CURRENCY = ''AusDollars'' ') "Amount"
     FROM DUAL;