package uk.nhs.nhsdigital.pmir.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.param.UriParam
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.nhsdigital.pmir.configuration.FHIRServerProperties
import uk.nhs.nhsdigital.pmir.configuration.MessageProperties
import java.util.*

@Component
class AWSTask(val messageProperties: MessageProperties, val awsClient: IGenericClient,
    //sqs: AmazonSQS?,
              @Qualifier("R4") val ctx: FhirContext,
              val fhirServerProperties: FHIRServerProperties,
              val awsPatient: AWSPatient,
              val awsOrganization: AWSOrganization,
              val awsQuestionnaire: AWSQuestionnaire,
              val awsPractitioner: AWSPractitioner,
              val awsBundleProvider: AWSBundle,
              val awsAuditEvent: AWSAuditEvent) {

    private val log = LoggerFactory.getLogger("FHIRAudit")


    fun createUpdate(newTask: Task): MethodOutcome? {
        var awsBundle: Bundle? = null
        if (!newTask.hasIdentifier()) throw UnprocessableEntityException("Task has no identifier")
        var nhsIdentifier: Identifier? = null
        for (identifier in newTask.identifier) {
                if (identifier.system != null && identifier.value != null) nhsIdentifier = identifier
        }
        if (nhsIdentifier == null) throw UnprocessableEntityException("Task has no identifier")
        var retry = 3
        while (retry > 0) {
            try {

                awsBundle = awsClient!!.search<IBaseBundle>().forResource(Task::class.java)
                    .where(
                        Task.IDENTIFIER.exactly()
                            .systemAndCode(nhsIdentifier.system, nhsIdentifier.value)
                    )
                    .returnBundle(Bundle::class.java)
                    .execute()
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }

        if (newTask.hasRequester()) {
            if (newTask.requester.hasIdentifier()) {
                val awsOrganization = awsOrganization.get(newTask.requester.identifier)
                if (awsOrganization != null)   awsBundleProvider.updateReference(newTask.requester,awsOrganization.identifierFirstRep,awsOrganization)

            }
        }
        if (newTask.hasFocus()) {
            if (newTask.focus.hasReference() && newTask.focus.reference.contains("Questionnaire")) {
                val questionnaire = awsQuestionnaire.seach(UriParam().setValue(newTask.focus.reference))
                if (questionnaire != null && questionnaire.size>0) {
                    val canonical = newTask.focus.reference
                    awsBundleProvider.updateReference(newTask.focus,questionnaire[0].identifierFirstRep,questionnaire[0])
                    newTask.focus.display = canonical
                }
            }
        }
        if (newTask.hasFor()) {
            val reference = newTask.`for`
            if (reference.hasIdentifier()) {
                val awsPatient = awsPatient.getPatient(reference.identifier)
                if (awsPatient != null) {
                    awsBundleProvider.updateReference(reference,awsPatient.identifierFirstRep,awsPatient)
                } else {
                    val awsOrganization = awsOrganization.get(reference.identifier)
                    if (awsOrganization != null) {
                        awsBundleProvider.updateReference(reference,awsOrganization.identifierFirstRep,awsOrganization)
                    } else {
                        val awsPractitioner = awsPractitioner.get(reference.identifier)
                        if (awsPractitioner != null) {
                            awsBundleProvider.updateReference(reference,awsPractitioner.identifierFirstRep,awsPractitioner)
                        }
                    }
                }
            }
        }
        if (newTask.hasOwner()) {
            val reference = newTask.owner
            if (reference.hasIdentifier()) {
                val awsPatient = awsPatient.getPatient(reference.identifier)
                if (awsPatient != null) {
                    awsBundleProvider.updateReference(reference,awsPatient.identifierFirstRep,awsPatient)
                } else {
                    val awsOrganization = awsOrganization.get(reference.identifier)
                    if (awsOrganization != null) {
                        awsBundleProvider.updateReference(reference,awsOrganization.identifierFirstRep,awsOrganization)
                    } else {
                        val awsPractitioner = awsPractitioner.get(reference.identifier)
                        if (awsPractitioner != null) {
                            awsBundleProvider.updateReference(reference,awsPractitioner.identifierFirstRep,awsPractitioner)
                        }
                    }
                }
            }
        }



        if (awsBundle!!.hasEntry() && awsBundle.entryFirstRep.hasResource()
            && awsBundle.entryFirstRep.hasResource()
            && awsBundle.entryFirstRep.resource is Task
        ) {
           return updateTask(awsBundle.entryFirstRep.resource as Task, newTask)
            return null
        } else {
            return createTask(newTask)
        }
    }

    private fun updateTask(task: Task, newTask: Task): MethodOutcome? {
        var response: MethodOutcome? = null
        var changed = false


        // TODO do change detection
        changed = true;

       // if (!changed) return MethodOutcome().setResource(task)
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient!!.update().resource(newTask).withId(task.id).execute()
                log.info("AWS Task updated " + response.resource.idElement.value)
                val auditEvent = awsAuditEvent.createAudit(task, AuditEvent.AuditEventAction.C)
                awsAuditEvent.writeAWS(auditEvent)
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response

    }

    private fun createTask(newTask: Task): MethodOutcome? {
        val awsBundle: Bundle? = null
        var response: MethodOutcome? = null

        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .create()
                    .resource(newTask)
                    .execute()
                val task = response.resource as Task
                val auditEvent = awsAuditEvent.createAudit(task, AuditEvent.AuditEventAction.C)
                awsAuditEvent.writeAWS(auditEvent)
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }
}
