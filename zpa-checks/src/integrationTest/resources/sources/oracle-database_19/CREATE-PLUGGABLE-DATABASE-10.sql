-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-PLUGGABLE-DATABASE.html
CREATE PLUGGABLE DATABASE newpdb FROM salespdb
  FILE_NAME_CONVERT = ('/disk1/oracle/dbs/salespdb/', '/disk1/oracle/dbs/newpdb/')
  PATH_PREFIX = '/disk1/oracle/dbs/newpdb';