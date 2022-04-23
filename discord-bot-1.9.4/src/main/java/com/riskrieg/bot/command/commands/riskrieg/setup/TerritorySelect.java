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

package com.riskrieg.bot.command.commands.riskrieg.setup;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.ConfigUtil;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.gamemode.brawl.BrawlMode;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.map.territory.TerritoryId;
import java.nio.file.Path;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;

public class TerritorySelect implements Command {

  private final Settings settings;

  public TerritorySelect() {
    this.settings = new StandardSettings(
        "Select a territory.",
        "select", "territory", "capital")
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
        .addOption(OptionType.STRING, "territory", "Select a territory to call your own.", true);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "You have selected a territory."); // First message has to be ephemeral, so send this.

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

      OptionMapping territoryOpt = event.getOption("territory");
      if (territoryOpt == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid territory name.")).queue();
        return;
      }

      String territoryName = territoryOpt.getAsString().toUpperCase();
      if (territoryName.isEmpty()) {
        hook.sendMessage(MessageUtil.error(settings, "A territory must be provided.")).queue();
        return;
      }

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {
          game.selectTerritory(Identity.of(member.getId()), new TerritoryId(territoryName)).submit(nation -> {

            EmbedBuilder embedBuilder = new EmbedBuilder();
            String fileName = game.map().mapName().simpleName() + ".png";
            embedBuilder.setColor(settings.embedColor());
            embedBuilder.setTitle(game.map().mapName().displayName() + " | " + game.displayName());
            embedBuilder.setDescription("**" + member.getEffectiveName() + "** has selected **" + territoryName + "**.");
            embedBuilder.setFooter("Your territory has been established.");
            embedBuilder.setImage("attachment://" + fileName);

            if (game instanceof BrawlMode brawlMode) {
              brawlMode.update().submit(updateBundle -> {
                String claimStr = switch (game.gameState()) {
                  case RUNNING -> "They may claim " + updateBundle.claims() + " " + (updateBundle.claims() == 1 ? "territory" : "territories") + " this turn.";
                  default -> "They may select " + updateBundle.claims() + " " + (updateBundle.claims() == 1 ? "territory" : "territories") + " this turn.";
                };
                embedBuilder.setFooter("It is " + updateBundle.currentTurnPlayer().name() + "'s turn. " + claimStr);

                if (ConfigUtil.canMention(hook)) {
                  hook.sendMessage(genericSuccess).queue(success -> {
                    ConfigUtil.sendWithMention(hook, updateBundle.currentTurnPlayer().identity().toString(), message -> {
                      message.editMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                    });
                  });
                } else {
                  hook.sendMessage(genericSuccess).queue(success -> {
                    hook.sendMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                  });
                }
                group.saveGame(event.getChannel().getId(), game).submit();

              }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
            } else {
              hook.sendMessage(genericSuccess).queue(success -> {
                hook.sendMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                group.saveGame(event.getChannel().getId(), game).submit();
              });
            }

          }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
