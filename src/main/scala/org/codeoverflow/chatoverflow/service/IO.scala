package org.codeoverflow.chatoverflow.service

import org.codeoverflow.chatoverflow.api.plugin.configuration.{Requirement, Requirements}
import org.codeoverflow.chatoverflow.registry.TypeRegistry.{InputTypes, OutputTypes, ParameterTypes, tripleToFrameworkType}
import org.codeoverflow.chatoverflow.service.twitch.impl.{TwitchChatInputImpl, TwitchChatOutputImpl}

object IO {
  def registerTypes(): Unit = {
    InputTypes(
      "org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatInput" ->
        ("org.codeoverflow.chatoverflow.service.twitch.impl.TwitchChatInputImpl", {
          (requirements: Requirements, id: String, serialized: String) =>
            val requirement = requirements.input.twitchChat(id, null, false)
            val input = new TwitchChatInputImpl
            input.setSourceConnector(serialized)
            input.init()
            requirement.setValue(input)
            requirement
        }, {
          requirement: Requirement[_] =>
            requirement.asInstanceOf[Requirement[TwitchChatInputImpl]].getValue.getSourceIdentifier
        })
    )

    OutputTypes(
      "org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput" ->
        ("org.codeoverflow.chatoverflow.service.twitch.impl.TwitchChatOutputImpl", {
          (requirements: Requirements, id: String, serialized: String) =>
            val requirement = requirements.output.twitchChat(id, null, false)
            val output = new TwitchChatOutputImpl
            output.setSourceConnector(serialized)
            requirement.setValue(output)
            requirement
        }, {
          requirement: Requirement[_] =>
            requirement.asInstanceOf[Requirement[TwitchChatOutputImpl]].getValue.getSourceIdentifier
        })
    )

    ParameterTypes(
      "java.lang.String" ->
        ("java.lang.String", {
          (requirements: Requirements, id: String, serialized: String) =>
            val requirement = requirements.parameter.string(id, null, false)
            requirement.setValue(serialized)
            requirement
        }, {
          requirement: Requirement[_] =>
            requirement.asInstanceOf[Requirement[String]].getValue
        })
    )
  }

  /* SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE

  "INSERT GENERIC TYPE FROM API HERE" ->
      ("INSERT EXACT TYPE FORM FRAMEWORK HERE", {
        (requirements: Requirements, id: String, name: String, isOptional: Boolean, serialized: String) =>
          val requirement = requirements. ...

          INSERT CODE TO CREATE A REQUIREMENT AND SET VALUE USING THE SERIALIZED STRING HERE

          requirement
      }, {
        requirement: Requirement[_] =>
          INSERT CODE TO GET THE IMPORTANT INFORMATION FROM THE REQUIREMENT RETURNED ABOVE AND SERIALIZE IT
      })

      SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE SAMPLE
   */
}
