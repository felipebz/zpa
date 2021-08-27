-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-TABLE.html
CREATE TABLE print_media_part (
    product_id NUMBER(6),
    ad_id              NUMBER(6),
    ad_composite       BLOB,
    ad_sourcetext      CLOB,
    ad_finaltext       CLOB,
    ad_fltextn         NCLOB,
    ad_textdocs_ntab   TEXTDOC_TAB,
    ad_photo           BLOB,
    ad_graphic         BFILE,
    ad_header          ADHEADER_TYP)
  NESTED TABLE ad_textdocs_ntab STORE AS textdoc_nt
  PARTITION BY RANGE (product_id)
    (PARTITION p1 VALUES LESS THAN (100),
     PARTITION p2 VALUES LESS THAN (200));