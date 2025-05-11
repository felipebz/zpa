-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-online-redefinition-migrate-json-data-type.html
ALTER TABLE j_purchaseorder_new ADD (vc_user GENERATED ALWAYS AS
  (json_value(data, '$.User' RETURNING VARCHAR2(20))));
ALTER TABLE j_purchaseorder_new ADD (vc_costcenter GENERATED ALWAYS AS
  (json_value(data, '$.CostCenter' RETURNING VARCHAR2(6))));