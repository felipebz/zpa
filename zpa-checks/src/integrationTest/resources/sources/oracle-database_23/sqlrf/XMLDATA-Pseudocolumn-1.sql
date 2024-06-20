-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLDATA-Pseudocolumn.html
ALTER TABLE xml_lob_tab
  MODIFY LOB (XMLDATA) (STORAGE (MAXSIZE 2G) CACHE);