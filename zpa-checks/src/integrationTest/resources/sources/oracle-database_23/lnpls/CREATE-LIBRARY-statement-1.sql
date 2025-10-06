-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-LIBRARY-statement.html
CREATE OR REPLACE LIBRARY ext_lib AS 'ddl_1' IN ddl_dir CREDENTIAL ddl_cred;
/