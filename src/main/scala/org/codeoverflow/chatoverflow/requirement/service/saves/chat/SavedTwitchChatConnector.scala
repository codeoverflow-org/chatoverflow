package org.codeoverflow.chatoverflow.requirement.service.saves.chat

import org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatMessage

class SavedTwitchChatConnector(override val sourceIdentifier: String) extends SavedChatConnector[TwitchChatMessage](sourceIdentifier) {

}
