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

package com.riskrieg.bot.command.commands.riskrieg.running.alliances;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.gamemode.AlliableMode;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.Constants;
import java.nio.file.Path;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class Unally implements Command {

  private final Settings settings;

  public Unally() {
    this.settings = new StandardSettings(
        "Break or reject to form an alliance with a player.",
        "unally", "deny")
        .withColor(Colors.BORDER_COLOR)
        .makeGuildOnly();
  }

  @NotNull
  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public CommandData commandData() {
    return Commands.slash(settings().name(), settings().description())
        .addOption(OptionType.USER, "player", "Select the player you would like to break or reject to form an alliance with.", true);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "Command successfully processed."); // First message has to be ephemeral, so send this.

      // Guard clauses
      Member requester = event.getMember();
      if (requester == null) {
        hook.sendMessage(MessageUtil.error(settings, "Could not find member.")).queue();
        return;
      }

      Guild guild = event.getGuild();
      if (guild == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid guild.")).queue();
        return;
      }

      OptionMapping playerOption = event.getOption("player");
      if (playerOption == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid player.")).queue();
        return;
      }

      Member requestee = playerOption.getAsMember();
      if (requestee == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid player.")).queue();
        return;
      }

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {
          if (game instanceof AlliableMode alliableGame) {
            boolean wasAllied = alliableGame.allied(Identity.of(requester.getId()), Identity.of(requestee.getId()));
            alliableGame.unally(Identity.of(requester.getId()), Identity.of(requestee.getId())).submit(allianceBundle -> {

              EmbedBuilder embedBuilder = new EmbedBuilder();
              embedBuilder.setColor(settings.embedColor());
              if (wasAllied) {
                embedBuilder.setTitle("Alliance Broken" + " | " + game.displayName()); // TODO: notify about claims
                embedBuilder.setDescription("**" + allianceBundle.player1().name() + "** and **" + allianceBundle.player2().name() + "** have broken their alliance.");
              } else {
                embedBuilder.setTitle("Alliance Request Rejected" + " | " + game.displayName());
                embedBuilder.setDescription(
                    "The alliance request between **" + allianceBundle.player1().name() + "** and **" + allianceBundle.player2().name() + "** has been rejected.");
              }
              embedBuilder.setFooter("Version: " + Constants.VERSION);
              embedBuilder.setTimestamp(Instant.now());

              hook.sendMessage(genericSuccess).queue(success -> {
                hook.sendMessageEmbeds(embedBuilder.build()).queue();
                group.saveGame(event.getChannel().getId(), game).submit();
              });

            }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
          } else {
            hook.sendMessage(MessageUtil.error(settings, "This game mode does not have alliances.")).queue();
          }
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
