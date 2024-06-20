-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_CLOB-bfile-blob.html
SELECT TO_CLOB(docu, 873, 'text/xml') FROM media_tab;