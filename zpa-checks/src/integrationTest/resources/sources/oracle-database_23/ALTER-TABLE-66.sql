-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLE.html
ALTER TABLE print_media_part 
   MOVE PARTITION p2b TABLESPACE omf_ts1;