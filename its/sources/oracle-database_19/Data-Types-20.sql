CREATE TYPE SDO_GEOMETRY AS OBJECT
  (sgo_gtype        NUMBER, 
   sdo_srid         NUMBER,
   sdo_point        SDO_POINT_TYPE,
   sdo_elem_info    SDO_ELEM_INFO_ARRAY,
   sdo_ordinates    SDO_ORDINATE_ARRAY);
/