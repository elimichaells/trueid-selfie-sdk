package com.trueid.sdk.selfie.internal

import com.trueid.sdk.selfie.TrueIDSdk
import com.trueid.sdk.selfie.VerificationError
import com.trueid.sdk.selfie.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight HTTP client for TrueID API calls.
 * Uses HttpURLConnection to avoid adding OkHttp as a dependency.
 */
internal object TrueIDApiClient {

    /**
     * Calls POST /api/v1/selfie-verify with the given PIN and selfie payload.
     *
     * @return [VerificationResult] on success, throws [VerificationError] on failure.
     */
    suspend fun verifySelfie(
        pinNumber: String,
        selfieBase64: String,
        burstFrames: List<String> = emptyList(),
        forceNia: Boolean,
        enforceFaceComparison: Boolean,
        livenessPassed: Boolean?,
        transactionType: String?,
    ): VerificationResult = withContext(Dispatchers.IO) {
        TrueIDSdk.ensureInitialized()

        val url = URL("${TrueIDSdk.baseUrl}/api/v1/selfie-verify")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", TrueIDSdk.apiKey)
            setRequestProperty("User-Agent", "TrueID-Android-SDK/1.0.0")
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
        }

        try {
            val body = JSONObject().apply {
                put("pinNumber", pinNumber)
                put("image", selfieBase64)
                put("forceNia", forceNia)
                put("enforceFaceComparison", enforceFaceComparison)
                if (burstFrames.isNotEmpty()) {
                    put("burstFrames", JSONArray(burstFrames))
                }
                if (livenessPassed != null) {
                    put("livenessPassed", livenessPassed)
                }
                if (transactionType != null) {
                    put("transactionType", transactionType)
                }
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseBody = try {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Exception) {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            }

            val json = JSONObject(responseBody)

            if (responseCode !in 200..299 || !json.optBoolean("success", false)) {
                val error = json.optString("error", "Verification failed")
                val code = json.optString("code").takeIf { it.isNotEmpty() }
                throw VerificationError.ApiError(code, error)
            }

            val data = json.optJSONObject("data") ?: json

            VerificationResult(
                verified = data.optBoolean("verified", false),
                lookupSource = data.optString("lookupSource").takeIf { it.isNotEmpty() },
                scanRecordId = data.optString("scanRecordId").takeIf { it.isNotEmpty() },
                fullName = data.optString("fullName").takeIf { it.isNotEmpty() },
                documentNumber = data.optString("documentNumber").takeIf { it.isNotEmpty() },
                nationality = data.optString("nationality").takeIf { it.isNotEmpty() },
                dateOfBirth = data.optString("dateOfBirth").takeIf { it.isNotEmpty() },
                gender = data.optString("gender").takeIf { it.isNotEmpty() },
                expiryDate = data.optString("expiryDate").takeIf { it.isNotEmpty() },
                phoneNumber = data.optString("phoneNumber").takeIf { it.isNotEmpty() },
                email = data.optString("email").takeIf { it.isNotEmpty() },
                selfieUrl = data.optString("selfieUrl").takeIf { it.isNotEmpty() },
                niaPhotoUrl = data.optString("niaPhotoUrl").takeIf { it.isNotEmpty() },
                transactionType = data.optString("transactionType").takeIf { it.isNotEmpty() },
                errorMessage = null,
                errorCode = null,
            )
        } catch (e: VerificationError) {
            throw e
        } catch (e: Exception) {
            throw VerificationError.NetworkError(e.message ?: "Network request failed")
        } finally {
            connection.disconnect()
        }
    }
}
