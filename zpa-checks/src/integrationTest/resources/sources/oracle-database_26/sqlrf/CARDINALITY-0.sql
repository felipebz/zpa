-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CARDINALITY.html
SELECT product_id, CARDINALITY(ad_textdocs_ntab) cardinality
  FROM print_media
  ORDER BY product_id;