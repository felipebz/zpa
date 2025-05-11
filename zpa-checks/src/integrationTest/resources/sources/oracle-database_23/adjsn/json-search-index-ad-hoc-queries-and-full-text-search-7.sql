-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-search-index-ad-hoc-queries-and-full-text-search.html
BEGIN
  CTX_DDL.create_path_list('json_pl', CTX_DDL.PATHLIST_JSON, CTX_DDL.PATHLIST_INCLUDE);
  CTX_DDL.add_path('json_pl', 'TEXT',     '$.SpecialInstructions');
  CTX_DDL.add_path('json_pl', 'TEXT',     '$.LineItems.Part.Description');
  CTX_DDL.add_path('json_pl', 'NUMBER',   '$.PONumber');
  CTX_DDL.add_path('json_pl', 'NUMBER',   '$.LineItems.Part.UnitPrice');
  CTX_DDL.add_path('json_pl', 'VARCHAR2', '$.Reference');
  CTX_DDL.add_path('json_pl', 'VARCHAR2', '$.User');
  CTX_DDL.add_path('json_pl', 'VARCHAR2', '$.ShippingInstructions.name');
  CTX_DDL.add_path('json_pl', 'VARCHAR2', '$.ShippingInstructions.Address.zipCode');
END;
/
CREATE SEARCH INDEX po_search_idx ON j_purchaseorder (data)
  FOR JSON PARAMETERS ('PATHLIST json_pl');