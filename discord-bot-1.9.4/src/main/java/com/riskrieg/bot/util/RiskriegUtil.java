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

import com.aaronjyoder.util.json.jackson.JacksonUtil;
import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.util.view.GameView;
import com.riskrieg.bot.util.view.fill.BasicFill;
import com.riskrieg.bot.util.view.fill.Fill;
import com.riskrieg.bot.util.view.fill.MilazzoFill;
import com.riskrieg.core.api.gamemode.GameMode;
import com.riskrieg.core.api.map.GameMap;
import com.riskrieg.core.api.map.TerritoryType;
import com.riskrieg.core.api.nation.Nation;
import com.riskrieg.core.api.player.Player;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.color.ColorBatch;
import com.riskrieg.core.constant.color.ColorId;
import com.riskrieg.core.constant.color.PlayerColor;
import com.riskrieg.map.territory.SeedPoint;
import com.riskrieg.map.territory.TerritoryId;
import com.riskrieg.map.vertex.Territory;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RiskriegUtil {

  public static ColorBatch loadColorBatch() {
    try {
      return JacksonUtil.read(Path.of(BotConstants.COLORS_PATH, "players.json"), ColorBatch.class);
    } catch (IOException e) {
      return new ColorBatch(
          new PlayerColor(ColorId.of(0), "Salmon", 255, 140, 150), new PlayerColor(ColorId.of(1), "Lavender", 155, 120, 190),
          new PlayerColor(ColorId.of(2), "Thistle", 215, 190, 240), new PlayerColor(ColorId.of(3), "Ice", 195, 230, 255),
          new PlayerColor(ColorId.of(4), "Sky", 120, 165, 215), new PlayerColor(ColorId.of(5), "Sea", 140, 225, 175),
          new PlayerColor(ColorId.of(6), "Forest", 85, 155, 60), new PlayerColor(ColorId.of(7), "Sod", 170, 190, 95),
          new PlayerColor(ColorId.of(8), "Cream", 255, 254, 208), new PlayerColor(ColorId.of(9), "Sun", 240, 225, 80),
          new PlayerColor(ColorId.of(10), "Gold", 255, 195, 5), new PlayerColor(ColorId.of(11), "Cadmium", 250, 105, 65),
          new PlayerColor(ColorId.of(12), "Sanguine", 95, 10, 0), new PlayerColor(ColorId.of(13), "Mocha", 75, 40, 0),
          new PlayerColor(ColorId.of(14), "Matte", 30, 30, 30), new PlayerColor(ColorId.of(15), "Cobalt", 0, 50, 120)
      );
    }
  }

  public static Color colorFrom(ColorId id) {
    ColorBatch batch = loadColorBatch();
    for (PlayerColor pColor : batch.toSet()) {
      if (pColor.id().equals(id)) {
        return pColor.value();
      }
    }
    return batch.last().value();
  }

  public static byte[] constructEmptyMapImageData(GameMode game) {
    return ImageUtil.convertToByteArray(constructMap(game.map(), new HashSet<>(), new HashSet<>()));
  }

  public static byte[] constructMapImageData(GameMode game) {
    return ImageUtil.convertToByteArray(constructMap(game.map(), game.players(), game.nations()));
  }

  private static BufferedImage constructMap(GameMap map, Collection<Player> players, Collection<Nation> nations) {
    try {
      BufferedImage baseImage = ImageUtil.createCopy(ImageUtil.convert(map.mapImage().baseImage(), BufferedImage.TYPE_INT_ARGB));

      for (Nation nation : nations) {
        for (TerritoryId id : nation.territories()) {
          var optPlayer = players.stream().filter(p -> p.identity().equals(nation.identity())).findAny();
          Territory territory = map.get(id);
          if (optPlayer.isPresent() && territory != null) {
            if (nation.territoryIsOfType(id, TerritoryType.CAPITAL)) {
              baseImage = colorCapitalTerritory(baseImage, territory.seedPoints(), RiskriegUtil.colorFrom(optPlayer.get().colorId()));
            } else {
              baseImage = colorTerritory(baseImage, territory.seedPoints(), RiskriegUtil.colorFrom(optPlayer.get().colorId()));
            }
          }
        }
      }

      GameView.drawTerritoryNames(baseImage, map.mapImage());
      GameView.drawPlayerUI(baseImage, map.options().alignment(), players, map.mapName().displayName(), map.mapName().simpleName());

      return baseImage;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static BufferedImage colorTerritory(BufferedImage image, Set<SeedPoint> points, Color newColor) {
    Fill bucket = new MilazzoFill(image, Colors.TERRITORY_COLOR, newColor);
    for (SeedPoint point : points) {
      bucket = new MilazzoFill(image, new Color(image.getRGB(point.x(), point.y())), newColor);
      bucket.fill(new Point(point.x(), point.y()));
    }
    return bucket.getImage();
  }

  private static BufferedImage colorCapitalTerritory(BufferedImage image, Set<SeedPoint> points, Color newColor) {
    BasicFill bucket = new BasicFill(image, Colors.TERRITORY_COLOR, newColor);
    for (SeedPoint point : points) {
      bucket = new BasicFill(image, new Color(image.getRGB(point.x(), point.y())), newColor);
      bucket.fillPattern(new Point(point.x(), point.y()));
    }
    return bucket.getImage();
  }

}
