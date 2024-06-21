-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/performing-migration-check.html
SELECT po_document FROM table_owner.j_purchaseorder
  WHERE ROWID IN (SELECT pt.ERROR_ROW_ID
	             FROM my_precheck_table pt
	             WHERE pt.schema_name = table_owner
	               AND pt.table_name = j_purchaseorder
	               AND pt.column_name = po_document);