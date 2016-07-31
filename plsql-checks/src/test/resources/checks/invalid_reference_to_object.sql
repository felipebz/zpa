begin
  find_alert('foo');
  find_alert('invalid'); -- Noncompliant {{Invalid reference to the object "invalid" in this FIND_ALERT call.}}
--           ^^^^^^^^^

  set_alert_button_property('foo', alert_button1, label, 'label');
  set_alert_button_property('invalid', alert_button1, label, 'label'); -- Noncompliant
--                          ^^^^^^^^^

  set_alert_property('foo', title, 'title');
  set_alert_property('invalid', title, 'title'); -- Noncompliant
--                   ^^^^^^^^^

  show_alert('foo');
  show_alert('invalid'); -- Noncompliant
--           ^^^^^^^^^

  find_lov('foo');
  find_lov('invalid'); -- Noncompliant
--         ^^^^^^^^^

  get_lov_property('foo', title);
  get_lov_property('invalid', title); -- Noncompliant
--                 ^^^^^^^^^

  set_lov_column_property('foo', 1, title, 'title');
  set_lov_column_property('invalid', 1, title, 'title'); -- Noncompliant
--                        ^^^^^^^^^

  set_lov_property('foo', title, 'title');
  set_lov_property('invalid', title, 'title'); -- Noncompliant
--                 ^^^^^^^^^

  set_lov_property('foo', position, x, y);
  set_lov_property('invalid', position, x, y); -- Noncompliant
--                 ^^^^^^^^^

  show_lov('foo');
  show_lov('invalid'); -- Noncompliant
--         ^^^^^^^^^
end;