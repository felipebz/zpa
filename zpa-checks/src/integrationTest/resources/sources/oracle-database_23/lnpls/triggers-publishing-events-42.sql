-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/triggers-publishing-events.html
INSERT INTO event_table
VALUES ('top stack error message' ||
        ora_server_error_msg(1));