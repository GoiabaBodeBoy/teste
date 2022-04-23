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
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.bot.util.ParseUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.api.player.Player;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.Constants;
import com.riskrieg.core.constant.color.PlayerColor;
import java.nio.file.Path;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class Join implements Command {

  private final Settings settings;

  public Join() {
    this.settings = new StandardSettings(
        "Join a " + Constants.NAME + " game.",
        "join")
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
        .addOptions(OptionDataUtil.colors().setRequired(true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "You have been added to the game."); // First message has to be ephemeral, so send this.

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
          game.join(Identity.of(member.getId()), member.getEffectiveName(), chosenColor.id()).submit(player -> {

            hook.sendMessage(genericSuccess).queue(success -> {
              hook.sendMessageEmbeds(createMessageEmbed(player, toTitleCase(chosenColor.name()), game.displayName())).queue();
              group.saveGame(event.getChannel().getId(), game).submit();
            });

          }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

  private MessageEmbed createMessageEmbed(Player player, String colorName, String gameModeName) {
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setColor(RiskriegUtil.colorFrom(player.colorId()));
    embedBuilder.setTitle("Join" + " | " + gameModeName);
    if (colorName == null) {
      embedBuilder.setDescription("**" + player.name() + "** has joined the game.");
    } else {
      embedBuilder.setDescription("**" + player.name() + "** has joined the game as **" + colorName + "**.");
    }
    embedBuilder.setFooter("Version: " + Constants.VERSION);
    embedBuilder.setTimestamp(Instant.now());
    return embedBuilder.build();
  }

  private String toTitleCase(String input) {
    StringBuilder titleCase = new StringBuilder(input.length());
    boolean nextTitleCase = true;

    for (char c : input.toLowerCase().toCharArray()) {
      if (Character.isSpaceChar(c)) {
        nextTitleCase = true;
      } else if (nextTitleCase) {
        c = Character.toTitleCase(c);
        nextTitleCase = false;
      }
      titleCase.append(c);
    }

    return titleCase.toString();
  }


}
