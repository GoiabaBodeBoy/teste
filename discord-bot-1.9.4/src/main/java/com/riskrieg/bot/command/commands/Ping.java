/*
 *     Riskrieg, an open-source conflict simulation game.
 *     Copyright (C) 2021 Aaron Yoder <aaronjyoder@gmail.com> and Contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.riskrieg.bot.command.commands;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class Ping implements Command {

  private final Settings settings;

  public Ping() {
    this.settings = new StandardSettings(
        "Test to see if the bot is online and functional.",
        "ping", "pong")
        .withColor(BotConstants.GENERIC_CMD_COLOR);
  }

  @NotNull
  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public CommandData commandData() {
    return Commands.slash(settings().name(), settings().description());
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      EmbedBuilder embedBuilder = new EmbedBuilder();
      embedBuilder.setColor(settings.embedColor());
      embedBuilder.setTitle("Ping");
      embedBuilder.setDescription("Pong! :table_tennis:");
      embedBuilder.addField("Gateway Ping", event.getJDA().getGatewayPing() + "ms", true);

      hook.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
        event.getJDA().getRestPing().queue(ping -> {
          embedBuilder.addField("Rest Ping", ping + "ms", true);
          message.editMessageEmbeds(embedBuilder.build()).queue();
        });
      });

    });
  }

}
