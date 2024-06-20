-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/file_specification.html
CREATE TABLESPACE stocks 
   DATAFILE 'stock1.dbf' SIZE 10M, 
            'stock2.dbf' SIZE 10M,
            'stock3.dbf' SIZE 10M;