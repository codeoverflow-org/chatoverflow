package org.codeoverflow.chatoverflow.requirement.service.saves.chat.impl

import java.util

import com.google.gson.Gson
import org.codeoverflow.chatoverflow.api.io.input.chat.ChatMessage
import org.codeoverflow.chatoverflow.api.io.output.file.ChatFileOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.saves.chat.SavedChatOutputConnector

class SavedChatOutputImpl[T <: ChatMessage] extends Connection[SavedChatOutputConnector] with ChatFileOutput[T] {
  val gson = new Gson()

  override def save(fileName: String, chatMessages: util.List[T]): Unit = {
    sourceConnector.get.save(fileName, gson.toJson(chatMessages))
  }

  override def init(): Unit = {
    if (sourceConnector.isDefined) {
      // TODO ?
    } else {
      logger warn "Source connector not set."
    }
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = setSourceConnector(value)
}
