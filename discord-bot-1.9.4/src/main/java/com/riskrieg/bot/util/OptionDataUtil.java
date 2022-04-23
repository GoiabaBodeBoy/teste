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

package com.riskrieg.bot.util;

import com.riskrieg.core.api.map.options.Availability;
import com.riskrieg.core.api.map.options.Flavor;
import com.riskrieg.core.api.map.options.alignment.HorizontalAlignment;
import com.riskrieg.core.api.map.options.alignment.VerticalAlignment;
import com.riskrieg.core.constant.color.PlayerColor;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class OptionDataUtil {

  public static OptionData modes() {
    return new OptionData(OptionType.STRING, "mode", "Select a game mode.")
        .addChoice("Classic", "classic").addChoice("Conquest", "conquest")
        .addChoice("Regicide", "regicide")
        .addChoice("Brawl", "brawl");
//        .addChoice("Creative", "creative");
  }

  public static OptionData colors() {
    OptionData colors = new OptionData(OptionType.STRING, "color", "Select a color.");
    for (PlayerColor color : RiskriegUtil.loadColorBatch().toSet()) {
      colors.addChoice(color.name(), color.name());
    }
    return colors;
  }

  public static OptionData turnOrders() {
    return new OptionData(OptionType.STRING, "order", "Select the turn order type to use.")
        .addChoice("Standard", "standard")
        .addChoice("Standard (Reversed)", "standard-reversed")
        .addChoice("Standard (Randomized)", "standard-randomized")
        .addChoice("Standard (Reversed & Randomized)", "standard-reversed-randomized")
        .addChoice("Full Random", "full-random");
  }

  public static OptionData mapFlavorsDisplay() {
    return new OptionData(OptionType.STRING, "flavor", "Select the flavor of maps you would like to show.")
        .addChoice("Official", "official")
        .addChoice("Community", "community");
  }

  public static OptionData flavors() {
    OptionData flavor = new OptionData(OptionType.STRING, "flavor", "The flavor of the map.");
    for (Flavor f : Flavor.values()) {
      flavor.addChoice(f.name(), f.name());
    }
    return flavor;
  }

  public static OptionData mapUrl() {
    return new OptionData(OptionType.STRING, "url", "A direct download link to the .rkm map file.");
  }

  public static OptionData verticalAlignment() {
    OptionData verticalAlignment = new OptionData(OptionType.STRING, "vertical-align", "The vertical alignment of the game UI.");
    for (VerticalAlignment va : VerticalAlignment.values()) {
      verticalAlignment.addChoice(va.name(), va.name());
    }
    return verticalAlignment;
  }

  public static OptionData horizontalAlignment() {
    OptionData horizontalAlignment = new OptionData(OptionType.STRING, "horizontal-align", "The horizontal alignment of the game UI.");
    for (HorizontalAlignment ha : HorizontalAlignment.values()) {
      horizontalAlignment.addChoice(ha.name(), ha.name());
    }
    return horizontalAlignment;
  }

  public static OptionData availability() {
    OptionData availability = new OptionData(OptionType.STRING, "availability", "The availability of the map.");
    for (Availability a : Availability.values()) {
      availability.addChoice(a.name(), a.name());
    }
    return availability;
  }

}
