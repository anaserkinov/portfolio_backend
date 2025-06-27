package me.anasmusa.portfolio

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.anasmusa.portfolio.ai.AI
import me.anasmusa.portfolio.api.module
import me.anasmusa.portfolio.db.Database


var isDebug = false

val ai = AI()
val db = Database()

fun main(args: Array<String>) {
    isDebug = args.getOrNull(0) == "dev"

    if (!db.initialized){
        db.init(ai.getResumeEmbeddings())
    }

    embeddedServer(
        Netty,
        port = 8085,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}
