-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/considerations-when-using-lob-storage-json-data.html
    stmt.setFetchSize(1000); // Set batch fetch size to 1000 rows.

      "SELECT json_serialize(jclob RETURNING CLOB VALUE) FROM myTab2";
      clob.free();
    rs.close();
    stmt.close();