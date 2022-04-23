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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

public class FontUtil {

  public static Font scaleFont(String text, Rectangle rect, Graphics g) {
    final float fMinimumFont = 12.0F;
    float fMaximumFont = 26.0F;

    /* Use Point2d.Float to hold ( font, width of font in pixels ) pairs. */
    Point2D.Float lowerPoint = new Point2D.Float(fMinimumFont, getWidthInPixelsOfString(text, fMinimumFont, g));
    Point2D.Float upperPoint = new Point2D.Float(fMaximumFont, getWidthInPixelsOfString(text, fMaximumFont, g));
    Point2D.Float midPoint = new Point2D.Float();

    for (int i = 0; i < 50; i++) {
      float middleFont = (lowerPoint.x + upperPoint.x) / 2;

      midPoint.setLocation(middleFont, getWidthInPixelsOfString(text, middleFont, g));

      if (midPoint.y >= rect.getWidth() * .95 && midPoint.y <= rect.getWidth()) {
        break;
      } else if (midPoint.y < rect.getWidth()) {
        lowerPoint.setLocation(midPoint);
      } else if (midPoint.y > rect.getWidth()) {
        upperPoint.setLocation(midPoint);
      }
    }

    fMaximumFont = midPoint.x;

    Font font = g.getFont().deriveFont(fMaximumFont);

    /* Now use Point2d.Float to hold ( font, height of font in pixels ) pairs. */
    lowerPoint.setLocation(fMinimumFont, getHeightInPixelsOfString(text, fMinimumFont, g));
    upperPoint.setLocation(fMaximumFont, getHeightInPixelsOfString(text, fMaximumFont, g));

    if (upperPoint.y < rect.getHeight()) {
      return font;
    }

    for (int i = 0; i < 50; i++) {
      float middleFont = (lowerPoint.x + upperPoint.x) / 2;

      midPoint.setLocation(middleFont, getHeightInPixelsOfString(text, middleFont, g));

      if (midPoint.y >= rect.getHeight() * .95 && midPoint.y <= rect.getHeight()) {
        break;
      } else if (midPoint.y < rect.getHeight()) {
        lowerPoint.setLocation(midPoint);
      } else if (midPoint.y > rect.getHeight()) {
        upperPoint.setLocation(midPoint);
      }
    }

    fMaximumFont = midPoint.x;

    font = g.getFont().deriveFont(fMaximumFont);

    return font;
  }

  public static float getOptimalSize(String text, Rectangle rect, Graphics g, float minFontSize, float maxFontSize) {
    /* Use Point2d.Float to hold ( font, width of font in pixels ) pairs. */
    Point2D.Float lowerPoint = new Point2D.Float(minFontSize, getWidthInPixelsOfString(text, minFontSize, g));
    Point2D.Float upperPoint = new Point2D.Float(maxFontSize, getWidthInPixelsOfString(text, maxFontSize, g));
    Point2D.Float midPoint = new Point2D.Float();

    for (int i = 0; i < 50; i++) {
      float middleFont = (lowerPoint.x + upperPoint.x) / 2;

      midPoint.setLocation(middleFont, getWidthInPixelsOfString(text, middleFont, g));

      if (midPoint.y >= rect.getWidth() * .95 && midPoint.y <= rect.getWidth()) {
        break;
      } else if (midPoint.y < rect.getWidth()) {
        lowerPoint.setLocation(midPoint);
      } else if (midPoint.y > rect.getWidth()) {
        upperPoint.setLocation(midPoint);
      }
    }

    /* Now use Point2d.Float to hold ( font, height of font in pixels ) pairs. */
    lowerPoint.setLocation(minFontSize, getHeightInPixelsOfString(text, minFontSize, g));
    upperPoint.setLocation(midPoint.x, getHeightInPixelsOfString(text, midPoint.x, g));

    if (upperPoint.y < rect.getHeight()) {
      return midPoint.x;
    }

    for (int i = 0; i < 50; i++) {
      float middleFont = (lowerPoint.x + upperPoint.x) / 2;

      midPoint.setLocation(middleFont, getHeightInPixelsOfString(text, middleFont, g));

      if (midPoint.y >= rect.getHeight() * .95 && midPoint.y <= rect.getHeight()) {
        break;
      } else if (midPoint.y < rect.getHeight()) {
        lowerPoint.setLocation(midPoint);
      } else if (midPoint.y > rect.getHeight()) {
        upperPoint.setLocation(midPoint);
      }
    }

    return midPoint.x;
  }


  private static float getWidthInPixelsOfString(String str, float fontSize, Graphics g) {
    Font font = g.getFont().deriveFont(fontSize);

    return getWidthInPixelsOfString(str, font, g);
  }

  private static float getWidthInPixelsOfString(String str, Font font, Graphics g) {
    FontMetrics fm = g.getFontMetrics(font);
    int nWidthInPixelsOfCurrentFont = fm.stringWidth(str);

    return (float) nWidthInPixelsOfCurrentFont;
  }


  private static float getHeightInPixelsOfString(String string, float fontSize, Graphics g) {
    Font font = g.getFont().deriveFont(fontSize);

    return getHeightInPixelsOfString(string, font, g);
  }

  private static float getHeightInPixelsOfString(String string, Font font, Graphics g) {
    FontMetrics metrics = g.getFontMetrics(font);
    int nHeightInPixelsOfCurrentFont = (int) metrics.getStringBounds(string, g).getHeight() - metrics.getDescent() - metrics.getLeading();

    return (float) nHeightInPixelsOfCurrentFont * .75f;
  }


}
