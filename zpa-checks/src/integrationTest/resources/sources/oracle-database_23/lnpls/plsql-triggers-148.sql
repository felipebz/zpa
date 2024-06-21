-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
INSERT INTO event_table
VALUES ('top stack error ' || 
        ora_server_error(1));