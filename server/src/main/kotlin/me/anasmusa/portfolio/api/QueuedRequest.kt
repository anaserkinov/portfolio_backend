package me.anasmusa.portfolio.api

data class QueuedRequest(
    val userId: Long,
    val messageId: Long,
    val message: String,
    val history: List<MessageRequest.QA>
)