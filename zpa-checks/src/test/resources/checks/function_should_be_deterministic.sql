CREATE FUNCTION double_value(p_value NUMBER) RETURN NUMBER IS -- Noncompliant
BEGIN
  RETURN p_value * 2;
END;
/

CREATE FUNCTION constant_value RETURN NUMBER IS -- Noncompliant
BEGIN
  RETURN 42;
END;
/

CREATE FUNCTION calculate_value(p_value NUMBER) RETURN NUMBER IS -- Noncompliant
  v_result NUMBER;
BEGIN
  v_result := p_value * 2;
  RETURN v_result + 1;
END;
/

CREATE FUNCTION clamp_positive(p_value NUMBER) RETURN NUMBER IS -- Noncompliant
BEGIN
  IF p_value < 0 THEN
    RETURN 0;
  END IF;
  RETURN p_value;
END;
/

CREATE FUNCTION case_value(p_value NUMBER) RETURN NUMBER IS -- Noncompliant
BEGIN
  CASE
    WHEN p_value < 0 THEN
      RETURN 0;
    ELSE
      RETURN p_value;
  END CASE;
END;
/

CREATE FUNCTION null_then_value(p_value NUMBER) RETURN NUMBER IS -- Noncompliant
BEGIN
  NULL;
  RETURN p_value;
END;
/

CREATE FUNCTION dual_value RETURN NUMBER IS
  v_value NUMBER;
BEGIN
  SELECT 1 INTO v_value FROM dual;
  RETURN v_value;
END;
/

CREATE FUNCTION cte_value RETURN NUMBER IS
  v_value NUMBER;
BEGIN
  WITH values_cte AS (
    SELECT 1 AS value FROM dual
  )
  SELECT value INTO v_value FROM values_cte;
  RETURN v_value;
END;
/

CREATE FUNCTION pure_builtin(p_value NUMBER) RETURN NUMBER IS
BEGIN
  RETURN ABS(p_value);
END;
/

CREATE FUNCTION delegated_value(p_value NUMBER) RETURN NUMBER IS
BEGIN
  RETURN normalize_value(p_value);
END;
/

CREATE FUNCTION unknown_member_value RETURN NUMBER IS
BEGIN
  RETURN package_state.current_value;
END;
/

CREATE FUNCTION nested_local_value RETURN NUMBER IS -- Noncompliant
  FUNCTION helper RETURN DATE IS
  BEGIN
    RETURN SYSDATE;
  END;
BEGIN
  RETURN 1;
END;
/

CREATE FUNCTION nested_local_call RETURN NUMBER IS
  FUNCTION helper RETURN NUMBER IS
  BEGIN
    RETURN 1;
  END;
BEGIN
  RETURN helper();
END;
/

CREATE FUNCTION exception_value RETURN NUMBER IS
BEGIN
  RAISE;
  RETURN 1;
EXCEPTION
  WHEN OTHERS THEN
    RETURN 1;
END;
/

CREATE FUNCTION sys_dual_value RETURN NUMBER IS
  v_value NUMBER;
BEGIN
  SELECT 1 INTO v_value FROM sys.dual;
  RETURN v_value;
END;
/

CREATE FUNCTION chained_cte_value RETURN NUMBER IS
  v_value NUMBER;
BEGIN
  WITH first_cte AS (
    SELECT 1 AS value FROM dual
  ), second_cte AS (
    SELECT value FROM first_cte
  )
  SELECT value INTO v_value FROM second_cte;
  RETURN v_value;
END;
/

CREATE FUNCTION cte_table_value RETURN NUMBER IS
  v_value NUMBER;
BEGIN
  WITH values_cte AS (
    SELECT value FROM test_table
  )
  SELECT value INTO v_value FROM values_cte;
  RETURN v_value;
END;
/

CREATE FUNCTION sequence_value RETURN NUMBER IS
BEGIN
  RETURN sequence_test.NEXTVAL;
END;
/

CREATE FUNCTION currval_value RETURN NUMBER IS
BEGIN
  RETURN sequence_test.CURRVAL;
END;
/

CREATE FUNCTION random_value RETURN NUMBER IS
BEGIN
  RETURN DBMS_RANDOM.VALUE;
END;
/

CREATE FUNCTION generated_value RETURN RAW IS
BEGIN
  RETURN SYS_GUID();
END;
/

CREATE FUNCTION context_value RETURN VARCHAR2 IS
BEGIN
  RETURN SYS_CONTEXT('USERENV', 'SESSION_USER');
END;
/

CREATE FUNCTION insert_candidate RETURN NUMBER IS
BEGIN
  INSERT INTO test_table (id) VALUES (1);
  RETURN 1;
END;
/

CREATE FUNCTION delete_candidate RETURN NUMBER IS
BEGIN
  DELETE FROM test_table WHERE id = 1;
  RETURN 1;
END;
/

CREATE FUNCTION merge_candidate RETURN NUMBER IS
BEGIN
  MERGE INTO test_table target
  USING source_table source
  ON (target.id = source.id)
  WHEN MATCHED THEN UPDATE SET target.value = source.value;
  RETURN 1;
END;
/

CREATE FUNCTION commit_candidate RETURN NUMBER IS
BEGIN
  COMMIT;
  RETURN 1;
END;
/

CREATE FUNCTION rollback_candidate RETURN NUMBER IS
BEGIN
  ROLLBACK;
  RETURN 1;
END;
/

CREATE FUNCTION dynamic_sql_candidate RETURN NUMBER IS
  v_value NUMBER;
BEGIN
  EXECUTE IMMEDIATE 'SELECT 1 FROM dual' INTO v_value;
  RETURN v_value;
END;
/

CREATE FUNCTION out_candidate(p_value OUT NUMBER) RETURN NUMBER IS
BEGIN
  RETURN 1;
END;
/

CREATE FUNCTION in_out_candidate(p_value IN OUT NUMBER) RETURN NUMBER IS
BEGIN
  RETURN 1;
END;
/

CREATE FUNCTION default_input_candidate(p_value NUMBER DEFAULT 1) RETURN NUMBER -- Noncompliant
IS
BEGIN
  RETURN p_value * 2;
END;
/

CREATE FUNCTION environment_default_candidate(p_value DATE DEFAULT SYSDATE) RETURN DATE
IS
BEGIN
  RETURN p_value;
END;
/

CREATE FUNCTION pipelined_candidate RETURN NUMBER PIPELINED IS
BEGIN
  RETURN 1;
END;
/

CREATE FUNCTION current_value RETURN DATE IS
BEGIN
  RETURN SYSDATE;
END;
/

CREATE FUNCTION partial_return(n INTEGER) RETURN INTEGER IS
BEGIN
  IF n = 0 THEN
    RETURN 1;
  ELSIF n = 1 THEN
    RETURN n;
  END IF;
END;
/

CREATE FUNCTION case_without_else(p_value NUMBER) RETURN NUMBER IS
  v_value NUMBER := 0;
BEGIN
  CASE
    WHEN p_value = 1 THEN
      v_value := 1;
  END CASE;
  RETURN v_value;
END;
/

CREATE FUNCTION classify_character(p_value VARCHAR2) RETURN NUMBER IS
BEGIN
  IF p_value = 'A' THEN
    RETURN 1;
  ELSE
    RETURN 0;
  END IF;
END;
/

CREATE FUNCTION classify_character_order(p_value VARCHAR2) RETURN NUMBER IS
BEGIN
  IF p_value < 'A' THEN
    RETURN 1;
  ELSE
    RETURN 0;
  END IF;
END;
/

CREATE FUNCTION starts_with_a(p_value VARCHAR2) RETURN NUMBER IS
BEGIN
  IF p_value LIKE 'A%' THEN
    RETURN 1;
  ELSE
    RETURN 0;
  END IF;
END;
/

CREATE FUNCTION simple_case_value(p_value NUMBER) RETURN NUMBER IS
BEGIN
  CASE p_value
    WHEN 1 THEN
      RETURN 1;
    ELSE
      RETURN 0;
  END CASE;
END;
/

CREATE FUNCTION reciprocal(p_value NUMBER) RETURN NUMBER IS
BEGIN
  RETURN 1 / p_value;
END;
/

CREATE FUNCTION numeric_value(p_value NUMBER) RETURN NUMBER -- Noncompliant
IS
BEGIN
  RETURN p_value * 2;
END;
/

CREATE FUNCTION text_value(p_value NUMBER) RETURN VARCHAR2
IS
BEGIN
  RETURN 'value=' || p_value;
END;
/

CREATE FUNCTION numeric_from_text(p_value VARCHAR2) RETURN NUMBER
IS
BEGIN
  RETURN p_value + 1;
END;
/

CREATE FUNCTION text_from_number(p_value NUMBER) RETURN VARCHAR2
IS
BEGIN
  RETURN p_value;
END;
/

CREATE FUNCTION database_value RETURN NUMBER IS
  v_value NUMBER;
BEGIN
  SELECT value INTO v_value FROM configuration;
  RETURN v_value;
END;
/

CREATE FUNCTION out_value(p_value OUT NUMBER) RETURN NUMBER IS
BEGIN
  RETURN 1;
END;
/

CREATE FUNCTION deterministic_value(p_value NUMBER) RETURN NUMBER DETERMINISTIC IS
BEGIN
  RETURN p_value * 2;
END;
/

CREATE FUNCTION update_value(p_value NUMBER) RETURN NUMBER IS
BEGIN
  UPDATE configuration SET value = p_value;
  RETURN p_value;
END;
/

CREATE FUNCTION raise_value RETURN NUMBER IS
BEGIN
  RAISE_APPLICATION_ERROR(-20001, 'invalid');
  RETURN 1;
END;
/
