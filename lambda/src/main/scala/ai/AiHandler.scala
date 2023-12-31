package ai

import ai.model.ChatState
import ai.handler.OnboardingHandler
import dev.langchain4j.data.segment.TextSegment
import ai.embedding.EmbeddingHandler
import dev.langchain4j.store.embedding.EmbeddingMatch

object AiHandler {

  def getAiResponse(
      input: String,
      conversationId: String,
      state: ChatState = ChatState.Onboarding
  ): String = {
    scribe.info(
      s"Get AI response for message: $input, for conversationId: $conversationId, in state: $state"
    )

    state match
      case ChatState.Onboarding            => OnboardingHandler.getResponse(input, conversationId)
      case ChatState.QueryingOpportunities => {
        val trimInput = input.split("query:").last

        val embeddingMatch: EmbeddingMatch[TextSegment] =
          EmbeddingHandler.findMostRelevantFromQuery(trimInput)

        val response =
          "Got embedding match with: " +
            s"score = ${embeddingMatch.score()}, " +
            s"embedded = ${embeddingMatch.embedded()}, " +
            s"embeddingId = ${embeddingMatch.embeddingId()}"
        response
      }
      case ChatState.Done                  => ???
  }
}
