-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SYS_TYPEID.html
SELECT b.title, b.author.name, SYS_TYPEID(author)
   "Type_ID" FROM books b;