-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-TABLE.html
CREATE TABLE mydoc(id NUMBER, docCreationTime DATE, doc CLOB, json_doc JSON) INMEMORY PRIORITY CRITICAL 
    INMEMORY TEXT(DOC, JSON_DOC)