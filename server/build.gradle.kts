plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

group = "me.anasmusa.portfolio.backend"
version = "1.0.0"


application {
    mainClass.set("me.anasmusa.portfolio.ApplicationKt")

    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {

    implementation(libs.kotlinx.datetime)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.websocket)
    implementation(libs.ktor.server.cors)


    implementation(libs.kotlin.serialization)

    implementation(libs.protobuf)
    implementation(libs.qdrant)
    implementation(libs.genai)

    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)

}