declare
  var tab%rowtype;

  subtype my_type is tab%rowtype;
  v_my_type my_type;

  type t_tab is table of tab%rowtype index by binary_integer;
  v_tab t_tab;
begin
  insert into tab values (1); -- Noncompliant {{Specify the columns in this INSERT.}}
  
  insert into tab (col) values (1);

  insert into tab values var;

  insert into tab values var returning id into v_id;

  for r_t in (select * from mytable) loop
      insert into mytable values r_t;
  end loop;

  forall idx in v_tab.first .. v_tab.last
  insert into tab values v_tab(idx);

  insert into tab values v_my_type;

end;


DECLARE
sales_q1 NUMBER;
  sales_q2 NUMBER;
BEGIN
SELECT Q1, Q2
INTO sales_q1, sales_q2
FROM (SELECT quarter, amount
      FROM sales_table) PIVOT (
                               SUM(amount) FOR quarter IN ('Q1', 'Q2')
  );

SELECT *
FROM sales
       PIVOT (
              SUM(quantity_sold) AS qty
  FOR region IN ('North' AS north, 'South' AS south)
    );

SELECT *
FROM sales UNPIVOT (
                    amount FOR quarter IN (Q1,Q2,Q3)
  );

SELECT *
FROM sales UNPIVOT
  INCLUDE NULLS (
  amount FOR quarter
IN
(
Q1, Q2, Q3
)
);


SELECT *
FROM sales UNPIVOT exclude nulls (
                         amount FOR quarter IN (Q1,Q2,Q3)
    );

SELECT *
FROM (SELECT *
      FROM (SELECT region, quarter, year, amount
            FROM sales) PIVOT (
                               SUM(amount) FOR quarter IN ('Q1' AS Q1, 'Q2' AS Q2)
        )) pivot PIVOT (
                  MAX(Q1) FOR year IN (2024 AS Y2024, 2025 AS Y2025)
  );


SELECT Q1, Q2
INTO sales_q1, sales_q2
FROM (SELECT quarter, amount
      FROM sales_table) PIVOT (
                               MAX(amount) FOR (quarter) IN ('Q1' AS Q1, 'Q2' AS Q2)
  );

SELECT Q1, Q2
INTO sales_q1, sales_q2
FROM (SELECT quarter, amount
      FROM sales_table) PIVOT (
                               MAX(amount) as 'TEST' FOR quarter IN ('Q1' Q1, 'Q2' AS Q2)
  );
END;
/



BEGIN
FOR rec IN (
  SELECT * FROM (
    SELECT region, quarter, revenue FROM sales_table
  )
  PIVOT (
    SUM(revenue) FOR quarter IN ('Q1' AS Q1, 'Q2' AS Q2)
  )
) LOOP
  DBMS_OUTPUT.PUT_LINE(rec.region || ': ' || rec.Q1 || ', ' || rec.Q2);
END LOOP;
END;
/

INSERT INTO quarterly_summary(region, q1, q2)
SELECT *
FROM (SELECT region, quarter, revenue
      FROM sales_table) PIVOT (
                               SUM(revenue) FOR quarter IN ('Q1' AS Q1, 'Q2' AS Q2)
  );


BEGIN
FOR rec IN (
    WITH pivoted_data AS (
      SELECT * FROM (
        SELECT region, quarter, revenue FROM sales_table
      )
      PIVOT (
        SUM(revenue) FOR quarter IN ('Q1' AS Q1, 'Q2' AS Q2)
      )
    )
    SELECT * FROM pivoted_data
  ) LOOP
    NULL; -- Do something with rec
END LOOP;
END;


FUNCTION
get_pivoted_sales RETURN SYS_REFCURSOR IS
  rc SYS_REFCURSOR;
BEGIN
OPEN rc FOR
SELECT *
FROM (SELECT region, quarter, revenue
      FROM sales_table) PIVOT (
                               SUM(revenue) FOR quarter IN ('Q1' AS Q1, 'Q2' AS Q2)
  );
RETURN rc;
END;


DECLARE
  -- Define a record type matching the PIVOT result
CURSOR c_pivoted_sales IS
SELECT *
FROM (SELECT region, quarter, amount
      FROM (SELECT 'East' AS region, 'Q1' AS quarter, 100 AS amount
            FROM dual
            UNION ALL
            SELECT 'East', 'Q2', 150
            FROM dual
            UNION ALL
            SELECT 'West', 'Q1', 200
            FROM dual
            UNION ALL
            SELECT 'West', 'Q2', 300
            FROM dual)) PIVOT (
                               SUM(amount) FOR quarter IN ('Q1' AS Q1, 'Q2' AS Q2)
  );

-- Variables to hold each column
v_region
VARCHAR2(20);
  v_q1
NUMBER;
  v_q2
NUMBER;
BEGIN
  -- Open and loop through the cursor
FOR rec IN c_pivoted_sales LOOP
    v_region := rec.region;
    v_q1
:= rec.Q1;
    v_q2
:= rec.Q2;

    -- Output or process the results
    DBMS_OUTPUT.PUT_LINE
('Region: ' || v_region || ', Q1: ' || v_q1 || ', Q2: ' || v_q2);
END LOOP;
END;
