package me.anasmusa.portfolio.api

import kotlinx.serialization.Serializable

@Serializable
class MessageRequest(
    val id: Long,
    val message: String,
    val history: List<QA> = emptyList()
){
    @Serializable
    class QA(
        val question: String,
        val answer: String
    )
}