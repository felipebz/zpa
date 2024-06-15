-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_CHAR-character.html
SELECT TO_CHAR(ad_sourcetext) FROM print_media
      WHERE product_id = 2268;