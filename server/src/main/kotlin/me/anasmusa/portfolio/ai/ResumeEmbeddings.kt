package me.anasmusa.portfolio.ai

import kotlinx.serialization.Serializable

@Serializable
data class ResumeEmbeddings(
    val id: String,
    val text: String,
    val embeddingValues: List<Float>
)
