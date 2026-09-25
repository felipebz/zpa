-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/CREATE-TYPE-statement.html
CREATE TYPE corporate_customer_typ_demo UNDER customer_typ
    ( account_mgr_id     NUMBER(6)
    );
/