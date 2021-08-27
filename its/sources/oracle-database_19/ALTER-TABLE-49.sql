-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
ALTER TABLE list_customers SPLIT PARTITION rest 
   VALUES ('CHINA', 'THAILAND')
   INTO (PARTITION asia, PARTITION rest);