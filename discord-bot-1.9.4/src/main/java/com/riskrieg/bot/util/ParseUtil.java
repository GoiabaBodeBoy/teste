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

import com.riskrieg.core.api.gamemode.GameMode;
import com.riskrieg.core.api.gamemode.GameModeType;
import com.riskrieg.core.api.gamemode.brawl.BrawlMode;
import com.riskrieg.core.api.gamemode.classic.ClassicMode;
import com.riskrieg.core.api.gamemode.conquest.ConquestMode;
import com.riskrieg.core.api.gamemode.creative.CreativeMode;
import com.riskrieg.core.api.gamemode.regicide.RegicideMode;
import com.riskrieg.core.api.map.options.Availability;
import com.riskrieg.core.api.map.options.Flavor;
import com.riskrieg.core.api.map.options.alignment.HorizontalAlignment;
import com.riskrieg.core.api.map.options.alignment.VerticalAlignment;
import com.riskrieg.core.constant.color.ColorBatch;
import com.riskrieg.core.constant.color.PlayerColor;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.LevenshteinDistance;

public class ParseUtil {

  public static Optional<Boolean> parseEnable(String str) {
    if (str.equals("false") || str.equals("disabled") || str.equals("disable") || str.equals("d") || str.equals("no") || str.equals("n")) {
      return Optional.of(false);
    } else if (str.equals("true") || str.equals("enabled") || str.equals("enable") || str.equals("e") || str.equals("yes") || str.equals("y")) {
      return Optional.of(true);
    }
    return Optional.empty();
  }

  public static Class<? extends GameMode> parseModeType(GameModeType type) {
    return switch (type) {
      case CLASSIC -> ClassicMode.class;
      case CONQUEST -> ConquestMode.class;
      case REGICIDE -> RegicideMode.class;
      case BRAWL -> BrawlMode.class;
      case CREATIVE -> CreativeMode.class;
      case UNKNOWN -> null;
    };
  }


  public static PlayerColor parsePlayerColor(String requestedColor) {
    ColorBatch batch = RiskriegUtil.loadColorBatch();
    if (requestedColor == null || requestedColor.isEmpty()) {
      return batch.last();
    }
    return batch.toSet().stream().filter(pc -> pc.name().equalsIgnoreCase(requestedColor)).findAny().orElse(batch.last());
  }

  public static PlayerColor parseStandardPlayerColor(int rgb) {
    return RiskriegUtil.loadColorBatch().valueOf(new Color(rgb));
  }

  public static Optional<String> parseMapName(Path optionsPath, String requestedName) {
    if (requestedName == null || requestedName.isEmpty()) {
      return Optional.empty();
    }

    try {
      Set<String> availableMaps = Files.list(optionsPath).map(path -> path.getFileName().toString().split("\\.")[0]).collect(Collectors.toSet());

      String closestName = null;
      int lowestDistance = Integer.MAX_VALUE;
      for (String name : availableMaps) {
        int distance = LevenshteinDistance.getDefaultInstance().apply(requestedName, name);
        if (distance < 5 && distance < lowestDistance) {
          lowestDistance = distance;
          closestName = name;
        }
      }
      return Optional.ofNullable(closestName);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static Optional<String> parseMapNameExact(Path optionsPath, String requestedName) {
    if (requestedName == null || requestedName.isEmpty()) {
      return Optional.empty();
    }

    try {
      return Files.list(optionsPath).map(path -> path.getFileName().toString().split("\\.")[0]).filter(name -> name.equalsIgnoreCase(requestedName)).findAny();
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public static VerticalAlignment parseVerticalAlignment(String alignment) {
    for (VerticalAlignment v : VerticalAlignment.values()) {
      if (v.name().equalsIgnoreCase(alignment)) {
        return v;
      }
    }
    return VerticalAlignment.BOTTOM;
  }

  public static HorizontalAlignment parseHorizontalAlignment(String alignment) {
    for (HorizontalAlignment h : HorizontalAlignment.values()) {
      if (h.name().equalsIgnoreCase(alignment)) {
        return h;
      }
    }
    return HorizontalAlignment.LEFT;
  }

  public static Availability parseAvailability(String availability) {
    for (Availability a : Availability.values()) {
      if (a.name().equalsIgnoreCase(availability)) {
        return a;
      }
    }
    return Availability.UNAVAILABLE;
  }

  public static Flavor parseFlavor(String flavor) {
    for (Flavor f : Flavor.values()) {
      if (f.name().equalsIgnoreCase(flavor)) {
        return f;
      }
    }
    return Flavor.UNKNOWN;
  }

}
