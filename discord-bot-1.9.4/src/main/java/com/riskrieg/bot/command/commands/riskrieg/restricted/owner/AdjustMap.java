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

package com.riskrieg.bot.command.commands.riskrieg.restricted.owner;

import com.aaronjyoder.util.json.gson.GsonUtil;
import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.bot.util.ParseUtil;
import com.riskrieg.core.api.map.MapOptions;
import com.riskrieg.core.api.map.options.Availability;
import com.riskrieg.core.api.map.options.Flavor;
import com.riskrieg.core.api.map.options.InterfaceAlignment;
import com.riskrieg.core.api.map.options.alignment.HorizontalAlignment;
import com.riskrieg.core.api.map.options.alignment.VerticalAlignment;
import com.riskrieg.map.RkmMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

public class AdjustMap implements Command {

  private final Settings settings;

  public AdjustMap() {
    this.settings = new StandardSettings(
        "Owner only. Set attributes in an existing map's options file.",
        "adjustmap")
        .withColor(BotConstants.MOD_CMD_COLOR)
        .makeOwnerOnly();
  }

  @NotNull
  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public CommandData commandData() {
    SubcommandData adjustAlignment = new SubcommandData("alignment", "Adjust the horizontal and vertical alignment of the UI elements on the map.");
    adjustAlignment.addOption(OptionType.STRING, "map", "The name of the map to adjust the alignment for.", true);
    adjustAlignment.addOptions(OptionDataUtil.verticalAlignment().setRequired(true), OptionDataUtil.horizontalAlignment().setRequired(true));

    SubcommandData adjustAvailability = new SubcommandData("availability", "Adjust the availability of the map.");
    adjustAvailability.addOption(OptionType.STRING, "map", "The name of the map to adjust the availability for.", true);
    adjustAvailability.addOptions(OptionDataUtil.availability().setRequired(true));

    SubcommandData adjustFlavor = new SubcommandData("flavor", "Adjust the flavor of the map.");
    adjustFlavor.addOption(OptionType.STRING, "map", "The name of the map to adjust the flavor for.", true);
    adjustFlavor.addOptions(OptionDataUtil.flavors().setRequired(true));

    return Commands.slash(settings().name(), settings().description())
        .addSubcommands(adjustFlavor, adjustAvailability, adjustAlignment);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply().queue(hook -> {

      String subcommandName = event.getSubcommandName();
      if (subcommandName == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid subcommand.")).queue();
        return;
      }

      var map = parseMap(event.getOption("map"));
      if (map.isEmpty()) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid map.")).queue();
        return;
      }

      switch (subcommandName) {
        default -> hook.sendMessage(MessageUtil.error(settings, "Invalid subcommand.")).queue();
        case "alignment" -> {
          OptionMapping vAlignOpt = event.getOption("vertical-align");
          OptionMapping hAlignOpt = event.getOption("horizontal-align");
          if (vAlignOpt == null || hAlignOpt == null) {
            hook.sendMessage(MessageUtil.error(settings, "Invalid alignment parameter.")).queue();
            return;
          }

          VerticalAlignment vAlign = ParseUtil.parseVerticalAlignment(vAlignOpt.getAsString());
          HorizontalAlignment hAlign = ParseUtil.parseHorizontalAlignment(hAlignOpt.getAsString());

          InterfaceAlignment alignment = new InterfaceAlignment(vAlign, hAlign);
          try {
            MapOptions currentOptions = GsonUtil.read(Path.of(BotConstants.MAP_OPTIONS_PATH + map.get().mapName().simpleName() + ".json"), MapOptions.class);
            if (currentOptions != null) {
              currentOptions.setAlignment(alignment);
              GsonUtil.write(Path.of(BotConstants.MAP_OPTIONS_PATH + map.get().mapName().simpleName() + ".json"), MapOptions.class, currentOptions);
              hook.sendMessage(MessageUtil.success(settings,
                  "Alignment values for " + map.get().mapName().simpleName() + " have successfully been adjusted.")).queue();
            } else {
              hook.sendMessage(MessageUtil.error(settings, "Could not find current map options.")).queue();
            }
          } catch (IOException e) {
            hook.sendMessage(MessageUtil.error(settings, "Could not write file.")).queue();
          }
        }
        case "availability" -> {
          OptionMapping availabilityOpt = event.getOption("availability");
          if (availabilityOpt == null) {
            hook.sendMessage(MessageUtil.error(settings, "Invalid availability parameter.")).queue();
            return;
          }

          Availability availability = ParseUtil.parseAvailability(availabilityOpt.getAsString());

          try {
            MapOptions currentOptions = GsonUtil.read(Path.of(BotConstants.MAP_OPTIONS_PATH + map.get().mapName().simpleName() + ".json"), MapOptions.class);
            if (currentOptions != null) {
              currentOptions.setAvailability(availability);
              GsonUtil.write(Path.of(BotConstants.MAP_OPTIONS_PATH + map.get().mapName().simpleName() + ".json"), MapOptions.class, currentOptions);
              hook.sendMessage(MessageUtil.success(settings,
                  "Availability for " + map.get().mapName().simpleName() + " has successfully been adjusted to " + availability.name() + ".")).queue();
            } else {
              hook.sendMessage(MessageUtil.error(settings, "Could not find current map options.")).queue();
            }
          } catch (IOException e) {
            hook.sendMessage(MessageUtil.error(settings, "Could not write file.")).queue();
          }
        }
        case "flavor" -> {
          OptionMapping flavorOpt = event.getOption("flavor");
          if (flavorOpt == null) {
            hook.sendMessage(MessageUtil.error(settings, "Invalid flavor parameter.")).queue();
            return;
          }

          Flavor flavor = ParseUtil.parseFlavor(flavorOpt.getAsString());

          try {
            MapOptions currentOptions = GsonUtil.read(Path.of(BotConstants.MAP_OPTIONS_PATH + map.get().mapName().simpleName() + ".json"), MapOptions.class);
            if (currentOptions != null) {
              currentOptions.setFlavor(flavor);
              GsonUtil.write(Path.of(BotConstants.MAP_OPTIONS_PATH + map.get().mapName().simpleName() + ".json"), MapOptions.class, currentOptions);
              hook.sendMessage(MessageUtil.success(settings,
                  "Flavor for " + map.get().mapName().simpleName() + " has successfully been adjusted to " + flavor.name() + ".")).queue();
            } else {
              hook.sendMessage(MessageUtil.error(settings, "Could not find current map options.")).queue();
            }
          } catch (IOException e) {
            hook.sendMessage(MessageUtil.error(settings, "Could not write file.")).queue();
          }
        }
      }

    });
  }

  private Optional<RkmMap> parseMap(OptionMapping mapping) {
    if (mapping == null) {
      return Optional.empty();
    }
    try {
      var closestName = ParseUtil.parseMapName(Path.of(BotConstants.MAP_OPTIONS_PATH), mapping.getAsString());
      if (closestName.isPresent()) {
        return RkmMap.load(Path.of(BotConstants.MAP_PATH, closestName.get() + ".rkm"));
      }
      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }


}
