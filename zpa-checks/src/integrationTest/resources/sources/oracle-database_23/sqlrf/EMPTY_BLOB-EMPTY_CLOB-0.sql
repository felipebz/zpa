-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/EMPTY_BLOB-EMPTY_CLOB.html
UPDATE print_media
  SET ad_photo = EMPTY_BLOB();