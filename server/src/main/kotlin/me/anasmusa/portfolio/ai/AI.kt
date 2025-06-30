package me.anasmusa.portfolio.ai

import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.EmbedContentConfig
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.anasmusa.portfolio.api.MessageRequest
import me.anasmusa.portfolio.db.Database
import java.io.File
import kotlin.jvm.optionals.getOrNull

private const val systemInstruction = """
    You are a helpful AI assistant designed to answer questions about Anas Erkinjonov, based on the provided context chunks retrieved from a vector database. 

    Always:
        Speak as if you are Anas' AI Assistant. 
        Refer to him in the third person (e.g., "He has experience in...", "Anas worked with...").
        Answer as if you are familiar with Anas's work, skills, and background.
        Prioritize accuracy and stay grounded in the provided context.
        Keep responses clear and concise, but friendly and professional.
        Reply in the same language as the user's question (e.g., Uzbek, English, etc.).
        If you don’t have enough information from the context, politely say so instead of making up an answer.
        If the user provides a job offer or job description, compare it with Anas’s skills and experience. Clearly state whether he appears to be a good fit, and support your reasoning with specifics from his background.
        If any required qualifications are missing from Anas's profile, mention them gently and honestly.

    If the user asks something unrelated (e.g., personal advice, current news, or general coding questions), respond with:
    "I'm designed to answer questions about Anas Erkinjonov. Could you please ask something related to him or his work?"

    Here is context for current question:
"""

class AI {

    private val ai = Client()

    fun getResumeEmbeddings(): List<ResumeEmbeddings>{
        val chunksFile = File("data/resume-chunks.json")
        if (chunksFile.exists())
            return Json.decodeFromString(chunksFile.readText())

        val resumeItems = Json.decodeFromString<List<ResumeItem>>(
            Database::class.java.classLoader.getResource("resume.json").readText()
        )

        val resumeEmbeddings = arrayListOf<ResumeEmbeddings>()
        resumeItems.forEachIndexed { index, resumeItem ->
            println(index)
            ai.models.embedContent(
                "gemini-embedding-exp-03-07",
                resumeItem.text,
                EmbedContentConfig.builder()
                    .outputDimensionality(768)
                    .taskType("RETRIEVAL_DOCUMENT")
                    .build()
            ).let {
                it.embeddings().getOrNull()?.forEachIndexed { _, contentEmbedding ->
                    resumeEmbeddings.add(
                        ResumeEmbeddings(
                            resumeItem.id,
                            resumeItem.text,
                            contentEmbedding.values().getOrNull() ?: emptyList()
                        )
                    )
                }
            }
            runBlocking {
                delay(15 * 1000)
            }
        }

        chunksFile.parentFile.mkdir()
        chunksFile.createNewFile()
        chunksFile.writeText(
            Json { prettyPrint = true }.encodeToString(resumeEmbeddings)
        )

        return resumeEmbeddings
    }

    fun embedText(text: String): List<Float> {
        return ai.models.embedContent(
            "gemini-embedding-exp-03-07",
            text,
            EmbedContentConfig.builder()
                .outputDimensionality(768)
                .taskType("RETRIEVAL_QUERY")
                .build()
        ).let {
            it.embeddings().getOrNull()?.getOrNull(0)?.values()?.getOrNull() ?: emptyList()
        }
    }

    fun generate(
        request: String,
        context: List<String>,
        history: List<MessageRequest.QA>
    ): String {

        val info = context.joinToString("\n\n")

        return ai.models.generateContent(
            "gemini-2.0-flash",
            arrayListOf<Content>().also {
                history.forEach { qa ->
                    it.add(
                        Content.builder()
                            .role("user")
                            .parts(listOf(Part.fromText(qa.question)))
                            .build()
                    )
                    it.add(
                        Content.builder()
                            .role("model")
                            .parts(listOf(Part.fromText(qa.answer)))
                            .build()
                    )
                }
                it.add(
                    Content.builder()
                        .role("user")
                        .parts(listOf(Part.fromText(request)))
                        .build()
                )
            },
            GenerateContentConfig.builder()
                .systemInstruction(
                    Content.builder()
                        .role("user")
                        .parts(
                            listOf(
                                Part.fromText(systemInstruction + info)
                            )
                        )
                        .build()
                )
                .build()
        ).text() ?: ""
    }

}