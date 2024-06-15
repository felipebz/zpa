-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-JAVA.html
CREATE JAVA RESOURCE NAMED "appText" 
   USING BFILE (java_dir, 'textBundle.dat')
/