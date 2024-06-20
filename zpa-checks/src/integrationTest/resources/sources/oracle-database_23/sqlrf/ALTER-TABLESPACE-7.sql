-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ALTER-TABLESPACE.html
ALTER TABLESPACE tbs_02
  RENAME DATAFILE 'diskb:tbs_f5.dbf'
  TO              'diska:tbs_f5.dbf';