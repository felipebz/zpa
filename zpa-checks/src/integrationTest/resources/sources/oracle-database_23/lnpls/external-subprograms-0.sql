-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/external-subprograms.html
      pstmt.setFloat(1, (1 + percent / 100));
      pstmt.setInt(2, empNo);
      pstmt.executeUpdate();
      pstmt.close();