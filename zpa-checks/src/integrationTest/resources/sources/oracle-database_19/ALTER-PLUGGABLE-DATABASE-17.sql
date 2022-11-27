-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-PLUGGABLE-DATABASE.html
ALTER PLUGGABLE DATABASE pdb5
  OPEN READ WRITE INSTANCES = ('ORCLDB_1', 'ORCLDB_2');