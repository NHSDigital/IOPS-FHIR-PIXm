package uk.nhs.nhsdigital.pmir.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import org.hl7.fhir.r4.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.nhsdigital.pmir.configuration.FHIRServerProperties
import uk.nhs.nhsdigital.pmir.configuration.MessageProperties
import java.util.*

@Component
class AWSSubscription(val messageProperties: MessageProperties, val awsClient: IGenericClient,
    //sqs: AmazonSQS?,
                      @Qualifier("R4") val ctx: FhirContext,
                      val fhirServerProperties: FHIRServerProperties,
                      val awsOrganization: AWSOrganization,
                      val awsBundleProvider : AWSBundle,
                      val awsPractitionerRole: AWSPractitionerRole,
                      val awsPractitioner: AWSPractitioner,
                      val awsPatient: AWSPatient,
                      val awsAuditEvent: AWSAuditEvent) {

    private val log = LoggerFactory.getLogger("FHIRAudit")




    public fun read(identifier: IdType): Subscription? {
        var subscription :Subscription? = null
        var retry = 3
        while (retry > 0) {
            try {
                subscription = awsClient
                    .read()
                    .resource(Subscription::class.java)
                    .withId(identifier)
                    .execute()
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
       return subscription
    }

    public fun delete(identifier: IdType): MethodOutcome? {
        var methodOutcome = MethodOutcome().setCreated(false)
        var retry = 3
        while (retry > 0) {
            try {
                methodOutcome = awsClient
                    .delete()
                    .resourceById(identifier)

                    .execute()
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return methodOutcome
    }

    public fun update(subscription: Subscription,
                      theId: IdType): MethodOutcome? {
        var response: MethodOutcome? = null
        var changed = false

        var retry = 3
        while (retry > 0) {
            try {
                subscription.id = theId.value
                response = awsClient!!.update().resource(subscription).withId(theId.value).execute()
                log.info("AWS Subscription updated " + response.resource.idElement.value)
                val auditEvent = awsAuditEvent.createAudit(subscription, AuditEvent.AuditEventAction.C)
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

    fun create(newSubscription: Subscription): MethodOutcome? {

        var response: MethodOutcome? = null

        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .create()
                    .resource(newSubscription)
                    .execute()
                val subscription = response.resource as Subscription
                val auditEvent = awsAuditEvent.createAudit(subscription, AuditEvent.AuditEventAction.C)
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
