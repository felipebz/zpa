-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/triggers-publishing-events.html
INSERT INTO event_table
VALUES ('object owner is' || 
        ora_dict_obj_owner);