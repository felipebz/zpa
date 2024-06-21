-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/ALTER-TYPE-statement.html
CREATE TABLE text (
   doc_id       NUMBER,
   description  textdoc_tab)
   NESTED TABLE description STORE AS text_store;
ALTER TYPE textdoc_typ 
   ADD ATTRIBUTE (author VARCHAR2) CASCADE;