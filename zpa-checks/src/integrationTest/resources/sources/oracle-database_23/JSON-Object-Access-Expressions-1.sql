-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON-Object-Access-Expressions.html
SELECT po.po_document.ShippingInstructions.Phone
  FROM j_purchaseorder po;