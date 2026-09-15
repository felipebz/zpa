declare
    local_value number;
begin
    local_value := function_call();
    select value into local_value from table_name;
end;
