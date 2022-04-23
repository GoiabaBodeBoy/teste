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

package com.riskrieg.bot.command.commands.riskrieg.restricted.server;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.constant.Colors;
import java.nio.file.Path;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class End implements Command {

  private final Settings settings;

  public End() {
    this.settings = new StandardSettings(
        "Requires 'Manage Channel' permission. End a game in the current channel.",
        "end")
        .withColor(Colors.BORDER_COLOR)
        .makeGuildOnly()
        .withAuthorPermissions(Permission.MANAGE_CHANNEL);
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

      // Guard clauses
      Guild guild = event.getGuild();
      if (guild == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid guild.")).queue();
        return;
      }

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {

        if (group.deleteGame(event.getChannel().getId())) {
          EmbedBuilder embedBuilder = new EmbedBuilder();
          embedBuilder.setColor(settings.embedColor());
          embedBuilder.setTitle("Game Ended");
          embedBuilder.setDescription("The game has been ended.");
          embedBuilder.setTimestamp(Instant.now());

          hook.sendMessage(MessageUtil.success(settings, "You have ended the game.")).queue(success -> {
            hook.sendMessageEmbeds(embedBuilder.build()).queue();
          });
        } else {
          hook.sendMessage(MessageUtil.error(settings, "Invalid guild.")).queue();
        }
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
