package uk.nhs.nhsdigital.integrationengine.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Organization
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.nhsdigital.integrationengine.configuration.FHIRServerProperties
import uk.nhs.nhsdigital.integrationengine.configuration.MessageProperties

@Component
class AWSOrganization(val messageProperties: MessageProperties, val awsClient: IGenericClient,
    //sqs: AmazonSQS?,
                      @Qualifier("R4") val ctx: FhirContext,
                      val fhirServerProperties: FHIRServerProperties
) {

    private val log = LoggerFactory.getLogger("FHIRAudit")
    public fun getOrganization(identifier: Identifier): Organization? {
        var bundle: Bundle? = null
        var retry = 3
        while (retry > 0) {
            try {
                bundle = awsClient
                    .search<IBaseBundle>()
                    .forResource(Organization::class.java)
                    .where(
                        Organization.IDENTIFIER.exactly()
                            .systemAndCode(identifier.system, identifier.value)
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
        if (bundle == null || !bundle.hasEntry()) return null
        return bundle.entryFirstRep.resource as Organization
    }
}
