package me.anasmusa.portfolio.db

import io.qdrant.client.PointIdFactory.id
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorsFactory.vectors
import io.qdrant.client.WithPayloadSelectorFactory.enable
import io.qdrant.client.grpc.Collections
import io.qdrant.client.grpc.Points
import io.qdrant.client.grpc.Points.PointStruct
import me.anasmusa.portfolio.ai.ResumeEmbeddings

class Database {

    private val qdrant = QdrantClient(
        QdrantGrpcClient.newBuilder("127.0.0.1", 6334, false)
            .build()
    )

    var initialized = false
        private set

    init {
        if (qdrant.collectionExistsAsync("resume").get())
            initialized = true
        else
            qdrant.createCollectionAsync(
                "resume",
                Collections.VectorParams.newBuilder()
                    .setSize(768)
                    .setDistance(Collections.Distance.Cosine)
                    .build()
            ).get()
    }

    fun init(chunks: List<ResumeEmbeddings>) {
        qdrant.upsertAsync(
            "resume",
            chunks.map {
                PointStruct.newBuilder()
                    .setId(id(it.id.hashCode().toLong()))
                    .setVectors(vectors(it.embeddingValues))
                    .putPayload("text", value(it.text))
                    .build()
            }
        ).get()

        initialized = true
    }

    fun find(vector: List<Float>): List<String>{
        return qdrant.searchAsync(
                Points.SearchPoints.newBuilder()
                    .setCollectionName("resume")
                    .addAllVector(vector)
                    .setWithPayload(enable(true))
                    .setLimit(8)
                    .build())
            .get().map {
                it.payloadMap["text"]?.stringValue ?: ""
            }
    }


}