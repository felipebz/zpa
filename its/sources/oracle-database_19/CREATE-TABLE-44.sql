CREATE TABLE hash_products 
    ( product_id          NUMBER(6)   PRIMARY KEY
    , product_name        VARCHAR2(50) 
    , product_description VARCHAR2(2000) 
    , category_id         NUMBER(2) 
    , weight_class        NUMBER(1) 
    , warranty_period     INTERVAL YEAR TO MONTH 
    , supplier_id         NUMBER(6) 
    , product_status      VARCHAR2(20) 
    , list_price          NUMBER(8,2) 
    , min_price           NUMBER(8,2) 
    , catalog_url         VARCHAR2(50) 
    , CONSTRAINT          product_status_lov_demo 
                          CHECK (product_status in ('orderable' 
                                                  ,'planned' 
                                                  ,'under development' 
                                                  ,'obsolete') 
 ) ) 
 PARTITION BY HASH (product_id) 
 PARTITIONS 4 
 STORE IN (tbs_01, tbs_02, tbs_03, tbs_04);