-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLDATA-Pseudocolumn.html
CREATE TABLE xml_lob_tab of XMLTYPE
  XMLTYPE STORE AS CLOB;