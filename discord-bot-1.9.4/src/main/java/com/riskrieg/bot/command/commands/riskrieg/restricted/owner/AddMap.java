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
import com.riskrieg.map.RkmMap;
import com.riskrieg.rkm.RkmWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class AddMap implements Command {

  private final Settings settings;

  public AddMap() {
    this.settings = new StandardSettings(
        "Owner only. Add a new map to the game.",
        "addmap")
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
    return Commands.slash(settings().name(), settings().description())
        .addOptions(OptionDataUtil.mapUrl().setRequired(true), OptionDataUtil.verticalAlignment().setRequired(true), OptionDataUtil.horizontalAlignment().setRequired(true))
        .addOption(OptionType.BOOLEAN, "overwrite", "Whether the map should be overwritten if it already exists.", false);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      OptionMapping vAlignOpt = event.getOption("vertical-align");
      OptionMapping hAlignOpt = event.getOption("horizontal-align");
      OptionMapping mapLinkOpt = event.getOption("url");
      OptionMapping overwriteOpt = event.getOption("overwrite");

      if (vAlignOpt != null && hAlignOpt != null && mapLinkOpt != null) {
        var vAlign = ParseUtil.parseVerticalAlignment(vAlignOpt.getAsString());
        var hAlign = ParseUtil.parseHorizontalAlignment(hAlignOpt.getAsString());
        boolean overwrite = overwriteOpt != null && overwriteOpt.getAsBoolean();
        String mapLink = mapLinkOpt.getAsString();

        try {
          var optMap = RkmMap.load(new URL(mapLink));
          if (optMap.isPresent()) {
            RkmMap map = optMap.get();

            var optName = ParseUtil.parseMapNameExact(Path.of(BotConstants.MAP_OPTIONS_PATH), map.mapName().simpleName());
            if (optName.isPresent() && !overwrite) {
              hook.sendMessage(MessageUtil.error(settings, "A map with that name already exists.")).queue();
              return;
            }

            RkmWriter rkmWriter = new RkmWriter(map);

            var fos = new FileOutputStream(BotConstants.MAP_PATH + map.mapName().simpleName() + ".rkm");
            rkmWriter.write(fos);
            fos.close();

            InterfaceAlignment alignment = new InterfaceAlignment(vAlign, hAlign);
            MapOptions options = new MapOptions(Flavor.COMMUNITY, Availability.COMING_SOON, alignment);
            GsonUtil.write(Path.of(BotConstants.MAP_OPTIONS_PATH + map.mapName().simpleName() + ".json"), MapOptions.class, options);

            hook.sendMessage(MessageUtil.success(settings, "Successfully added map: **" + map.mapName().displayName() + "**\n"
                    + "Overwritten: **" + overwrite + "**\n"
                    + "Flavor: **" + options.flavor().toString() + "**\n"
                    + "Availability: **" + options.availability().name() + "**\n"))
                .queue();
          } else {
            hook.sendMessage(MessageUtil.error(settings, "Invalid map file.")).queue();
          }
        } catch (MalformedURLException e) {
          hook.sendMessage(MessageUtil.error(settings, "Malformed URL.")).queue();
        } catch (FileNotFoundException e) {
          hook.sendMessage(MessageUtil.error(settings, "File not found.")).queue();
        } catch (IOException e) {
          hook.sendMessage(MessageUtil.error(settings, "Exception while writing map file.")).queue();
        } catch (NoSuchAlgorithmException e) {
          hook.sendMessage(MessageUtil.error(settings, "Error while writing checksum.")).queue();
        }
      } else {
        hook.sendMessage(MessageUtil.error(settings, "Invalid arguments.")).queue();
      }

    });
  }

}
