-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/loop-statements.html
v := vec_rec_t( FOR r rec_t IN (EXECUTE IMMEDIATE query_var) SEQUENCE => r);