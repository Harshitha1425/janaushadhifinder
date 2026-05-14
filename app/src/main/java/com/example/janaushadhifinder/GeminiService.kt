package com.example.janaushadhifinder

// ── Legacy stubs kept so any existing references still compile ──────────────
// The app now uses the Anthropic Claude API directly in MainActivity.kt
// via callCohereApi() (function name kept for compatibility).
//
// To configure: set ANTHROPIC_API_KEY in MainActivity.kt

data class GeminiRequest(val contents: List<GContent>)
data class GContent(val parts: List<GPart>)
data class GPart(val text: String)
data class GeminiResponse(val candidates: List<GCandidate>?)
data class GCandidate(val content: GContent?)
fun GeminiResponse.extractText(): String = ""
interface GeminiApi {
    suspend fun ask(key: String, body: GeminiRequest): GeminiResponse
}
object GeminiClient {
    val api: GeminiApi = object : GeminiApi {
        override suspend fun ask(key: String, body: GeminiRequest) = GeminiResponse(null)
    }
}
