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

package com.riskrieg.bot;

import java.awt.Color;

public class BotConstants {

  public static final String serverInviteStr = "https://discord.com/invite/weU8jYDbW4";
  public static final String sponsorLinkStr = "https://github.com/sponsors/aaronjyoder";

  public static final Color MOD_CMD_COLOR = new Color(235, 125, 50);
  public static final Color GENERIC_CMD_COLOR = new Color(114, 137, 218);
  public static final Color ERROR_COLOR = new Color(165, 15, 5);
  public static final Color SUCCESS_COLOR = new Color(15, 165, 5);

  public static final String VERSION = "2204.22a";

  /* Bot Paths */
  public static final String AUTH_PATH = "res/auth/";
  public static final String CONFIG_PATH = "res/config/";

  /* Riskrieg Paths */
  public static final String SAVE_PATH = "res/saves/";
  public static final String MAP_PATH = "res/maps/";
  public static final String MAP_OPTIONS_PATH = MAP_PATH + "options/";
  public static final String COLORS_PATH = "res/colors/";

  /* Riskrieg Image Resources */
  public static final String COLOR_CHOICES_BLANK = "res/images/color-choices-blank.png";
  public static final String COLOR_CHOICES_VERTICAL_BLANK = "res/images/color-choices-vertical-blank.png";
  public static final String SKULL_IMAGE = "res/images/skull.png";
  public static final String PLAYER_NAME_BACKGROUND = "res/images/player-name-background.png";

  /* Emoji */
  public static final String BULLET_POINT_EMOJI = " :white_small_square: ";
  public static final String ENABLED_EMOJI = " :white_check_mark: ";
  public static final String DISABLED_EMOJI = " :x: ";

}