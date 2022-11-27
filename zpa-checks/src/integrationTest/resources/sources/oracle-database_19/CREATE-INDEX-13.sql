-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-INDEX.html
CREATE INDEX src_idx ON print_media(text_length(ad_sourcetext));

SELECT product_id FROM print_media 
   WHERE text_length(ad_sourcetext) < 1000
   ORDER BY product_id;