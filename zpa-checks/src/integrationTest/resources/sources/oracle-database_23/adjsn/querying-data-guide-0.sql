-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/querying-data-guide.html
WITH dg_t AS (SELECT json_dataguide(po_document) dg_doc
                FROM j_purchaseorder)
  SELECT jt.*
    FROM dg_t,
         json_table(dg_doc, '$[*]'
           COLUMNS
             jpath   VARCHAR2(40) PATH '$."o:path"',
             type    VARCHAR2(10) PATH '$."type"',
             tlength NUMBER       PATH '$."o:length"') jt
   ORDER BY jt.jpath;