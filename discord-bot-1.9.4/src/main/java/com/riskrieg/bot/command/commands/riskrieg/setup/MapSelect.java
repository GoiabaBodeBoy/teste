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
import com.riskrieg.bot.util.ParseUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.gamemode.brawl.BrawlMode;
import com.riskrieg.core.api.map.MapOptions;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.Constants;
import com.riskrieg.map.RkmMap;
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

public class MapSelect implements Command {

  private final Settings settings;

  public MapSelect() {
    this.settings = new StandardSettings(
        "Select a " + Constants.NAME + " map.",
        "map")
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
        .addOption(OptionType.STRING, "name", "Type the name of the map you want to select.", true);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "You have selected a map."); // First message has to be ephemeral, so send this.

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

      OptionMapping nameOpt = event.getOption("name");
      if (nameOpt == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid map name.")).queue();
        return;
      }

      var mapName = ParseUtil.parseMapName(Path.of(BotConstants.MAP_OPTIONS_PATH), nameOpt.getAsString());
      if (mapName.isEmpty()) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid map name.")).queue();
        return;
      }

      var optMap = RkmMap.load(Path.of(BotConstants.MAP_PATH + mapName.get() + ".rkm"));
      var optOptions = MapOptions.load(Path.of(BotConstants.MAP_OPTIONS_PATH + mapName.get() + ".json"), false);
      if (optMap.isEmpty() || optOptions.isEmpty()) {
        hook.sendMessage(MessageUtil.error(settings, "Could not load map.")).queue();
        return;
      }

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {
          // TODO: Add player ID to selectMap method so that this check isn't necessary
          if (game.players().stream().anyMatch(player -> player.identity().equals(Identity.of(member.getId())))) {
            game.selectMap(optMap.get(), optOptions.get()).submit(map -> {
              String fileName = map.mapName().simpleName() + ".png";
              EmbedBuilder embedBuilder = new EmbedBuilder();
              embedBuilder.setColor(settings.embedColor());
              embedBuilder.setAuthor("Map created by " + game.map().author().name());
              embedBuilder.setTitle("Map Selected: " + game.map().mapName().displayName() + " | " + game.displayName());
              embedBuilder.setDescription(member.getAsMention() + " has selected this map.");
              if (game instanceof BrawlMode) {
                embedBuilder.setFooter("The game can be started once everyone has joined.");
              } else {
                embedBuilder.setFooter("If you have not selected a territory, you may select one.");
              }
              embedBuilder.setImage("attachment://" + fileName);

              hook.sendMessage(genericSuccess).queue(success -> {
                hook.sendMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                group.saveGame(event.getChannel().getId(), game).submit();
              });

            }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
          } else {
            hook.sendMessage(MessageUtil.error(settings, "Maps can only be selected by players in the game.")).queue();
          }
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
