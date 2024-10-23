-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/considerations-when-using-lob-storage-json-data.html
      "SELECT json_serialize(jblob RETURNING BLOB) FROM myTab1";
    stmt.defineColumnType(1, OracleTypes.LONGVARBINARY, 1);
      br.close();
    rs.close();
    stmt.close();