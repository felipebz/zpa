package br.com.felipezorzo.sonar.plsql.toolkit;

import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.colorizer.KeywordsTokenizer;
import org.sonar.colorizer.Tokenizer;
import org.sonar.sslr.toolkit.AbstractConfigurationModel;
import org.sonar.sslr.toolkit.ConfigurationProperty;
import org.sonar.sslr.toolkit.Validators;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

import br.com.felipezorzo.sonar.plsql.PlSqlConfiguration;
import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;
import br.com.felipezorzo.sonar.plsql.parser.PlSqlParser;

public class PlSqlConfigurationModel extends AbstractConfigurationModel {

    private static final Logger LOG = LoggerFactory.getLogger(PlSqlConfigurationModel.class);

    private static final String CHARSET_PROPERTY_KEY = "sonar.sourceEncoding";

    @VisibleForTesting
    ConfigurationProperty charsetProperty = new ConfigurationProperty("Charset",
            CHARSET_PROPERTY_KEY,
            getPropertyOrDefaultValue(CHARSET_PROPERTY_KEY, "UTF-8"),
            Validators.charsetValidator());

    @Override
    public Charset getCharset() {
        return Charset.forName(charsetProperty.getValue());
    }

    @Override
    public List<ConfigurationProperty> getProperties() {
        return ImmutableList.of(charsetProperty);
    }

    @Override
    public Parser<Grammar> doGetParser() {
        return PlSqlParser.create(getConfiguration());
    }

    @Override
    public List<Tokenizer> doGetTokenizers() {
        return ImmutableList.of((Tokenizer) new KeywordsTokenizer("<span class=\"k\">", "</span>", PlSqlKeyword.keywordValues()));
    }

    @VisibleForTesting
    PlSqlConfiguration getConfiguration() {
        return new PlSqlConfiguration(Charset.forName(charsetProperty.getValue()));
    }

    @VisibleForTesting
    static String getPropertyOrDefaultValue(String propertyKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);

        if (propertyValue == null) {
            LOG.info("The property \"" + propertyKey + "\" is not set, using the default value \"" + defaultValue + "\".");
            return defaultValue;
        } else {
            LOG.info("The property \"" + propertyKey + "\" is set, using its value \"" + propertyValue + "\".");
            return propertyValue;
        }
    }

}
