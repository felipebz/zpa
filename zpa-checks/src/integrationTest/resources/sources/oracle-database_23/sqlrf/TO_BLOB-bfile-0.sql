-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_BLOB-bfile.html
SELECT TO_BLOB(media_col, 'JPEG') FROM media_tab;