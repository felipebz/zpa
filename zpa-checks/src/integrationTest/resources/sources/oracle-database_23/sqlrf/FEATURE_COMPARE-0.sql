-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/FEATURE_COMPARE.html
SELECT 1-FEATURE_COMPARE(esa_wiki_mod USING 'There are several PGA tour golfers from South Africa' text AND USING 'Nick Price won the 2002 Mastercard Colonial Open' text) similarity FROM DUAL;