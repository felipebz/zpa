begin
    if a = 1 then
        first_call;
    end if;

    if b = 1 then
        first_branch_call;
    elsif b = 2 then
        second_branch_call;
    elsif b = 3 then
        third_branch_call;
    else
        fourth_branch_call;
    end if;

    if outer_condition then
        if inner_condition then
            null;
        end if;
    end if;

    <<labeled_if>>
    if labeled_condition then
        null;
    end if labeled_if;
end;
