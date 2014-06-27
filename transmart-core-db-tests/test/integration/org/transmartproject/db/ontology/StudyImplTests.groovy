package org.transmartproject.db.ontology

import grails.test.mixin.TestMixin
import org.junit.Before
import org.junit.Test
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.test.RuleBasedIntegrationTestMixin

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.is

@TestMixin(RuleBasedIntegrationTestMixin)
class StudyImplTests {

    StudyTestData studyTestData = new StudyTestData()

    StudiesResource studiesResourceService

    @Before
    void before() {
        studyTestData.saveAll()
    }

    @Test
    void testStudyGetAllPatients() {
        Study study = studiesResourceService.getStudyById('study_id_1')

        assertThat study.patients, containsInAnyOrder(studyTestData.i2b2Data.patients.collect { is it })
    }

    @Test
    void testStudyGetName() {
        Study study = studiesResourceService.getStudyById('study_id_1')

        assertThat study.id, is('STUDY_ID_1' /* term name in uppercase */)
    }

}
