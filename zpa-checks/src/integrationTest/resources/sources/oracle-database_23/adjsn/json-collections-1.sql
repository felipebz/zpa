-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-collections.html
ocstmt.registerReturnParameter(1, OracleTypes.JSON);
ocstmt.executeUpdate();
System.out.println("Retrieved _id : " + rs.getObject(1, OracleJsonBinary.class));