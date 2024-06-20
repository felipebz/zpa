-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/BFILENAME.html
CREATE DIRECTORY media_dir AS '/demo/schema/product_media';
INSERT INTO print_media (product_id, ad_id, ad_graphic)
  VALUES (3000, 31001, BFILENAME('MEDIA_DIR', 'modem_comp_ad.gif'));