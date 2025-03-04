-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dml-triggers.html
INSERT INTO Library_view (Section, Booklist)
VALUES (
  'History', 
  book_list_t (book_t (121330, 'Alexander', 'Mirth', 'Y'))
);