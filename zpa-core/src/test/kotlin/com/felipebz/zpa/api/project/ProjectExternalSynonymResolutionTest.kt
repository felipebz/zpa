//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectExternalTypeDescription.Status
import com.felipebz.zpa.api.project.ProjectTypeReference.Anchor.ROWTYPE
import com.felipebz.zpa.api.project.ProjectTypeReference.Anchor.TYPE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.project.ProjectExternalLookup.Status as Lookup

/** Unqualified relation names of anchors read through private and public synonyms, as Oracle resolves them. */
@OptIn(ZpaExperimentalApi::class)
class ProjectExternalSynonymResolutionTest {
    private val app = ProjectIdentifier.fromSource("APP_B")
    private val empty = ProjectSnapshots.prepare(emptyList(), false)

    private fun name(type: String, length: Int? = null) = ProjectExternalColumn.of("NAME", 1, ProjectExternalDatatype.of(type, null, length, length?.let { ProjectExternalDatatype.LengthSemantics.BYTE }, null, null))
    private val coreCustomer = ProjectExternalRelation.of("CORE_A", "CUSTOMER", true, listOf(name("VARCHAR2", 60)))
    private val otherCustomer = ProjectExternalRelation.of("OTHER_C", "CUSTOMER", true, listOf(name("DATE")))
    private val localCustomer = ProjectExternalRelation.of("APP_B", "CUSTOMER", true, listOf(name("NUMBER")))
    private val coreView = ProjectExternalRelation.view("CORE_A", "V_CUST", true, listOf(name("VARCHAR2", 60)))

    private fun synonym(owner: String, name: String, targetOwner: String?, target: String, link: String? = null) =
        ProjectExternalSynonym.of(owner, name, targetOwner, target, link)

    private fun metadata(
        complete: List<String> = listOf("APP_B", "CORE_A", "OTHER_C"),
        publicComplete: Boolean = true,
        relations: List<ProjectExternalRelation> = listOf(coreCustomer, otherCustomer),
        vararg synonyms: ProjectExternalSynonym
    ) = ProjectExternalMetadata.builder("fixture").apply {
        complete.forEach { completeSchema(it) }
        if (publicComplete) completePublicSynonyms()
        relations.forEach { relation(it) }
        synonyms.forEach { synonym(it) }
    }.build()

    private fun describe(md: ProjectExternalMetadata, vararg segments: String, kind: ProjectTypeReference.Anchor = TYPE) =
        empty.describeExternalType(ProjectWrittenAnchor.of(ProjectQualifiedName.of(*segments), kind), null, md, app)

    private fun type(d: ProjectExternalTypeDescription) = d.lookup.get().column.get().datatype.get().name
    private fun chain(d: ProjectExternalTypeDescription) = d.synonyms.map { s ->
        "${s.owner.identifier.lookupName}.${s.name.identifier.lookupName}->${s.targetOwner.get().identifier.lookupName}.${s.targetName.identifier.lookupName}"
    }

    private val privateSynonym = synonym("APP_B", "CUSTOMER", "CORE_A", "CUSTOMER")
    private val publicSynonym = synonym("PUBLIC", "CUSTOMER", "OTHER_C", "CUSTOMER")

    @Test
    fun aPrivateSynonymComesBeforeAPublicOneAndAnObjectBeforeBoth() {
        val viaPrivate = describe(metadata(synonyms = arrayOf(privateSynonym)), "customer", "name")
        assertThat(viaPrivate.status).isEqualTo(Status.RESOLVED)
        assertThat(viaPrivate.relationName).contains(ProjectQualifiedName.of("CORE_A", "CUSTOMER"))
        assertThat(type(viaPrivate)).isEqualTo("VARCHAR2")
        assertThat(chain(viaPrivate)).containsExactly("APP_B.CUSTOMER->CORE_A.CUSTOMER")

        val viaPublic = describe(metadata(synonyms = arrayOf(publicSynonym)), "customer", "name")
        assertThat(type(viaPublic)).isEqualTo("DATE")
        assertThat(viaPublic.synonyms.single().isPublic).isTrue()

        assertThat(type(describe(metadata(synonyms = arrayOf(privateSynonym, publicSynonym)), "customer", "name"))).isEqualTo("VARCHAR2")
        // A table of the compilation schema is read before a public synonym (a private synonym cannot share its name).
        val local = describe(metadata(relations = listOf(coreCustomer, otherCustomer, localCustomer), synonyms = arrayOf(publicSynonym)), "customer", "name")
        assertThat(type(local)).isEqualTo("NUMBER")
        assertThat(local.synonyms).isEmpty()
    }

    @Test
    fun synonymsOfSynonymsAreFollowedUntilARelation() {
        val alias = synonym("APP_B", "CUST_ALIAS", "APP_B", "CUSTOMER")
        val twice = describe(metadata(synonyms = arrayOf(alias, privateSynonym)), "cust_alias", "name")
        assertThat(twice.status).isEqualTo(Status.RESOLVED)
        assertThat(chain(twice)).containsExactly("APP_B.CUST_ALIAS->APP_B.CUSTOMER", "APP_B.CUSTOMER->CORE_A.CUSTOMER")
        // A private synonym may stand for a public one.
        val toPublic = describe(metadata(synonyms = arrayOf(synonym("APP_B", "PUB_ALIAS", "PUBLIC", "CUSTOMER"), publicSynonym)), "pub_alias", "name")
        assertThat(type(toPublic)).isEqualTo("DATE")
        // %ROWTYPE through a private synonym for a view.
        val row = describe(metadata(relations = listOf(coreView), synonyms = arrayOf(synonym("APP_B", "VS", "CORE_A", "V_CUST"))), "vs", kind = ROWTYPE)
        assertThat(row.status).isEqualTo(Status.RESOLVED)
        assertThat(row.lookup.get().relation.get().isView).isTrue()
    }

    @Test
    fun aSynonymThatCannotReachARelationIsNotOne() {
        // The target is not in the metadata (dropped, or not visible): Oracle fails with PLS-00201 when it is dropped,
        // but the session may also not see it, so nothing is claimed.
        val dangling = describe(metadata(synonyms = arrayOf(synonym("APP_B", "GONE_SYN", "CORE_A", "GONE"))), "gone_syn", "name")
        assertThat(dangling.status).isEqualTo(Status.INCOMPLETE)
        assertThat(dangling.lookup.get().status).isEqualTo(Lookup.NOT_COVERED)
        assertThat(dangling.relationName).contains(ProjectQualifiedName.of("CORE_A", "GONE"))
        assertThat(dangling.synonyms).hasSize(1)
        // Oracle rejects a synonym that resolves to itself (ORA-01775).
        val loop = describe(metadata(synonyms = arrayOf(synonym("APP_B", "LOOP_A", "APP_B", "LOOP_B"), synonym("APP_B", "LOOP_B", "APP_B", "LOOP_A"))), "loop_a", "name")
        assertThat(loop.status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(loop.synonyms).hasSize(2)
        // A synonym over a database link is remote.
        val remote = describe(metadata(synonyms = arrayOf(synonym("APP_B", "REMOTE_C", null, "CUSTOMER", "NOWHERE_LINK"))), "remote_c", "name")
        assertThat(remote.status).isEqualTo(Status.REMOTE)
        assertThat(remote.synonyms.single().dbLink).contains("NOWHERE_LINK")
        // A synonym for a package is not a relation.
        val pkgMetadata = ProjectExternalMetadata.builder("fixture").completeSchema("APP_B").completeSchema("CORE_A").completePublicSynonyms()
            .schemaObject(ProjectExternalObject.of("CORE_A", "PK_UTIL", ProjectExternalObject.Kind.PACKAGE))
            .synonym(synonym("APP_B", "UTIL", "CORE_A", "PK_UTIL")).build()
        val pkg = describe(pkgMetadata, "util", "name")
        assertThat(pkg.status).isEqualTo(Status.NOT_EXTERNAL)
        assertThat(pkg.capturingObject.get().kind).isEqualTo(ProjectExternalObject.Kind.PACKAGE)
    }

    @Test
    fun absenceNeedsTheCompilationSchemaAndThePublicSynonymsComplete() {
        assertThat(describe(metadata(), "customer", "name").status).isEqualTo(Status.RELATION_NOT_FOUND)
        // A public synonym the metadata does not hold may take the name.
        val publicOpen = describe(metadata(publicComplete = false), "customer", "name")
        assertThat(publicOpen.status).isEqualTo(Status.INCOMPLETE)
        assertThat(publicOpen.lookup.get().status).isEqualTo(Lookup.NOT_COVERED)
        // An object of the compilation schema the metadata does not hold would come before the public synonym.
        val schemaOpen = describe(metadata(complete = listOf("CORE_A", "OTHER_C"), synonyms = arrayOf(publicSynonym)), "customer", "name")
        assertThat(schemaOpen.status).isEqualTo(Status.INCOMPLETE)
        assertThat(schemaOpen.synonyms).isEmpty()
        // A private synonym supplied for a schema that is not complete is still the schema's object.
        assertThat(describe(metadata(complete = listOf("CORE_A"), synonyms = arrayOf(privateSynonym)), "customer", "name").status).isEqualTo(Status.RESOLVED)
    }

    @Test
    fun metadataKeepsObjectsAndSynonymsWithTheirCoverage() {
        val md = metadata(relations = listOf(coreCustomer, coreView), synonyms = arrayOf(privateSynonym))
        fun id(s: String) = ProjectIdentifier.fromSource("\"$s\"")
        assertThat(md.findObject(id("CORE_A"), id("CUSTOMER")).get().kind).isEqualTo(ProjectExternalObject.Kind.TABLE)
        assertThat(md.findObject(id("CORE_A"), id("V_CUST")).get().kind).isEqualTo(ProjectExternalObject.Kind.VIEW)
        assertThat(md.findObject(id("APP_B"), id("CUSTOMER")).get().kind).isEqualTo(ProjectExternalObject.Kind.SYNONYM)
        assertThat(md.findObject(id("APP_B"), id("NOPE"))).isEmpty
        assertThat(md.isSchemaComplete(id("APP_B"))).isTrue()
        assertThat(md.isPublicSynonymsComplete).isTrue()
        assertThat(md.findSynonym(id("APP_B"), id("CUSTOMER"))).contains(privateSynonym)
        assertThat(ProjectExternalMetadata.none().isPublicSynonymsComplete).isFalse()
        val builder = ProjectExternalMetadata.builder("fixture").schemaObject(ProjectExternalObject.of("A", "X", ProjectExternalObject.Kind.PACKAGE))
        builder.schemaObject(ProjectExternalObject.of("A", "X", ProjectExternalObject.Kind.PACKAGE))
        assertThatThrownBy { builder.schemaObject(ProjectExternalObject.of("A", "X", ProjectExternalObject.Kind.TYPE)) }.isInstanceOf(IllegalArgumentException::class.java)
        builder.synonym(privateSynonym)
        assertThatThrownBy { builder.synonym(privateSynonym) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { synonym("APP_B", "X", null, "T") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(synonym("APP_B", "X", null, "T", "LNK").targetOwner).isEmpty
    }
}
