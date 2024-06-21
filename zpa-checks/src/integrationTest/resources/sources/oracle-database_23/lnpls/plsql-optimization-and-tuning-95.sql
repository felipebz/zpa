-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-optimization-and-tuning.html
DROP TABLE gradereport;
CREATE TABLE gradereport (
  student VARCHAR2(30),
  subject VARCHAR2(30),
  weight NUMBER,
  grade NUMBER
);
INSERT INTO gradereport (student, subject, weight, grade)
VALUES ('Mark', 'Physics', 4, 4);
INSERT INTO gradereport (student, subject, weight, grade) 
VALUES ('Mark','Chemistry', 4, 3);
INSERT INTO gradereport (student, subject, weight, grade) 
VALUES ('Mark','Maths', 3, 3);
INSERT INTO gradereport (student, subject, weight, grade) 
VALUES ('Mark','Economics', 3, 4);