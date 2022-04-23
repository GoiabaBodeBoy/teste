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

package com.riskrieg.bot.command.commands.riskrieg;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.Constants;
import java.nio.file.Path;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class ChangeName implements Command {

  private final Settings settings;

  public ChangeName() {
    this.settings = new StandardSettings(
        "Change your nickname in the current game.",
        "name")
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
        .addOption(OptionType.STRING, "name", "Type your new name.", true);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

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

      OptionMapping nameOption = event.getOption("name");
      if (nameOption == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid name.")).queue();
        return;
      }

      String name = nameOption.getAsString();

      if (name.isBlank()) {
        hook.sendMessage(MessageUtil.error(settings, "Name cannot be blank.")).queue();
        return;
      }

      if (name.length() > 32) {
        hook.sendMessage(MessageUtil.error(settings, "Name can only be 32 characters long at maximum.")).queue();
        return;
      }

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {
          game.changeName(Identity.of(member.getId()), name).submit(changed -> {

            if (changed) {
              EmbedBuilder embedBuilder = new EmbedBuilder();
              embedBuilder.setColor(settings.embedColor());
              embedBuilder.setTitle("Name Changed");
              embedBuilder.setDescription("Your name has successfully been changed.");
              embedBuilder.setFooter("Version: " + Constants.VERSION);
              embedBuilder.setTimestamp(Instant.now());

              hook.sendMessageEmbeds(embedBuilder.build()).queue();
              group.saveGame(event.getChannel().getId(), game).submit();
            } else {
              hook.sendMessage(MessageUtil.error(settings, "Name could not be changed.")).queue();
            }

          }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
