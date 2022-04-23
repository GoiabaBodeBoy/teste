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
import java.util.Objects;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;

public record StandardSettings(boolean disabled, boolean ownerOnly, boolean guildOnly,
                               @Nonnull String description, @Nonnull String[] aliases, @Nonnull Color embedColor,
                               @Nonnull Permission[] authorPermissions, @Nonnull Permission[] selfPermissions) implements Settings {

  public StandardSettings {
    Objects.requireNonNull(aliases);
    Objects.requireNonNull(description);
    Objects.requireNonNull(embedColor);
    Objects.requireNonNull(authorPermissions);
    Objects.requireNonNull(selfPermissions);
    if (aliases.length == 0) {
      throw new IllegalStateException("Set 'aliases' cannot be empty");
    }
    if (description.isBlank()) {
      throw new IllegalStateException("Field 'description' cannot be blank");
    }
  }

  public StandardSettings(@Nonnull String description, @Nonnull String... aliases) {
    this(false, false, false, description, aliases, new Color(128, 128, 128), new Permission[0], new Permission[0]);
  }

  public StandardSettings withColor(Color embedColor) {
    return new StandardSettings(disabled(), ownerOnly(), guildOnly(), description(), aliases(), embedColor, authorPermissions(), selfPermissions());
  }

  public StandardSettings withAuthorPermissions(Permission... authorPermissions) {
    return new StandardSettings(disabled(), ownerOnly(), guildOnly(), description(), aliases(), embedColor(), authorPermissions, selfPermissions());
  }

  public StandardSettings withSelfPermissions(Permission... selfPermissions) {
    return new StandardSettings(disabled(), ownerOnly(), guildOnly(), description(), aliases(), embedColor(), authorPermissions(), selfPermissions);
  }

  public StandardSettings makeDisabled() {
    return new StandardSettings(true, ownerOnly(), guildOnly(), description(), aliases(), embedColor(), authorPermissions(), selfPermissions());
  }

  public StandardSettings makeOwnerOnly() {
    return new StandardSettings(disabled(), true, guildOnly(), description(), aliases(), embedColor(), authorPermissions(), selfPermissions());
  }

  public StandardSettings makeGuildOnly() {
    return new StandardSettings(disabled(), ownerOnly(), true, description(), aliases(), embedColor(), authorPermissions(), selfPermissions());
  }

}
