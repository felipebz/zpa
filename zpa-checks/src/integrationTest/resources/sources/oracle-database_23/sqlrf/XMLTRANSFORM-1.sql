-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/XMLTRANSFORM.html
SELECT XMLTRANSFORM(w.warehouse_spec, x.col1).GetClobVal()
   FROM warehouses w, xsl_tab x
   WHERE w.warehouse_name = 'San Francisco';