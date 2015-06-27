package br.com.felipezorzo.sonar.plsql.api;

import org.sonar.squidbridge.measures.CalculatedMetricFormula;
import org.sonar.squidbridge.measures.MetricDef;

public enum PlSqlMetric implements MetricDef {
    FILES;
    
    @Override
    public String getName() {
      return name();
    }

    @Override
    public boolean isCalculatedMetric() {
      return false;
    }

    @Override
    public boolean aggregateIfThereIsAlreadyAValue() {
      return true;
    }

    @Override
    public boolean isThereAggregationFormula() {
      return true;
    }

    @Override
    public CalculatedMetricFormula getCalculatedMetricFormula() {
      return null;
    }
}
