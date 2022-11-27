-- Get the time in ms from 23.10.2015 until this moment

FUNCTION f_get_timer_value RETURN number IS
  out_result number;
begin
        select extract(day from(systimestamp - to_timestamp('2015-10-23', 'YYYY-MM-DD'))) * 86400000 
            + to_number(to_char(sys_extract_utc(systimestamp), 'SSSSSFF3'))
        into out_result
        from dual;
        return out_result;
end f_get_timer_value;
