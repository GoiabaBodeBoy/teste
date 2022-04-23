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

package com.riskrieg.bot.command.settings;

import java.awt.Color;
import java.util.Set;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;

public interface Settings {

  default boolean disabled() {
    return false;
  }

  default boolean ownerOnly() {
    return false;
  }

  boolean guildOnly();

  @Nonnull
  default String name() {
    return aliases()[0];
  }

  @Nonnull
  String[] aliases();

  @Nonnull
  default Set<String> aliasesSet() {
    return Set.of(aliases());
  }

  @Nonnull
  String description();

  @Nonnull
  default Color embedColor() {
    return new Color(128, 128, 128);
  }

  default Permission[] authorPermissions() {
    return new Permission[0];
  }

  default Permission[] selfPermissions() {
    return new Permission[0];
  }

}
