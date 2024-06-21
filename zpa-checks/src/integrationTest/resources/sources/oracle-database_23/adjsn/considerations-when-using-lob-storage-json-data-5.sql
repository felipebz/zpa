-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/considerations-when-using-lob-storage-json-data.html
      "SELECT json_serialize(jclob RETURNING CLOB) FROM myTab2";
    stmt.defineColumnType(1, OracleTypes.LONGVARCHAR, 1);
      reader.close();
    rs.close();
    stmt.close();