-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Using-XML-in-SQL-Statements.html
CREATE TABLE xwarehouses OF XMLTYPE
   XMLSCHEMA "http://www.example.com/xwarehouses.xsd"
   ELEMENT "Warehouse";