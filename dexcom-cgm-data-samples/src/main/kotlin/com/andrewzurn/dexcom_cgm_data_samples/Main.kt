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
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.SimpleQuantity
import java.math.BigDecimal
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun main() {
    // gather our components
    val client = HttpClient.newBuilder().build()
    val jsonObjectMapper = getObjectMapper()
    val fhirContext = FhirContext.forR4()
    val parser = fhirContext.newJsonParser().setPrettyPrint(true)

    println("Gathering token, egvs, and writing to local files...")
    val token = getAccessToken(client, jsonObjectMapper)
    val egvWrapper = getSandboxEgvs(token, client, jsonObjectMapper)
    val observationResource = convertToObservations(egvWrapper, fhirContext)

    FileUtils.getFile("./dexcom-egvs.json")
        .writeText(jsonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(egvWrapper))
    FileUtils.getFile("./fhir-egvs-bundle.json")
        .writeText(parser.encodeResourceToString(observationResource))

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
        println("Successfully fetch egvs.")
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
    egvWrapper.records.map { egv ->
        val egvObservation = Observation().apply {
            id = egv.recordId

            // Set the timestamp
            effective = DateTimeType(egv.systemTime.toString())

            // Set the value as SimpleQuantity
            value = SimpleQuantity().apply {
                value = BigDecimal.valueOf(egv.value.toLong())
            }
        }
        builder.addCollectionEntry(egvObservation)
    }

    return builder.bundle
}
