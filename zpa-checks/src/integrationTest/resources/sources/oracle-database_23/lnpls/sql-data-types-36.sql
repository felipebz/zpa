-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DROP TABLE vecLogTable;
DROP SEQUENCE vecTrgSeq;
CREATE TABLE vecLogTable (embedding VECTOR(3, float32), 
        describe VARCHAR2(25), seq NUMBER);
CREATE SEQUENCE vecTrgSeq;
CREATE OR REPLACE TRIGGER vecTrg 
BEFORE UPDATE ON theVectorTable
FOR EACH ROW
BEGIN
  INSERT INTO vecLogTable VALUES (:old.embedding, 'OLD.VECTRG',
          vecTrgSeq.NEXTVAL);
  INSERT INTO vecLogTable VALUES (:new.embedding, 'NEW.VECTRG',
          vecTrgSeq.NEXTVAL);
END;
/
UPDATE theVectorTable SET embedding='[2.22, 4.44, 6.66]' WHERE id=2;
SELECT * FROM vecLogTable ORDER BY seq;