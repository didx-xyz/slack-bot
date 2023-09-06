package slackBotLambda

import com.xebia.functional.xef.prompt.JvmPromptBuilder
import com.xebia.functional.xef.prompt.Prompt
import com.xebia.functional.xef.scala.conversation.*

object AiHandler {

  def getAiResponse(input: String): String = {
    conversation {
      val builder = new JvmPromptBuilder()
        .addSystemMessage(
          "You are an onboarding assistant. " +
            "Your assignment is to obtain the following info from the user: " +
            "Full Name; Email; Cellphone. " +
            "Be courteous."
        )
        .addUserMessage(
          input
        )

      val response = promptMessage(builder.build())
      response
    }
  }
}
