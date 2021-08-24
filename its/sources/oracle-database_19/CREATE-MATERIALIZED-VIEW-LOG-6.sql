CREATE MATERIALIZED VIEW LOG ON product_information 
   WITH ROWID, SEQUENCE (list_price, min_price, category_id), PRIMARY KEY
   INCLUDING NEW VALUES;