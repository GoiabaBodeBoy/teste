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

package com.riskrieg.bot.command.commands.riskrieg.running;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.ConfigUtil;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.internal.bundle.SkipBundle;
import java.nio.file.Path;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;

public class Skip implements Command {

  private final Settings settings;

  public Skip() {
    this.settings = new StandardSettings(
        "Skip your turn, or skip the current player's turn if you have the 'Kick Members' permission..",
        "skip")
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
    return Commands.slash(settings().name(), settings().description());
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "Player successfully skipped."); // First message has to be ephemeral, so send this.

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

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {

          if (member.hasPermission(Permission.KICK_MEMBERS)) {
            game.skip(Identity.of(member.getId())).submit(bundle -> {

              if (ConfigUtil.canMention(hook)) {
                hook.sendMessage(genericSuccess).queue(success -> {
                  ConfigUtil.sendWithMention(hook, bundle.currentTurnPlayer().identity().toString(), message -> {
                    message.editMessageEmbeds(skipMessage(bundle)).addFile(RiskriegUtil.constructMapImageData(game), "map.png", new AttachmentOption[0]).queue();
                  });
                });
              } else {
                hook.sendMessage(genericSuccess).queue(success -> {
                  hook.sendMessageEmbeds(skipMessage(bundle)).addFile(RiskriegUtil.constructMapImageData(game), "map.png", new AttachmentOption[0]).queue();
                });
              }
              group.saveGame(event.getChannel().getId(), game).submit();

            }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
          } else {
            game.skipSelf(Identity.of(member.getId())).submit(bundle -> {

              if (ConfigUtil.canMention(hook)) {
                hook.sendMessage(genericSuccess).queue(success -> {
                  ConfigUtil.sendWithMention(hook, bundle.currentTurnPlayer().identity().toString(), message -> {
                    message.editMessageEmbeds(skipMessage(bundle)).addFile(RiskriegUtil.constructMapImageData(game), "map.png", new AttachmentOption[0]).queue();
                  });
                });
              } else {
                hook.sendMessage(genericSuccess).queue(success -> {
                  hook.sendMessageEmbeds(skipMessage(bundle)).addFile(RiskriegUtil.constructMapImageData(game), "map.png", new AttachmentOption[0]).queue();
                });
              }
              group.saveGame(event.getChannel().getId(), game).submit();

            }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
          }
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

  private MessageEmbed skipMessage(SkipBundle bundle) {
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setColor(Colors.BORDER_COLOR);
    embedBuilder.setTitle("Skip");
    embedBuilder.setDescription("**" + bundle.skippedPlayer().name() + "** has skipped their turn.");
    embedBuilder.setImage("attachment://map.png");

    String claimStr = "They may claim " + bundle.claims() + " " + (bundle.claims() == 1 ? "territory" : "territories") + " this turn.";
    embedBuilder.setFooter("It is " + bundle.currentTurnPlayer().name() + "'s turn. " + claimStr);
    return embedBuilder.build();
  }


}
