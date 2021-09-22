begin
  ---------------
  -- ALERT
  ---------------
  find_alert(var); -- ignore variables
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


  ---------------
  -- BLOCK
  ---------------
  find_block('foo');
  find_block('invalid'); -- Noncompliant
--           ^^^^^^^^^

  get_block_property('foo', order_by);
  get_block_property('invalid', order_by); -- Noncompliant
--                   ^^^^^^^^^

  go_block('foo');
  go_block('invalid'); -- Noncompliant
--         ^^^^^^^^^

  set_block_property('foo', order_by, 'col');
  set_block_property('invalid', order_by, 'col'); -- Noncompliant
--                   ^^^^^^^^^

  set_block_property('foo', blockscrollbar_position, x, y);
  set_block_property('invalid', blockscrollbar_position, x, y); -- Noncompliant
--                   ^^^^^^^^^


  ---------------
  -- ITEM
  ---------------
  checkbox_checked('foo.item1');
  checkbox_checked('foo.invalid'); -- Noncompliant
--                 ^^^^^^^^^^^^^
  
  convert_other_value('foo.item1');
  convert_other_value('foo.invalid'); -- Noncompliant
--                    ^^^^^^^^^^^^^

  display_item('foo.item1', 'bar');
  display_item('foo.invalid', 'bar'); -- Noncompliant
--             ^^^^^^^^^^^^^

  find_item('foo.item1');
  find_item('foo.invalid'); -- Noncompliant
--          ^^^^^^^^^^^^^

  get_item_instance_property('foo.item1', current_record, visual_attribute);
  get_item_instance_property('foo.invalid', current_record, visual_attribute); -- Noncompliant
--                           ^^^^^^^^^^^^^

  get_item_property('foo.item1', enabled);
  get_item_property('foo.invalid', enabled); -- Noncompliant
--                  ^^^^^^^^^^^^^

  get_radio_button_property('foo.item1', 'bar', label);
  get_radio_button_property('foo.invalid', 'bar', label); -- Noncompliant
--                          ^^^^^^^^^^^^^

  go_item('foo.item1');
  go_item('foo.invalid'); -- Noncompliant
--        ^^^^^^^^^^^^^

  image_scroll('foo.item1', x, y);
  image_scroll('foo.invalid', x, y); -- Noncompliant
--             ^^^^^^^^^^^^^

  image_zoom('foo.item1', adjust_to_fit);
  image_zoom('foo.invalid', adjust_to_fit); -- Noncompliant
--           ^^^^^^^^^^^^^

  image_zoom('foo.item1', zoom_in_factor, 10);
  image_zoom('foo.invalid', zoom_in_factor, 10); -- Noncompliant
--           ^^^^^^^^^^^^^

  play_sound('foo.item1');
  play_sound('foo.invalid'); -- Noncompliant
--           ^^^^^^^^^^^^^

  read_image_file('file.jpg', 'jpg', 'foo.item1');
  read_image_file('file.jpg', 'jpg', 'foo.invalid'); -- Noncompliant
--                                   ^^^^^^^^^^^^^

  read_sound_file('file.wav', 'wave', 'foo.item1');
  read_sound_file('file.wav', 'wave', 'foo.invalid'); -- Noncompliant
--                                    ^^^^^^^^^^^^^

  recalculate('foo.item1');
  recalculate('foo.invalid'); -- Noncompliant
--            ^^^^^^^^^^^^^

  set_item_instance_property('foo.item1', currrent_record, visual_attribute, 'bar');
  set_item_instance_property('foo.invalid', currrent_record, visual_attribute, 'bar'); -- Noncompliant
--                           ^^^^^^^^^^^^^

  set_item_property('foo.item1', enabled, property_false);
  set_item_property('foo.invalid', enabled, property_false); -- Noncompliant
--                  ^^^^^^^^^^^^^

  set_item_property('foo.item1', position, x, y);
  set_item_property('foo.invalid', position, x, y); -- Noncompliant
--                  ^^^^^^^^^^^^^

  set_radio_button_property('foo.item1', 'bar', enabled, property_false);
  set_radio_button_property('foo.invalid', 'bar', enabled, property_false); -- Noncompliant
--                          ^^^^^^^^^^^^^

  set_radio_button_property('foo.item1', 'bar', position, x, y);
  set_radio_button_property('foo.invalid', 'bar', position, x, y); -- Noncompliant
--                          ^^^^^^^^^^^^^

  write_image_file('file.jpg', 'jpg', 'foo.item1', maximize_compression, original_depth);
  write_image_file('file.jpg', 'jpg', 'foo.invalid', maximize_compression, original_depth); -- Noncompliant
--                                    ^^^^^^^^^^^^^

  write_sound_file('file.wav', 'wave', 'foo.item1', original_quality, original_setting);
  write_sound_file('file.wav', 'wave', 'foo.invalid', original_quality, original_setting); -- Noncompliant
--                                     ^^^^^^^^^^^^^

  ---------------
  -- LOV
  ---------------
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