-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
DECLARE
  name_list ora_name_list_t;
  number_modified PLS_INTEGER;
BEGIN
  IF (ora_sysevent='ASSOCIATE STATISTICS') THEN
    number_modified :=
     ora_dict_obj_name_list(name_list);
  END IF;
END;