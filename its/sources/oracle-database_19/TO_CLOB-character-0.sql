-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_CLOB-character.html
UPDATE PRINT_MEDIA 
   SET AD_FINALTEXT = TO_CLOB (AD_FLTEXTN);