/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import org.sonar.api.server.rule.RulesDefinition.NewParam;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.api.server.rule.RulesDefinition.NewRule;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.squidbridge.annotations.RuleTemplate;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleLinearRemediation;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.rules.ExternalDescriptionLoader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class CustomAnnotationBasedRulesDefinition {

    private final NewRepository repository;
    private final String languageKey;
    private final ExternalDescriptionLoader externalDescriptionLoader;
    private final Locale locale;

    public CustomAnnotationBasedRulesDefinition(NewRepository repository, String languageKey) {
        this.repository = repository;
        this.languageKey = languageKey;
        this.locale = Locale.getDefault();
        String externalDescriptionBasePath = getLocalizedFolderName(String.format("/org/sonar/l10n/%s", languageKey), locale);
        externalDescriptionBasePath = String.format("%s/rules/%s", externalDescriptionBasePath, repository.key());
        this.externalDescriptionLoader = new ExternalDescriptionLoader(repository, externalDescriptionBasePath);
    }
    
    /**
     * Adds annotated rule classes to an instance of NewRepository. Fails if one
     * the classes has no SQALE annotation.
     */
    @SuppressWarnings("rawtypes")
    public static void load(NewRepository repository, String languageKey, Iterable<Class> ruleClasses) {
        new CustomAnnotationBasedRulesDefinition(repository, languageKey).addRuleClasses(true, ruleClasses);
    }

    @SuppressWarnings("rawtypes")
    public void addRuleClasses(Iterable<Class> ruleClasses) {
        addRuleClasses(true, ruleClasses);
    }

    @SuppressWarnings("rawtypes")
    public void addRuleClasses(boolean failIfNoExplicitKey, Iterable<Class> ruleClasses) {
        new RulesDefinitionAnnotationLoader().load(repository, Iterables.toArray(ruleClasses, Class.class));
        List<NewRule> newRules = new ArrayList<>();
        for (Class<?> ruleClass : ruleClasses) {
            NewRule rule = newRule(ruleClass, failIfNoExplicitKey);
            externalDescriptionLoader.addHtmlDescription(rule);
            rule.setTemplate(AnnotationUtils.getAnnotation(ruleClass, RuleTemplate.class) != null);
            try {
                setupSqaleModel(rule, ruleClass);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Could not setup SQALE model on " + ruleClass, e);
            }
            newRules.add(rule);
        }
        setupExternalNames(newRules);
    }

    @VisibleForTesting
    NewRule newRule(Class<?> ruleClass, boolean failIfNoExplicitKey) {
        org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(ruleClass, org.sonar.check.Rule.class);
        if (ruleAnnotation == null) {
            throw new IllegalArgumentException("No Rule annotation was found on " + ruleClass);
        }
        String ruleKey = ruleAnnotation.key();
        if (ruleKey.isEmpty()) {
            if (failIfNoExplicitKey) {
                throw new IllegalArgumentException("No key is defined in Rule annotation of " + ruleClass);
            }
            ruleKey = ruleClass.getCanonicalName();
        }
        return repository.rule(ruleKey);
    }

    private void setupExternalNames(Collection<NewRule> rules) {
        ResourceBundle bundle = ResourceBundle.getBundle("org.sonar.l10n." + languageKey, locale);
        for (NewRule rule : rules) {
            String baseKey = "rule." + repository.key() + "." + rule.key();
            String nameKey = baseKey + ".name";
            if (bundle.containsKey(nameKey)) {
                rule.setName(bundle.getString(nameKey));
            }
            for (NewParam param : rule.params()) {
                String paramDescriptionKey = baseKey + ".param." + param.key();
                if (bundle.containsKey(paramDescriptionKey)) {
                    param.setDescription(bundle.getString(paramDescriptionKey));
                }
            }
        }
    }

    private static void setupSqaleModel(NewRule rule, Class<?> ruleClass) {
        SqaleConstantRemediation constant = AnnotationUtils.getAnnotation(ruleClass, SqaleConstantRemediation.class);
        SqaleLinearRemediation linear = AnnotationUtils.getAnnotation(ruleClass, SqaleLinearRemediation.class);
        SqaleLinearWithOffsetRemediation linearWithOffset = AnnotationUtils.getAnnotation(ruleClass,
                SqaleLinearWithOffsetRemediation.class);

        Set<Annotation> remediations = Sets.newHashSet(constant, linear, linearWithOffset);
        if (Iterables.size(Iterables.filter(remediations, Predicates.notNull())) > 1) {
            throw new IllegalArgumentException("Found more than one SQALE remediation annotations on " + ruleClass);
        }

        if (constant != null) {
            rule.setDebtRemediationFunction(rule.debtRemediationFunctions().constantPerIssue(constant.value()));
        }
        if (linear != null) {
            rule.setDebtRemediationFunction(rule.debtRemediationFunctions().linear(linear.coeff()));
            rule.setGapDescription(linear.effortToFixDescription());
        }
        if (linearWithOffset != null) {
            rule.setDebtRemediationFunction(rule.debtRemediationFunctions().linearWithOffset(linearWithOffset.coeff(),
                    linearWithOffset.offset()));
            rule.setGapDescription(linearWithOffset.effortToFixDescription());
        }
    }

    public static String getLocalizedFolderName(String baseName, Locale locale) {
        ResourceBundle.Control control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);
        
        String path = control.toBundleName(baseName, locale);
        URL url = CustomAnnotationBasedRulesDefinition.class.getResource(path);
        
        if (url == null) {
            Locale localeWithoutCountry = locale.getCountry() == null ? locale : new Locale(locale.getLanguage());
            path = control.toBundleName(baseName, localeWithoutCountry);
            url = CustomAnnotationBasedRulesDefinition.class.getResource(path);
            
            if (url == null) {
                path = baseName;
                CustomAnnotationBasedRulesDefinition.class.getResource(path);
            }
        }
        
        return path;
    }

}
