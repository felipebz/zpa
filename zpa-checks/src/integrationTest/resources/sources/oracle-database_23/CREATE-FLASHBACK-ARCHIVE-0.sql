-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-FLASHBACK-ARCHIVE.html
CREATE FLASHBACK ARCHIVE DEFAULT test_archive1
   TABLESPACE example
   QUOTA 1 M
   RETENTION 1 DAY;

CREATE FLASHBACK ARCHIVE test_archive2
   TABLESPACE example
   QUOTA 1 M
   RETENTION 1 DAY;