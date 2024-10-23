-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/considerations-when-using-lob-storage-json-data.html
    stmt.setFetchSize(1000); // Set batch fetch size to 1000 rows.

      "SELECT json_serialize(jblob RETURNING BLOB) FROM myTab1";
    rs.close();
    stmt.close();