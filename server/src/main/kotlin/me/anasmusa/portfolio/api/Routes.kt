package me.anasmusa.portfolio.api

import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import me.anasmusa.portfolio.ai
import me.anasmusa.portfolio.db
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds

private const val TYPE_MESSAGE = 1
private const val TYPE_ID = 2

private var userId = AtomicLong(1L)

private val activeSessions = ConcurrentHashMap<Long, DefaultWebSocketServerSession>()
private val requestQueue = Channel<QueuedRequest>(Channel.UNLIMITED)
private val messageIdByUser = HashMap<Long, Long>()

private val token = System.getenv("LOGBASE_BOT_TOKEN")
private val chatId = System.getenv("LOGBASE_CHAT_ID")

private fun log(message: String){
    println("chatId: $chatId")
    try {
        val text = URLEncoder.encode("#portfolio\n $message", StandardCharsets.UTF_8)
        ProcessBuilder(
            "curl",
            "-X", "GET",
            "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$text"
        ).redirectErrorStream(true)
            .start()
            .waitFor()
    } catch (e: Exception){
        e.printStackTrace()
    }
}

fun Application.module() {
    install(CORS){
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    install(WebSockets){
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(
            Json {
                prettyPrint = true
                explicitNulls = false
            }
        )
    }


    launch {
        for (request in requestQueue){
            if (request.messageId != (messageIdByUser[request.userId] ?: 0L))
                continue
            try {
                val context = db.find(
                    ai.embedText(request.message)
                )
                val aiResponse = ai.generate(
                    request.message,
                    context,
                    request.history
                )

                log(
                    "Message: ${request.message}\n" +
                            "Response: $aiResponse"
                )

                activeSessions[request.userId]?.apply {
                    sendSerialized(
                        Response(
                            TYPE_MESSAGE,
                            Json.encodeToString(MessageResponse(request.messageId, aiResponse))
                        )
                    )
                }
            } catch (e: Exception){
                e.printStackTrace()
                log(
                    "Message: ${request.message}\n" +
                            "Error: ${e.message}"
                )
                activeSessions[request.userId]?.apply {
                    sendSerialized(
                        Response(
                            TYPE_MESSAGE,
                            Json.encodeToString(
                                MessageResponse(
                                    request.messageId,
                                    "Unknown error"
                                )
                            )
                        )
                    )
                }
            }
            delay(1000)
        }
    }


    routing {
        webSocket("/ws"){
            webSocket(userId.incrementAndGet())
        }
        webSocket("/ws/{userId}"){
            webSocket(
                call.parameters["userId"]?.toLong() ?: userId.incrementAndGet()
            )
        }
    }
}

private suspend fun DefaultWebSocketServerSession.webSocket(userId: Long){
    activeSessions[userId] = this

    try {
        while (isActive){
            val request = receiveDeserialized<Request>()
            if (request.type == TYPE_MESSAGE){
                val messageRequest = Json.decodeFromJsonElement<MessageRequest>(request.data!!)
                messageIdByUser[userId] = messageRequest.id
                requestQueue.send(
                    QueuedRequest(
                        userId,
                        messageRequest.id,
                        messageRequest.message,
                        messageRequest.history
                    )
                )
            } else if (request.type == TYPE_ID){
                sendSerialized(
                    Response(
                        TYPE_ID,
                        "$userId"
                    )
                )
            }
        }
    } catch (e: Exception){
        e.printStackTrace()
        this.close()
    } finally {
        activeSessions.remove(userId)
    }
}
