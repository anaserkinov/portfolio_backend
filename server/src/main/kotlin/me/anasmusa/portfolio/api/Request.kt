package me.anasmusa.portfolio.api
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class Request(
    val type: Int,
    val data: JsonElement?
)