CREATE OR REPLACE PACKAGE app.config AS
  g NUMBER;
  c CONSTANT VARCHAR2(30) := 'value';
  "Mixed Case" NUMBER;
  anchor_source NUMBER;
  anchor_copy anchor_source%TYPE;
  row_copy orders%ROWTYPE;
  external_value ext_schema.external_type;
  ref_value REF ext_schema.obj_t;
  TYPE rec_t IS RECORD (a NUMBER);
  PROCEDURE g;
END config;
/
