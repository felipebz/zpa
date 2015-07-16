package br.com.felipezorzo.sonar.plsql.toolkit;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Charsets;

public class PlSqlConfigurationModelTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getConfiguration_charset() {
        PlSqlConfigurationModel model = new PlSqlConfigurationModel();
        model.charsetProperty.setValue("UTF-8");
        assertThat(model.getCharset(), is(equalTo(Charsets.UTF_8)));
        assertThat(model.getConfiguration().getCharset(), is(equalTo(Charsets.UTF_8)));
        model.charsetProperty.setValue("ISO-8859-1");
        assertThat(model.getCharset(), is(equalTo(Charsets.ISO_8859_1)));
        assertThat(model.getConfiguration().getCharset(), is(equalTo(Charsets.ISO_8859_1)));
    }

    @Test
    public void getPropertyOrDefaultValue_with_property_set() {
        String oldValue = System.getProperty("foo");

        try {
            System.setProperty("foo", "bar");
            assertThat(PlSqlConfigurationModel.getPropertyOrDefaultValue("foo", "baz"), is(equalTo("bar")));
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo");
            } else {
                System.setProperty("foo", oldValue);
            }
        }
    }

    @Test
    public void getPropertyOrDefaultValue_with_property_not_set() {
        String oldValue = System.getProperty("foo");

        try {
            System.clearProperty("foo");
            assertThat(PlSqlConfigurationModel.getPropertyOrDefaultValue("foo", "baz"), is(equalTo("baz")));
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo");
            } else {
                System.setProperty("foo", oldValue);
            }
        }
    }
}
