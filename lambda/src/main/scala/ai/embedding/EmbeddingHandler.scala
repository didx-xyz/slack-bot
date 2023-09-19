package ai.embedding

import scala.collection.JavaConverters._
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.inprocess.{InProcessEmbeddingModel, InProcessEmbeddingModelType}
import dev.langchain4j.store.embedding.{EmbeddingMatch, EmbeddingStore}
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel
import dev.langchain4j.data.segment.TextSegment
import java.time.Duration

object EmbeddingHandler {
  private val embeddingModel: EmbeddingModel = HuggingFaceEmbeddingModel
    .builder()
    .accessToken(sys.env("HF_API_KEY"))
    .modelId("sentence-transformers/all-MiniLM-L6-v2")
    .waitForModel(true)
    .timeout(Duration.ofSeconds(60))
    .build()

  private val embeddingStore: EmbeddingStore[TextSegment] =
    new InMemoryEmbeddingStore[TextSegment]()

  def getEmbedding(input: String): Embedding = {
    embeddingModel.embed(input.take(256))
  }

  def getEmbedding(input: TextSegment): Embedding = {
    embeddingModel.embed(input)
  }

  def embedAll(inputList: List[String]): java.util.List[Embedding] = {
    val inputAsTextSegments: java.util.List[TextSegment] = inputList.map(TextSegment.from).asJava
    embeddingModel.embedAll(inputAsTextSegments)
  }

  def storeEmbedding(embedding: Embedding): Unit = {
    embeddingStore.add(embedding)
  }

  def storeEmbedding(embedding: Embedding, embedded: String): Unit = {
    embeddingStore.add(embedding, TextSegment.from(embedded))
  }

  def storeEmbedding(embedding: Embedding, embedded: TextSegment): Unit = {
    embeddingStore.add(embedding, embedded)
  }

  def storeAllEmbeddings(embeddings: java.util.List[Embedding]): Unit = {
    embeddingStore.addAll(embeddings)
  }

  def getAndStoreEmbedding(input: String): Unit = {
    val embedding = getEmbedding(input)
    storeEmbedding(embedding, input)
  }

  def getAndStoreAll(inputList: List[String]): Unit = {
    storeAllEmbeddings(embedAll(inputList))
  }

  def findMostRelevantFromQuery(
      queryText: String,
      maxResults: Int = 1
  ): EmbeddingMatch[TextSegment] = {
    val queryEmbedding: Embedding                   = embeddingModel.embed(queryText.take(256))
    val relevant: List[EmbeddingMatch[TextSegment]] =
      embeddingStore.findRelevant(queryEmbedding, maxResults).asScala.toList
    val embeddingMatch                              = relevant.head
    scribe.info(s"Got embedding match with score: ${embeddingMatch.score()}")
    scribe.info(s"Got embedding match with text: ${embeddingMatch.embedded().text()}")
    embeddingMatch
  }

  def cosineSimilarity(f1: Array[Float], f2: Array[Float]): Double = {
    require(f1.length == f2.length, "Vectors must have the same length")

    val num  = (f1, f2).zipped.map(_ * _).sum
    val den1 = math.sqrt(f1.map(math.pow(_, 2)).sum)
    val den2 = math.sqrt(f2.map(math.pow(_, 2)).sum)

    num / (den1 * den2)
  }

}
