package ai

import ai.model.ChatState
import ai.handler.OnboardingHandler

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
      case ChatState.QueryingOpportunities => ???
      case ChatState.Done                  => ???

  }
}
