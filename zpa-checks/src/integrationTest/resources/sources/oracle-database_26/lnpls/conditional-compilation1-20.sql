-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/conditional-compilation1.html
CALL DBMS_PREPROCESSOR.PRINT_POST_PROCESSED_SOURCE (
  'PACKAGE', 'HR', 'MY_PKG'
);