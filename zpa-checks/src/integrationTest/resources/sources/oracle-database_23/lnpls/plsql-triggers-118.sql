-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
INSERT INTO event_table 
VALUES ('Changed object is ' ||
        ora_dict_obj_name);