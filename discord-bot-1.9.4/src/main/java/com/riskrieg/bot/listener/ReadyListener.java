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

package com.riskrieg.bot.listener;

import com.aaronjyoder.util.json.jackson.JacksonUtil;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.config.BotConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

public class ReadyListener extends ListenerAdapter {

  private final Set<Command> commands;

  public ReadyListener(Set<Command> commands) {
    this.commands = commands;
  }

  @Override
  public void onReady(@NotNull ReadyEvent event) {
    BotConfig botConfig = new BotConfig(false);
    try {
      if (!Files.exists(botConfig.path())) {
        JacksonUtil.write(botConfig.path(), BotConfig.class, botConfig);
        System.out.println("[Startup] A new configuration file was created because one did not exist.");
      }
      botConfig = JacksonUtil.read(botConfig.path(), BotConfig.class);
    } catch (IOException e) {
      System.out.println("[Warning] Could not load bot configuration. Creating new config file with default values.");
      try {
        JacksonUtil.write(botConfig.path(), BotConfig.class, botConfig);
      } catch (IOException ex) {
        System.out.println("[Error] Could not create new config file. Continuing with default values.");
      }
    }
    if (botConfig != null && botConfig.registerCommandsOnStartup()) {
      registerCommands(event.getJDA());
    }
    System.out.println("[ReadyEvent] All systems ready.");
  }

  private void registerCommands(JDA jda) {
    System.out.print("[ReadyEvent] Registering commands with Discord...");

    CommandListUpdateAction updateCommands = jda.updateCommands();

    updateCommands.addCommands(commands.stream().map(Command::commandData).collect(Collectors.toList())).complete();

    System.out.println("\r[ReadyEvent] " + commands.size() + (commands.size() == 1 ? " command" : " commands") + " registered with Discord.");
    System.out.println("[ReadyEvent] NOTE: Commands may take up to one hour to propagate changes.");
  }

}
