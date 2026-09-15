declare
    plain number;
    numeric_precision number(5, 2);
    constant_number constant number := 1;
    default_text varchar2(10) default 'x';
    not_null_number number not null := 1;
    nullable_number number null default 1;
    custom_type_variable custom_type;
    package_type package_name.custom_type;
    column_type table_name.column_name%type;
    row_type table_name%rowtype;
    ref_type ref custom_type;
    "MyVariable" number;
begin
    null;
end;
