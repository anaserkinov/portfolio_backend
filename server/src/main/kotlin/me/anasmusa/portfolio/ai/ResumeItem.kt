package me.anasmusa.portfolio.ai

import kotlinx.serialization.Serializable

@Serializable
data class ResumeItem(
    val id: String,
    val text: String
)
