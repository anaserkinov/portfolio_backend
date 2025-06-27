package me.anasmusa.portfolio.api

import kotlinx.serialization.Serializable

@Serializable
class MessageResponse(
    val id: Long,
    val message: String
)