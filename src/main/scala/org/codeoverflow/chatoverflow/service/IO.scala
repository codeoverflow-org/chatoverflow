package org.codeoverflow.chatoverflow.service

import org.codeoverflow.chatoverflow.api.plugin.configuration.{Requirement, Requirements}
import org.codeoverflow.chatoverflow.registry.TypeRegistry.{InputTypes, OutputTypes, ParameterTypes, tripleToFrameworkType}
import org.codeoverflow.chatoverflow.service.twitch.chat.impl.{TwitchChatInputImpl, TwitchChatOutputImpl}

// TODO: The specific type argument is not in use by now - maybe it can be used later to define type options
// TODO: Rework after initial GUI version to accept sub types (tree structure)

/**
  * This object is used to register new input / output / parameter types. This process is needed because the requirement
  * instantiation is statically typed, but the serialization is dynamic. By using a mapping from dynamic type to static
  * instance creation, the framework is highly configurable but the plugin implementation is easy and type safe.
  */
object IO {
  def registerTypes(): Unit = {
    InputTypes(
      "org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatInput" ->
        ("org.codeoverflow.chatoverflow.service.twitch.chat.impl.TwitchChatInputImpl", {
          (requirements: Requirements, id: String, serialized: String) =>
            val requirement = requirements.input.twitchChat(id, null, false)
            val input = new TwitchChatInputImpl
            input.setSourceConnector(serialized)
            input.init()
            requirement.set(input)
            requirement
        }, {
          requirement: Requirement[_] =>
            requirement.asInstanceOf[Requirement[TwitchChatInputImpl]].get.getSourceIdentifier
        })
    )

    OutputTypes(
      "org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput" ->
        ("org.codeoverflow.chatoverflow.service.twitch.chat.impl.TwitchChatOutputImpl", {
          (requirements: Requirements, id: String, serialized: String) =>
            val requirement = requirements.output.twitchChat(id, null, false)
            val output = new TwitchChatOutputImpl
            output.setSourceConnector(serialized)
            output.init()
            requirement.set(output)
            requirement
        }, {
          requirement: Requirement[_] =>
            requirement.asInstanceOf[Requirement[TwitchChatOutputImpl]].get.getSourceIdentifier
        })
    )

    ParameterTypes(
      "java.lang.String" ->
        ("java.lang.String", {
          (requirements: Requirements, id: String, serialized: String) =>
            val requirement = requirements.parameter.string(id, null, false)
            requirement.set(serialized)
            requirement
        }, {
          requirement: Requirement[_] =>
            requirement.asInstanceOf[Requirement[String]].get
        })
    )
  }
}
