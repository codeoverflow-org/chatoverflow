package org.codeoverflow.chatoverflow.requirement.service.saves.chat.impl

import org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatMessage
import org.codeoverflow.chatoverflow.api.io.output.file.TwitchChatFileOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.service.saves.chat.SavedChatOutputConnector

@Impl(impl = classOf[TwitchChatFileOutput], connector = classOf[SavedChatOutputConnector])
class SavedTwitchChatOutputImpl extends SavedChatOutputImpl[TwitchChatMessage] with TwitchChatFileOutput {

}
