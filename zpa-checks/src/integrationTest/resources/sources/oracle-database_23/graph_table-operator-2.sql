-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/graph_table-operator.html
CREATE TABLE students (
      s_id NUMBER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
      s_univ_id NUMBER,
      s_person_id NUMBER,
      subject VARCHAR2(10),
      
      CONSTRAINT stud_pk PRIMARY KEY (s_id),
      CONSTRAINT stud_fk_person FOREIGN KEY (s_person_id) REFERENCES persons(person_id),
      CONSTRAINT stud_fk_univ FOREIGN KEY (s_univ_id) REFERENCES university(id)
    );


INSERT INTO students(s_univ_id, s_person_id,subject, height) VALUES (1,1,'Arts');
INSERT INTO students(s_univ_id, s_person_id,subject, height) VALUES (1,3,'Music');
INSERT INTO students(s_univ_id, s_person_id,subject, height) VALUES (2,2,'Math');
INSERT INTO students(s_univ_id, s_person_id,subject, height) VALUES (2,4,'Science');