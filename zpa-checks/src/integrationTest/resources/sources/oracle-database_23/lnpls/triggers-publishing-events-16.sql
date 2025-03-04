-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/triggers-publishing-events.html
INSERT INTO event_table
VALUES ('This object is a ' || 
        ora_dict_obj_type);