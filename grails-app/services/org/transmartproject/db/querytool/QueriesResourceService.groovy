package org.transmartproject.db.querytool

import groovy.xml.MarkupBuilder
import org.hibernate.jdbc.Work
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.querytool.ConstraintByValue
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueriesResource
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.core.querytool.QueryResult
import org.transmartproject.core.querytool.QueryStatus

import java.sql.Connection

class QueriesResourceService implements QueriesResource {

    def grailsApplication
    def patientSetQueryBuilderService
    def queryDefinitionXmlService
    def sessionFactory

    @Override
    QueryResult runQuery(QueryDefinition definition) throws InvalidRequestException {

        // 1. Populate qt_query_master
        QtQueryMaster queryMaster = new QtQueryMaster(
            name           : definition.name,
            userId         : grailsApplication.config.org.transmartproject.i2b2.user_id,
            groupId        : grailsApplication.config.org.transmartproject.i2b2.group_id,
            createDate     : new Date(),
            generatedSql   : null,
            requestXml     : queryDefinitionToXml(definition),
            i2b2RequestXml : null,
        )

        // 2. Populate qt_query_instance
        QtQueryInstance queryInstance = new QtQueryInstance(
                userId       : grailsApplication.config.org.transmartproject.i2b2.user_id,
                groupId      : grailsApplication.config.org.transmartproject.i2b2.group_id,
                startDate    : new Date(),
                statusTypeId : QueryStatus.PROCESSING.id,
                queryMaster  : queryMaster,
        )
        queryMaster.addToQueryInstances(queryInstance)

        // 3. Populate qt_query_result_instance
        QtQueryResultInstance resultInstance = new QtQueryResultInstance(
                statusTypeId  : QueryStatus.PROCESSING.id,
                startDate     : new Date(),
                queryInstance : queryInstance
        )
        queryInstance.addToQueryResults(resultInstance)

        // 4. Save the three objects
        if (!queryMaster.validate()) {
            throw new InvalidRequestException('Could not create a valid ' +
                    'QtQueryMaster: ' + queryMaster.errors)
        }
        if (queryMaster.save() == null) {
            throw new RuntimeException('Failure saving QtQueryMaster')
        }

        // 5. Build the patient set
        def setSize
        try {
             def sql = patientSetQueryBuilderService.buildPatientSetQuery(
                    resultInstance, definition)

            sessionFactory.currentSession.doWork ({ Connection conn ->
                def statement = conn.prepareStatement(sql)
                setSize = statement.executeUpdate()
                log.debug "Inserted $setSize rows into qt_patient_set_collection"
            } as Work)
        } catch (InvalidRequestException e) {
            throw e /* unchecked; rolls back transaction */
        } catch (Exception e) {
            // 6e. Handle error when building/running patient set query
            StringWriter sw = new StringWriter()
            e.print(new PrintWriter(sw, true))

            resultInstance.setSize = resultInstance.realSetSize = -1
            resultInstance.endDate = new Date()
            resultInstance.statusTypeId = QueryStatus.ERROR.id
            resultInstance.errorMessage = sw.toString()

            queryInstance.endDate = new Date()
            queryInstance.statusTypeId = QueryStatus.ERROR.id
            queryInstance.message = sw.toString()

            if (!resultInstance.save()) {
                log.error("After exception from " +
                        "patientSetQueryBuilderService::buildService, " +
                        "failed saving updated resultInstance and " +
                        "queryInstance")
            }
            return resultInstance
        }

        // 6. Update result instance and query instance
        resultInstance.setSize = resultInstance.realSetSize = setSize
        resultInstance.description = "Patient set for \"${definition.name}\""
        resultInstance.endDate = new Date()
        resultInstance.statusTypeId = QueryStatus.FINISHED.id

        queryInstance.endDate = new Date()
        queryInstance.statusTypeId = QueryStatus.COMPLETED.id

        def newResultInstance = resultInstance.save()
        if (!newResultInstance) {
            throw new RuntimeException('Failure saving resultInstance after ' +
                    'successfully building patient set. Errors: ' +
                    resultInstance.errors)
        }

        // 7. Return result instance
        resultInstance
    }

    private String queryDefinitionToXml(QueryDefinition definition) {
        queryDefinitionXmlService.toXml(definition)
    }
}