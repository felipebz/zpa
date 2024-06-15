-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLESPACE.html
CREATE TABLESPACE encts2 DATAFILE ‘encts2.f’ SIZE 1G ENCRYPTION USING AES256 MODE ‘XTS’ ENCRYPT;