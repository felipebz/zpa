-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NLS_CHARSET_DECL_LEN.html
SELECT NLS_CHARSET_DECL_LEN(200, nls_charset_id('ja16eucfixed')) 
  FROM DUAL;