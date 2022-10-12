package uk.nhs.nhsdigital.integrationengine.configuration

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.StrictErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
open class ApplicationConfiguration {
    @Bean("R4")
    open fun fhirR4Context(): FhirContext {
        val fhirContext = FhirContext.forR4()
        fhirContext.setParserErrorHandler(StrictErrorHandler())
        return fhirContext
    }

    @Bean("R5")
    open fun fhirR5Context(): FhirContext {
        val fhirContext = FhirContext.forR5()
        fhirContext.setParserErrorHandler(StrictErrorHandler())
        return fhirContext
    }

    @Bean("R4B")
    open fun fhirR4BContext(): FhirContext {
        val fhirContext = FhirContext.forR5()
        fhirContext.setParserErrorHandler(StrictErrorHandler())
        return fhirContext
    }

    @Bean("STU3")
    open fun fhirSTU3Context(): FhirContext {
        val fhirContext = FhirContext.forDstu3()
        fhirContext.setParserErrorHandler(StrictErrorHandler())
        return fhirContext
    }

    @Bean
    open fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}