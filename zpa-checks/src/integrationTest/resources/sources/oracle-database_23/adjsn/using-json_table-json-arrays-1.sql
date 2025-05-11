-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/using-json_table-json-arrays.html
SELECT jt.*
  FROM j_purchaseorder,
       json_table(data, '$.ShippingInstructions.Phone[*]'
         COLUMNS (phone_type VARCHAR2(10) PATH '$.type',
                  phone_num  VARCHAR2(20) PATH '$.number')) AS "JT";