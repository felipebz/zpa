-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-JAVA.html
CREATE JAVA SOURCE NAMED "Welcome" AS
   public class Welcome {
      public static String welcome() {
         return "Welcome World";   } }/