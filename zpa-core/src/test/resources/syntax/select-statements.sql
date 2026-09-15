begin
    select value
      into result
      from table_a;

    select value
      bulk collect into results
      from table_a;

    select a, b
      into first_value, second_value
      from table_a
     where active = 1;

    select value
      into result
      from table_a
    union
    select value
      from table_b;

    select outer_col
      into result
      from (
          select inner_col
            from inner_table
      );

    with cte as (
        select inner_col
          from inner_table
    )
    select outer_col
      into result
      from cte;

    (select value from table_a union select value from table_b);

    <<query_label>>
    select value
      into result
      from table_a;
end;
