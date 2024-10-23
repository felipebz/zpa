-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-search-index-ad-hoc-queries-and-full-text-search.html
CREATE SEARCH INDEX po_search_idx ON j_purchaseorder (po_document)
  FOR JSON PARAMETERS ('SEARCH_ON
    TEXT INCLUDE    ($.SpecialInstructions,
                     $.LineItems.Part.Description)
    VALUE(NUMBER)   ($.PONumber, $.LineItems.Part.UnitPrice)
    VALUE(VARCHAR2) ($.Reference, $.User,
                     $.ShippingInstructions.name,
                     $.ShippingInstructions.Address.zipCode)');