-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
    RAISE;
    RAISE_APPLICATION_ERROR(-20000, 'Irrecoverable error.');
END;