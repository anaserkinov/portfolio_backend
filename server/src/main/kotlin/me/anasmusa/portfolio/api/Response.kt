package me.anasmusa.portfolio.api
import kotlinx.serialization.Serializable

@Serializable
class Response(
    val type: Int,
    val data: String
)