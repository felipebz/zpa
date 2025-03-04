-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-injection.html
       DBMS_OUTPUT.PUT_LINE('user_name: Anybody');
       DBMS_OUTPUT.PUT_LINE('service_type: Anything');
       DELETE FROM secret_records WHERE service_type=INITCAP('Merger');
     END;