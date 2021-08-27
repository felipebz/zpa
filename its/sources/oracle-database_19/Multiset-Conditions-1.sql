-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Multiset-Conditions.html
SELECT product_id, TO_CHAR(ad_finaltext) AS text
   FROM print_media
   WHERE ad_textdocs_ntab IS NOT EMPTY 
   ORDER BY product_id, text;