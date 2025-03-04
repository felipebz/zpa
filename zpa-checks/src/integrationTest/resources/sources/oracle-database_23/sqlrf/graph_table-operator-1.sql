-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph_table-operator.html
CREATE TABLE persons (
     person_id NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT
     BY 1),
     name VARCHAR2(10),
     birthdate DATE,
     height FLOAT DEFAULT ON NULL 0,
     person_data JSON,
     CONSTRAINT person_pk PRIMARY KEY (person_id));
INSERT INTO persons (name, height, birthdate, person_data)
       VALUES ('John', 1.80, to_date('13/06/1963', 'DD/MM/YYYY'), '{"department":"IT","role":"Software Developer"}');
INSERT INTO persons (name, height, birthdate, person_data)
       VALUES ('Mary', 1.65, to_date('25/09/1982', 'DD/MM/YYYY'), '{"department":"HR","role":"HR Manager"}');
INSERT INTO persons (name, height, birthdate, person_data)
       VALUES ('Bob', 1.75, to_date('11/03/1966', 'DD/MM/YYYY'), '{"department":"IT","role":"Technical Consultant"}');
INSERT INTO persons (name, height, birthdate, person_data)
       VALUES ('Alice', 1.70, to_date('01/02/1987', 'DD/MM/YYYY'), '{"department":"HR","role":"HR Assistant"}');