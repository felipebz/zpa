package com.felipebz.zpa.oracle.metadata;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** The request API as Java callers see it. */
class OracleMetadataJavaApiTest {
    @Test
    void buildsARequestFromJava() {
        OracleMetadataRequest request = OracleMetadataRequest.builder()
            .schema("HR")
            .relation("OTHER_SCHEMA", "CUSTOMERS")
            .queryTimeout(Duration.ofSeconds(30))
            .build();
        assertThat(request.getSchemas()).containsExactly("HR");
        assertThat(request.getRelations()).containsExactly(OracleRelationName.of("OTHER_SCHEMA", "CUSTOMERS"));
        assertThat(request.getQueryTimeout()).isEqualTo(Duration.ofSeconds(30));
    }
}
