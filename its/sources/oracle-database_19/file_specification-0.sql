-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/file_specification.html
CREATE DATABASE payable 
   LOGFILE GROUP 1 ('diska:log1.log', 'diskb:log1.log') SIZE 50K, 
           GROUP 2 ('diska:log2.log', 'diskb:log2.log') SIZE 50K 
   DATAFILE 'diskc:dbone.dbf' SIZE 30M;