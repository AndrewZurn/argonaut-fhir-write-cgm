package com.andrewzurn.dexcom_cgm_data_samples

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.BundleBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.io.FileUtils
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Device.DeviceNameType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.SimpleQuantity
import java.math.BigDecimal
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/*
 * Access Dexcom's Sandbox API, get some EGVs for a simulated patient using G7, and convert the EGVs
 * to simple FHIR Observation data types following the HL7 Argonaut's Working Groups WIP Implementation Guide found
 * here: https://github.com/hl7/cgm
 *
 * For those attempting to run this, there are a few secrets that need to be provided as environment variables,
 * see `getAccessToken` call below.
 */
fun main() {
    // gather our components
    val client = HttpClient.newBuilder().build()
    val jsonObjectMapper = getObjectMapper()
    val fhirContext = FhirContext.forR4()
    val parser = fhirContext.newJsonParser().setPrettyPrint(true)

    println("Gathering token, egvs, and writing to local files...")
    val token = getAccessToken(client, jsonObjectMapper)
    val egvWrapper = getSandboxEgvs(token, client, jsonObjectMapper)
    val observationResources = convertToObservations(egvWrapper, fhirContext)

    FileUtils.getFile("./dexcom-egvs.json")
        .writeText(jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(egvWrapper))
    FileUtils.getFile("./fhir-egvs-bundle.json")
        .writeText(parser.encodeResourceToString(observationResources))

    println("Done! Wrote local files with EGV and FHIR Observation data.")
}

private fun getSandboxEgvs(token: TokenResponse, client: HttpClient, jsonObjectMapper: ObjectMapper): DexcomEgvWrapper {
    val startDate = "2024-01-01"
    val endDate = "2024-01-02"

    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://sandbox-api.dexcom.com/v3/users/self/egvs?startDate=$startDate&endDate=$endDate"))
        .header("Authorization", "Bearer ${token.accessToken}")
        .GET()
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

    val egvWrapper = if (response.statusCode() == 200) {
        println("Successfully fetched egvs.")
        jsonObjectMapper.readValue<DexcomEgvWrapper>(response.body())
    } else {
        throw RuntimeException("Failed to fetch token, unexpected response status code: ${response.statusCode()}.")
    }
    return egvWrapper
}

private fun getAccessToken(client: HttpClient, jsonObjectMapper: ObjectMapper): TokenResponse {
    val formParams = mapOf(
        "client_id" to System.getenv("DEXCOM_CLIENT_ID"),
        "client_secret" to System.getenv("DEXCOM_CLIENT_SECRET"),
        "code" to System.getenv("DEXCOM_SANDBOX_AUTH_CODE"), // short cut for an actual login, sandbox only
        "redirect_uri" to System.getenv("DEXCOM_LOCAL_REDIRECT_URI"),
        "grant_type" to "authorization_code",
    )
    val formBody = formParams.entries.joinToString("&") {
        "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8.toString())}"
    }

    val tokenRequest = HttpRequest.newBuilder()
        .uri(URI.create("https://sandbox-api.dexcom.com/v2/oauth2/token"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(formBody))
        .build()
    val tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString())
    val token = if (tokenResponse.statusCode() == 200) {
        println("Successfully fetched the token.")
        jsonObjectMapper.readValue<TokenResponse>(tokenResponse.body())
    } else {
        throw RuntimeException("Failed to fetch token, unexpected response status code: ${tokenResponse.statusCode()}.")
    }
    return token
}

private fun getObjectMapper(): ObjectMapper {
    return jacksonObjectMapper()
        .registerModules(JavaTimeModule())
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

private fun convertToObservations(egvWrapper: DexcomEgvWrapper, fhirContext: FhirContext): IBaseBundle {
    val builder = BundleBuilder(fhirContext)
    val idPrefix = "https://sandbox-api.dexcom.com/Observation"
    builder.setMetaField("tag",
        Coding().apply {
            system = "http://hl7.org/uv/cgm/CodeSystem/cgm"
            code = "cgm-data-submission-bundle"
        })
    // val observationMeta = Meta().apply {
    //     profile = listOf(
    //         CanonicalType().apply {
    //             value = "http://hl7.org/uv/cgm/StructureDefinition/cgm-sensor-reading-mass-per-volume"
    //         }
    //     )
    // }

    val observationCategory = listOf(
        CodeableConcept(
            Coding().apply {
                system = "http://terminology.hl7.org/CodeSystem/observation-category"
                code = "laboratory"
                display = "laboratory"
            }
        )
    )
    val observationCode = CodeableConcept(
        Coding().apply {
            system = "http://loinc.org"
            code = "99504-3"
            display = "Glucose [Mass/volume] in Interstitial fluid"
        }
    )
    val observationReference = Reference(IdType("Patient", egvWrapper.userId))
    val observationValueUnit = "mg/dL"
    val observationValueSystem = "http://unitsofmeasure.org"
    val observationValueCode = "mg/dL"
    egvWrapper.records.map { egv ->
        val egvObservation = Observation().apply {
            id = "$idPrefix/${egv.recordId}"
            status = Observation.ObservationStatus.FINAL
            category = observationCategory
            code = observationCode
            // meta = observationMeta
            subject = observationReference
            effective = DateTimeType(egv.systemTime.toString())
            value = SimpleQuantity().apply {
                value = BigDecimal.valueOf(egv.value.toLong())
                unit = observationValueUnit
                system = observationValueSystem
                code = observationValueCode
            }
            device = Reference(IdType("Device", egv.transmitterId))
        }
        builder.addCollectionEntry(egvObservation)
    }

    addDevices(builder, egvWrapper)

    return builder.bundle
}

private fun addDevices(builder: BundleBuilder, egvWrapper: DexcomEgvWrapper) {
    val idPrefix = "https://sandbox-api.dexcom.com/Device"
    // val deviceMeta = Meta().apply {
    //     profile = listOf(
    //         CanonicalType().apply {
    //             value = "http://hl7.org/uv/cgm/StructureDefinition/cgm-device"
    //         }
    //     )
    // }

    egvWrapper.records
        .map { it.device }
        .distinct()
        .map { dexcomDevice ->
            val device = Device().apply {
                id = "$idPrefix/${dexcomDevice.transmitterId}"
                identifier = listOf(
                    Identifier().apply {
                        system = "http://dexcom.com"
                        value = dexcomDevice.transmitterId
                    }
                )
                // meta = deviceMeta
                serialNumber = dexcomDevice.transmitterId
                deviceName = listOf(
                    Device.DeviceDeviceNameComponent().apply {
                        name = "Dexcom CGM - ${dexcomDevice.transmitterGeneration.value.uppercase()}"
                        type = DeviceNameType.USERFRIENDLYNAME
                    }
                )
            }

            builder.addCollectionEntry(device)
        }
}
