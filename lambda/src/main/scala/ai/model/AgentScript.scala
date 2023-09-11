package ai.model

import com.xebia.functional.xef.prompt.JvmPromptBuilder
import com.xebia.functional.xef.prompt.PromptBuilder

object AgentScript {
  def createOnboardingBuilder(): PromptBuilder = { // Describe AI agent's assignment
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
}
