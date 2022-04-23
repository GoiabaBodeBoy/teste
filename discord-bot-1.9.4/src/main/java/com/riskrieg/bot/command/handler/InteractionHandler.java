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

package com.riskrieg.bot.command.handler;

import com.riskrieg.bot.command.Command;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class InteractionHandler {

  private final String ownerId;
  private final Set<Command> commands = new HashSet<>();

  public InteractionHandler(String ownerId) {
    this.ownerId = ownerId;
  }

  public void registerCommands(@Nonnull final Set<Command> commands) {
    this.commands.clear();
    this.commands.addAll(commands);
  }

  public void process(SlashCommandInteractionEvent event) {
    var command = fetchCommand(event);
    if (command.isPresent() && canExecute(command.get(), event)) {
      command.get().execute(event);
    } else {
      event.reply("Command action failed.").setEphemeral(true).queue();
    }
  }

  private boolean canExecute(Command command, SlashCommandInteractionEvent event) {
    if (command.settings().ownerOnly() && !event.getUser().getId().equals(ownerId)) {
      return false;
    }
    if (command.settings().guildOnly() && !event.isFromGuild()) {
      return false;
    }
    Guild guild = event.getGuild();
    if (event.isFromGuild() && guild != null && !guild.getSelfMember().hasPermission(command.settings().selfPermissions())) {
      return false;
    }
    Member member = event.getMember();
    if (member != null && !member.hasPermission(command.settings().authorPermissions())) {
      return false;
    }
    return true;
  }

  private Optional<Command> fetchCommand(SlashCommandInteractionEvent event) {
    if (event == null) {
      return Optional.empty();
    }
    for (Command command : commands) {
      if (command.settings().aliasesSet().contains(event.getName())) {
        return Optional.of(command);
      }
    }
    return Optional.empty();
  }

}
