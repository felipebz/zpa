-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-FLASHBACK-ARCHIVE.html
ALTER TABLE oe.orders
   FLASHBACK ARCHIVE test_archive2;