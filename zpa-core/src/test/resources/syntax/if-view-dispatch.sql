declare
    local_value number;
begin
    local_value := function_call();
    if local_value = 1 then
        select value into local_value from table_name;
    end if;
end;
