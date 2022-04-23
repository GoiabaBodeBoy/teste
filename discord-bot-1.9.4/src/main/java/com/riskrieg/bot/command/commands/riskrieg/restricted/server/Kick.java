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
import com.riskrieg.bot.util.ConfigUtil;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.bot.util.ParseUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.gamemode.GameState;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.color.PlayerColor;
import java.nio.file.Path;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;

public class Kick implements Command {

  private final Settings settings;

  public Kick() {
    this.settings = new StandardSettings(
        "Requires the 'Kick Members' permission. Remove a player from the current game.",
        "kick")
        .withColor(Colors.BORDER_COLOR)
        .makeGuildOnly()
        .withAuthorPermissions(Permission.KICK_MEMBERS);
  }

  @NotNull
  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public CommandData commandData() {
    return Commands.slash(settings().name(), settings().description())
        .addOptions(OptionDataUtil.colors().setRequired(true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "You have kicked a player from the game."); // First message has to be ephemeral, so send this.

      // Guard clauses
      Member member = event.getMember();
      if (member == null) {
        hook.sendMessage(MessageUtil.error(settings, "Could not find member.")).queue();
        return;
      }

      Guild guild = event.getGuild();
      if (guild == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid guild.")).queue();
        return;
      }

      OptionMapping rgbOpt = event.getOption("color");
      if (rgbOpt == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid color.")).queue();
        return;
      }

      PlayerColor chosenColor = ParseUtil.parsePlayerColor(rgbOpt.getAsString());

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {
          game.leave(chosenColor.id()).submit(bundle -> {

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(RiskriegUtil.colorFrom(bundle.leavingPlayer().colorId()));
            embedBuilder.setTitle("Kick");
            embedBuilder.setDescription("**" + bundle.leavingPlayer().name() + "** has been kicked from the game.");

            var messageAction = hook.sendMessageEmbeds(embedBuilder.build()); // Determine if map should be sent before sending

            boolean shouldSendMap = game.map().isSet() && (game.gameState().equals(GameState.RUNNING) || game.gameState().equals(GameState.SELECTION));
            if (shouldSendMap) {
              embedBuilder.setImage("attachment://map.png");
              messageAction = hook.sendMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), "map.png", new AttachmentOption[0]);
            }

            var finalMessageAction = messageAction;
            hook.sendMessage(genericSuccess).queue(success -> {
              finalMessageAction.queue(message -> {
                if (game.isEnded()) {
                  StringBuilder sb = new StringBuilder();
                  switch (bundle.reason()) {
                    case NO_PLAYERS -> sb.append("There are no players left in the game, so the game has ended.").append("\n");
                    case FORFEIT -> sb.append("**").append(bundle.leavingPlayer().name()).append("** has forfeited the game. **").append(bundle.currentTurnPlayer().name())
                        .append("** has won.").append("\n");
                    case ALLIED_VICTORY -> sb.append("Allied victory! The remaining players have won the game.").append("\n");
                    default -> sb.append("The game is now over.").append("\n");
                  }
                  embedBuilder.addField("Game Ended", sb.toString(), false);
                  embedBuilder.setFooter("Thank you for playing!");

                  message.editMessageEmbeds(embedBuilder.build()).queue();
                  group.deleteGame(event.getChannel().getId());
                } else if (shouldSendMap) {
                  embedBuilder.setColor(Colors.BORDER_COLOR);
                  String claimStr = switch (game.gameState()) {
                    case SELECTION -> "They may select " + bundle.claims() + " " + (bundle.claims() == 1 ? "territory" : "territories") + " this turn.";
                    default -> "They may claim " + bundle.claims() + " " + (bundle.claims() == 1 ? "territory" : "territories") + " this turn.";
                  };
                  embedBuilder.setFooter("It is " + bundle.currentTurnPlayer().name() + "'s turn. " + claimStr);

                  message.editMessageEmbeds(embedBuilder.build()).queue(success2 -> ConfigUtil.sendMentionIfEnabled(hook, bundle.currentTurnPlayer().identity().toString()));
                  group.saveGame(event.getChannel().getId(), game).submit();
                } else {
                  group.saveGame(event.getChannel().getId(), game).submit();
                }
              });
            });

          }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
