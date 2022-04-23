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

package com.riskrieg.bot.command.commands.riskrieg.restricted;

import com.aaronjyoder.util.json.jackson.JacksonUtil;
import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.auth.Auth;
import com.riskrieg.bot.auth.DefaultAuth;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.config.BotConfig;
import com.riskrieg.bot.config.ServerConfig;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.core.constant.Colors;
import java.io.IOException;
import java.nio.file.Path;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

public class AdjustConfig implements Command {

  private final Settings settings;

  public AdjustConfig() {
    this.settings = new StandardSettings(
        "Manage the configuration settings.",
        "config")
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
    SubcommandData server = new SubcommandData("server", "Requires 'Manage Server' permission. Manage the server configuration settings.");
    OptionData serverConfigItems = new OptionData(OptionType.STRING, "item", "Select a server config item.")
        .addChoice("kickOnServerExit", "kickOnServerExit")
        .addChoice("mentionOnTurn", "mentionOnTurn");
    server.addOptions(serverConfigItems.setRequired(true));
    server.addOption(OptionType.BOOLEAN, "enabled", "Select whether this config item should be enabled or disabled.", true);

    SubcommandData bot = new SubcommandData("bot", "Owner only. Manage the bot configuration settings.");
    OptionData botConfigItems = new OptionData(OptionType.STRING, "item", "Select a bot config item.")
        .addChoice("registerCommandsOnStartup", "registerCommandsOnStartup");
    bot.addOptions(botConfigItems.setRequired(true));
    bot.addOption(OptionType.BOOLEAN, "enabled", "Select whether this config item should be enabled or disabled.", true);

    return Commands.slash(settings().name(), settings().description())
        .addSubcommands(server, bot);
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

      String subcommandName = event.getSubcommandName();
      if (subcommandName == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid subcommand.")).queue();
        return;
      }

      OptionMapping enabledOpt = event.getOption("enabled");
      if (enabledOpt == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid value for 'enabled' parameter.")).queue();
        return;
      }

      boolean enabled = enabledOpt.getAsBoolean();

      OptionMapping itemOpt = event.getOption("item");
      if (itemOpt == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid config item selected.")).queue();
        return;
      }

      String configItemName = itemOpt.getAsString();

      // Command execution
      switch (subcommandName) {
        default -> hook.sendMessage(MessageUtil.error(settings, "Invalid subcommand.")).queue();
        case "server" -> processServerConfig(hook, member, guild, configItemName, enabled);
        case "bot" -> processBotConfig(hook, member, configItemName, enabled);
      }

    });
  }

  private void processServerConfig(InteractionHook hook, Member member, Guild guild, String configItemName, boolean enabled) {
    if (member.hasPermission(Permission.MANAGE_SERVER)) {

      ServerConfig config = new ServerConfig(guild.getIdLong());
      try {
        config = JacksonUtil.read(config.path(), ServerConfig.class);
      } catch (IOException ignored) {
        // Can continue using default values.
      }
      if (config == null) {
        config = new ServerConfig(guild.getIdLong());
      }

      switch (configItemName) {
        default -> hook.sendMessage(MessageUtil.error(settings, "Unknown config item name.")).queue();
        case "kickOnServerExit" -> {
          try {
            JacksonUtil.write(config.path(), ServerConfig.class, config.withKickOnServerExit(enabled));
            hook.sendMessage(MessageUtil.success(settings, "Config successfully updated.")).queue();
          } catch (IOException e) {
            hook.sendMessage(MessageUtil.error(settings, "Could not write new values to config.")).queue();
          }
        }
        case "mentionOnTurn" -> {
          try {
            JacksonUtil.write(config.path(), ServerConfig.class, config.withMentionOnTurn(enabled));
            hook.sendMessage(MessageUtil.success(settings, "Config successfully updated.")).queue();
          } catch (IOException e) {
            hook.sendMessage(MessageUtil.error(settings, "Could not write new values to config.")).queue();
          }
        }
      }
    } else {
      hook.sendMessage(MessageUtil.error(settings, "Insufficient permissions to use this command.")).queue();
    }
  }

  private void processBotConfig(InteractionHook hook, Member member, String configItemName, boolean enabled) {
    try {
      Auth auth = JacksonUtil.read(Path.of(BotConstants.AUTH_PATH + "auth.json"), DefaultAuth.class);
      if (auth == null) {
        throw new NullPointerException("Auth could not be loaded: null or missing");
      }
      if (member.getId().equals(auth.ownerId())) {

        BotConfig config = new BotConfig();
        try {
          config = JacksonUtil.read(config.path(), BotConfig.class);
        } catch (IOException ignored) {
          // Can continue using default values.
        }
        if (config == null) {
          config = new BotConfig();
        }

        switch (configItemName) {
          default -> hook.sendMessage(MessageUtil.error(settings, "Unknown config item name.")).queue();
          case "registerCommandsOnStartup" -> {
            try {
              JacksonUtil.write(config.path(), ServerConfig.class, config.withRegisterCommandsOnStartup(enabled));
              hook.sendMessage(MessageUtil.success(settings, "Config successfully updated.")).queue();
            } catch (IOException e) {
              hook.sendMessage(MessageUtil.error(settings, "Could not write new values to config.")).queue();
            }
          }
        }
      } else {
        hook.sendMessage(MessageUtil.error(settings, "Insufficient permissions to use this command.")).queue();
      }
    } catch (Exception e) {
      hook.sendMessage(MessageUtil.error(settings, "Unable to check for permissions to use this command.")).queue();
    }
  }

}
