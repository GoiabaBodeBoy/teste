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

package com.riskrieg.bot.config;

import com.riskrieg.bot.BotConstants;
import java.nio.file.Path;

public record ServerConfig(long guildId, boolean kickOnServerExit, boolean mentionOnTurn) implements Configuration {

  public ServerConfig(long guildId) {
    this(guildId, false, false);
  }

  public ServerConfig withKickOnServerExit(boolean enabled) {
    return new ServerConfig(guildId, enabled, mentionOnTurn);
  }

  public ServerConfig withMentionOnTurn(boolean enabled) {
    return new ServerConfig(guildId, kickOnServerExit, enabled);
  }

  @Override
  public Path path() {
    return Path.of(BotConstants.CONFIG_PATH + "server/config-" + guildId + ".json");
  }

}
