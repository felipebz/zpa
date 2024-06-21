-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
DROP TABLE Book_table;
CREATE TABLE Book_table (
  Booknum    NUMBER,
  Section    VARCHAR2(20),
  Title      VARCHAR2(20),
  Author     VARCHAR2(20),
  Available  CHAR(1)
);
INSERT INTO Book_table (
  Booknum, Section, Title, Author, Available
) 
VALUES (
  121001, 'Classic', 'Iliad', 'Homer', 'Y'
);
INSERT INTO Book_table (
  Booknum, Section, Title, Author, Available
) 
VALUES ( 
  121002, 'Novel', 'Gone with the Wind', 'Mitchell M', 'N'
);
SELECT * FROM Book_table ORDER BY Booknum;