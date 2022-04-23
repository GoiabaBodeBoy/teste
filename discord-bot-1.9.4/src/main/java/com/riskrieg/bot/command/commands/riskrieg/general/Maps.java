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

package com.riskrieg.bot.command.commands.riskrieg.general;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.core.api.map.MapOptions;
import com.riskrieg.core.api.map.options.Flavor;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.Constants;
import com.riskrieg.map.RkmMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class Maps implements Command {

  private final Settings settings;

  public Maps() {
    this.settings = new StandardSettings(
        "Display a list of maps.",
        "maps")
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
        .addOptions(OptionDataUtil.mapFlavorsDisplay());
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Set<MapBundle> bundles = loadMapBundles();

      String mapFlavorTitle = Constants.NAME;
      StringBuilder description = new StringBuilder();

      Flavor mapFlavorChoice = Flavor.OFFICIAL;
      OptionMapping option = event.getOption("flavor");
      if (option != null) {
        switch (option.getAsString()) {
          default -> {
            mapFlavorTitle = "Official " + Constants.NAME;
            mapFlavorChoice = Flavor.OFFICIAL;
            description.append("These are official Riskrieg maps that live up to high quality standards.").append("\n\n");
          }
          case "community" -> {
            mapFlavorTitle = "Community " + Constants.NAME;
            mapFlavorChoice = Flavor.COMMUNITY;
            description.append("These are community-made Riskrieg maps that may or may not live up to the same quality standards as official maps.").append("\n\n");
          }
        }
      }

      description.append("*Map names are in bold and the number of map territories appears in parentheses after the map name.*");

      EmbedBuilder embedBuilder = new EmbedBuilder();
      embedBuilder.setColor(settings.embedColor());
      embedBuilder.setTitle(mapFlavorTitle + " Maps | v" + Constants.VERSION);

      Set<MessageEmbed.Field> fields = getFields(bundles, mapFlavorChoice);
      if (!fields.isEmpty()) {
        fields.forEach(embedBuilder::addField);
      } else {
        embedBuilder.addField("Maps Unavailable", "*There are currently no maps available.*", false);
      }
      embedBuilder.setDescription(description.toString());
      embedBuilder.setFooter("If you would like to contribute, join the official " + Constants.NAME + " server!");
      hook.sendMessageEmbeds(embedBuilder.build()).queue();

    });
  }

  private Set<MessageEmbed.Field> getFields(Set<MapBundle> bundles, Flavor flavorChoice) {
    Set<MessageEmbed.Field> result = new LinkedHashSet<>();
    StringBuilder epicSb = new StringBuilder();
    StringBuilder largeSb = new StringBuilder();
    StringBuilder mediumSb = new StringBuilder();
    StringBuilder smallSb = new StringBuilder();
    StringBuilder comingSoonSb = new StringBuilder();

    bundles.forEach(bundle -> {
      switch (bundle.options().availability()) {
        case AVAILABLE -> {
          if (bundle.options().flavor().equals(flavorChoice)) {
            int size = bundle.rkmMap().graph().vertices().size();
            String displayName = bundle.rkmMap().mapName().displayName();
            displaySorted(displayName, size, smallSb, mediumSb, largeSb, epicSb);
          }
        }
        case COMING_SOON -> {
          if (bundle.options().flavor().equals(flavorChoice)) {
            comingSoonSb.append("**").append(bundle.rkmMap().mapName().displayName()).append("**").append("\n");
          }
        }
        case RESTRICTED -> {
          // TODO: Riskrieg-server only
        }
        case UNAVAILABLE -> {
          // Do nothing for now
        }
      }
    });

    if (!epicSb.isEmpty()) {
      result.add(new MessageEmbed.Field("Epic", epicSb.toString(), false));
    }
    if (!largeSb.isEmpty()) {
      result.add(new MessageEmbed.Field("Large", largeSb.toString(), true));
    }
    if (!mediumSb.isEmpty()) {
      result.add(new MessageEmbed.Field("Medium", mediumSb.toString(), true));
    }
    if (!smallSb.isEmpty()) {
      result.add(new MessageEmbed.Field("Small", smallSb.toString(), true));
    }
    if (!comingSoonSb.isEmpty()) {
      result.add(new MessageEmbed.Field("Coming Soon", comingSoonSb.toString(), false));
    }
    return result;
  }

  private void displaySorted(String displayName, int size, StringBuilder smallSb, StringBuilder mediumSb, StringBuilder largeSb, StringBuilder epicSb) {
    if (size > 0 && size < 65) {
      smallSb.append("**").append(displayName).append("** (").append(size).append(")").append("\n");
    } else if (size >= 65 && size < 125) {
      mediumSb.append("**").append(displayName).append("** (").append(size).append(")").append("\n");
    } else if (size >= 125 && size < 200) {
      largeSb.append("**").append(displayName).append("** (").append(size).append(")").append("\n");
    } else if (size >= 200) {
      epicSb.append("**").append(displayName).append("** (").append(size).append(")").append("\n");
    }
  }

  private Set<MapBundle> loadMapBundles() {
    Set<MapBundle> result = new TreeSet<>();
    try {
      var mapPaths = Files.list(Path.of(BotConstants.MAP_PATH)).collect(Collectors.toSet());
      for (Path path : mapPaths) {
        var optMap = RkmMap.load(path);
        if (optMap.isPresent()) {
          var optOption = MapOptions.load(Path.of(BotConstants.MAP_OPTIONS_PATH + optMap.get().mapName().simpleName() + ".json"), false);
          optOption.ifPresent(mapOptions -> result.add(new MapBundle(optMap.get(), mapOptions)));
        }
      }
      return result;
    } catch (Exception e) {
      return new TreeSet<>();
    }
  }

}

record MapBundle(@Nonnull RkmMap rkmMap, @Nonnull MapOptions options) implements Comparable<MapBundle> {

  @Override
  public int compareTo(@Nonnull MapBundle o) {
    int n = Integer.compare(o.rkmMap().graph().vertices().size(), this.rkmMap().graph().vertices().size());
    if (n == 0) {
      n = o.rkmMap.mapName().simpleName().compareTo(this.rkmMap().mapName().simpleName());
    }
    return n;
  }

}
