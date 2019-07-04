package org.codeoverflow.chatoverflow.requirement.service.discord.impl

import java.awt.Color
import java.time.Instant

import net.dv8tion.jda.core.EmbedBuilder
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.discord.DiscordEmbed
import org.codeoverflow.chatoverflow.api.io.output.chat.DiscordChatOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.discord.DiscordChatConnector

import scala.compat.java8.OptionConverters._

/**
  * This is the implementation of the discord chat output, using the discord connector.
  */
@Impl(impl = classOf[DiscordChatOutput], connector = classOf[DiscordChatConnector])
class DiscordChatOutputImpl extends OutputImpl[DiscordChatConnector] with DiscordChatOutput with WithLogger {

  private var channelId: Option[String] = None

  override def start(): Boolean = true

  override def setChannel(channelId: String): Unit = {
    sourceConnector.get.getTextChannel(channelId) match {
      case Some(_) => this.channelId = Some(channelId.trim)
      case None => throw new IllegalArgumentException("TextChannel with that id doesn't exist")
    }
  }

  override def getChannelId: String =
    channelId.getOrElse(throw new IllegalStateException("first set the channel for this output"))

  override def sendChatMessage(message: String): Unit = sourceConnector.get.sendChatMessage(getChannelId, message)

  override def sendChatMessage(embed: DiscordEmbed): Unit = {
    val embedBuilder = new EmbedBuilder()
      .setTitle(embed.getTitle.orElse(null), embed.getUrl.orElse(null))
      .setDescription(embed.getDescription.orElse(null))
      .setColor(embed.getColor.asScala.map(Color.decode).orNull)
      .setTimestamp(embed.getTime.orElse(null))
      .setFooter(embed.getFooterText.orElse(null), embed.getFooterIconUrl.orElse(null))
      .setThumbnail(embed.getThumbnailUrl.orElse(null))
      .setImage(embed.getImageUrl.orElse(null))
      .setAuthor(embed.getAuhthorName.orElse(null), embed.getAuhthorUrl.orElse(null), embed.getAuhthorIconUrl.orElse(null))
    embed.getFields.forEach(f => embedBuilder.addField(f.getName.orElse(null), f.getValue.orElse(null), f.isInline))
    sourceConnector.get.sendChatMessage(getChannelId, embedBuilder.build())
  }

  override def sendFile(file: String): Unit = sourceConnector.get.sendFile(getChannelId, file)

  override def sendFile(file: String, message: String): Unit = sourceConnector.get.sendFile(getChannelId, file, Some(message))

  /**
    * Stops the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
}
