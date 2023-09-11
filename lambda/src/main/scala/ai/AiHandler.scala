package ai

import com.xebia.functional.xef.prompt.JvmPromptBuilder
import com.xebia.functional.xef.prompt.Prompt
import com.xebia.functional.xef.scala.conversation.*
import com.xebia.functional.xef.store.ConversationId
import scala.collection.mutable
import com.xebia.functional.xef.prompt.PromptBuilder
import io.circe.Decoder

case class OnboardingResult(
    @Description("The next message that you want to send to the user") nextMessageToUser: String,
    @Description("The full name as obtained from the user") fullName: Option[String] = None,
    @Description("The email address as obtained from the user") email: Option[String] = None,
    @Description("The cellphone number as obtained from the user") cellphone: Option[String] = None
) derives SerialDescriptor,
      Decoder

object AiHandler {
  private def createBuilder(): PromptBuilder = {
    // Describe AI agent's assignment
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
        val result = prompt[OnboardingResult](builder.build())
        scribe.info(f"We have the following onboarding result: $result")
        result.nextMessageToUser
      },
      conversationId = Some(ConversationId(conversationId))
    )
  }
}
