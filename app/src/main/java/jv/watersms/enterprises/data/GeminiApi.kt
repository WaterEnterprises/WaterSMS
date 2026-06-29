package jv.watersms.enterprises.data

import jv.watersms.enterprises.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "type") val type: String = "application/json"
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "responseSchema") val responseSchema: ResponseSchema? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    @Json(name = "type") val type: String, // "ARRAY"
    @Json(name = "items") val items: SchemaItems? = null,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class SchemaItems(
    @Json(name = "type") val type: String // "STRING"
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiRepository {
    suspend fun generateVariations(originalMessage: String): List<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is missing or invalid in Secrets!")
        }

        val prompt = """
            Generate exactly 20 distinct, engaging, natural, and concise ways to say the following SMS message.
            Keep the core meaning intact. Do not include placeholders like "[Your Name]" or generic text unless it's in the original message.
            Make some professional, some friendly, some urgent, and some casual so there's high variety.
            Return ONLY a JSON array of 20 strings. Do not use any markdown formatting, no backticks, no comments.
            Original Message: $originalMessage
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = ResponseSchema(
                    type = "ARRAY",
                    items = SchemaItems(type = "STRING"),
                    description = "A list of exactly 20 message phrasings"
                ),
                temperature = 0.9f
            )
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty or null response from Gemini API")

        // Parse JSON array
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(listType)
        
        // Clean markdown backticks if any
        val cleanText = text.trim()
            .replace("^```json".toRegex(), "")
            .replace("^```".toRegex(), "")
            .replace("```$".toRegex(), "")
            .trim()

        return adapter.fromJson(cleanText) ?: emptyList()
    }
}
