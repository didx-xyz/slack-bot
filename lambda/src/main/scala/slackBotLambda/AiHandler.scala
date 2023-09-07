package slackBotLambda

import com.xebia.functional.xef.prompt.JvmPromptBuilder
import com.xebia.functional.xef.prompt.Prompt
import com.xebia.functional.xef.scala.conversation.*
import com.xebia.functional.xef.store.ConversationId
import scala.collection.mutable
import com.xebia.functional.xef.prompt.PromptBuilder

case class OnboardingResult(
    fullName: Option[String] = None,
    email: Option[String] = None,
    cellphone: Option[String] = None
)
object AiHandler {
  private def createBuilder(): PromptBuilder = {
    new JvmPromptBuilder()
      .addSystemMessage(
        "You are an onboarding assistant. " +
          "If you receive a first message from a user, your job is ask for " +
          "the following info: " +
          "Name; Email; Cellphone. " +
          "Be friendly." +
          "If they only give one attribute at a time, that's fine, just remind " +
          "them until you have all 3 fields." +
          "If a user says no, they don't want to give their email or cellphone, then that is fine, but we at least need a name"
      )
  }

  // Define a map from conversationId to JvmPromptBuilder
  private val builders: mutable.Map[String, PromptBuilder] = mutable.Map()

  def getAiResponse(input: String, conversationId: String): String = {
    scribe.info(s"Get AI response for message: $input, for conversationId: $conversationId")
    // Get the builder for this conversationId, or create a new one if it doesn't exist
    val builder = builders.getOrElseUpdate(
      conversationId,
      createBuilder()
    )

    // Add the user message to the builder
    builder.addUserMessage(input)

    // Create a new conversation with the specific conversationId
    conversation(
      {
        // Get the AI's response
        val response = promptMessage(builder.build())
        response
      },
      conversationId = Some(ConversationId(conversationId))
    )
  }
}
