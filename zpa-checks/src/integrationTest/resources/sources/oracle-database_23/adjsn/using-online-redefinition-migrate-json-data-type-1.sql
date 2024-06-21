-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-online-redefinition-migrate-json-data-type.html
CREATE TABLE j_purchaseorder_new (id VARCHAR2(32),
                                  date_loaded TIMESTAMP(6) WITH TIME ZONE,
                                  po_document JSON);