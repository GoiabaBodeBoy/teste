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

import com.aaronjyoder.util.json.jackson.JacksonUtil;
import com.riskrieg.bot.auth.Auth;
import com.riskrieg.bot.auth.DefaultAuth;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.commands.Ping;
import com.riskrieg.bot.command.commands.riskrieg.ChangeName;
import com.riskrieg.bot.command.commands.riskrieg.Leave;
import com.riskrieg.bot.command.commands.riskrieg.general.Help;
import com.riskrieg.bot.command.commands.riskrieg.general.Maps;
import com.riskrieg.bot.command.commands.riskrieg.restricted.AdjustConfig;
import com.riskrieg.bot.command.commands.riskrieg.restricted.owner.AddMap;
import com.riskrieg.bot.command.commands.riskrieg.restricted.owner.AdjustMap;
import com.riskrieg.bot.command.commands.riskrieg.restricted.server.End;
import com.riskrieg.bot.command.commands.riskrieg.restricted.server.Kick;
import com.riskrieg.bot.command.commands.riskrieg.running.Claim;
import com.riskrieg.bot.command.commands.riskrieg.running.Skip;
import com.riskrieg.bot.command.commands.riskrieg.running.Stats;
import com.riskrieg.bot.command.commands.riskrieg.running.Turn;
import com.riskrieg.bot.command.commands.riskrieg.running.alliances.Ally;
import com.riskrieg.bot.command.commands.riskrieg.running.alliances.ListAlliances;
import com.riskrieg.bot.command.commands.riskrieg.running.alliances.Unally;
import com.riskrieg.bot.command.commands.riskrieg.setup.Create;
import com.riskrieg.bot.command.commands.riskrieg.setup.Join;
import com.riskrieg.bot.command.commands.riskrieg.setup.MapSelect;
import com.riskrieg.bot.command.commands.riskrieg.setup.Play;
import com.riskrieg.bot.command.commands.riskrieg.setup.TerritorySelect;
import com.riskrieg.bot.command.handler.InteractionHandler;
import com.riskrieg.bot.listener.InteractionListener;
import com.riskrieg.bot.listener.ReadyListener;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class Main {

  public static void main(String[] args) {
    registerFonts();

    try {
      Auth auth = JacksonUtil.read(Path.of(BotConstants.AUTH_PATH + "auth.json"), DefaultAuth.class);
      if (auth == null) {
        throw new NullPointerException("Auth could not be loaded: null or missing");
      }
      Bot bot = new RiskriegBot(auth);

      System.out.print("[Startup] Registering commands locally...");
      InteractionHandler handler = new InteractionHandler(auth.ownerId());
      Set<Command> commands = Set.of(
          new Ping(),

          new Help(),
          new Maps(),

          new Create(),
          new Join(),
          new MapSelect(),
          new TerritorySelect(),
          new Play(),

          new Claim(),
          new Skip(),
          new Turn(),

          new Stats(),

          new Ally(),
          new Unally(),
          new ListAlliances(),

          new Leave(),
          new Kick(),
          new End(),

          new ChangeName(),

          new AddMap(),
          new AdjustMap(),

          new AdjustConfig()
      );
      handler.registerCommands(commands);
      System.out.println("\r[Startup] " + commands.size() + (commands.size() == 1 ? " command" : " commands") + " registered locally.");

      bot.registerListeners(
          new ReadyListener(commands),
          new InteractionListener(handler)
      );
      bot.start();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("\n[Error] The bot could not be started due to an exception.");
    }
  }

  private static void registerFonts() {
    System.out.print("[Startup] Registering fonts...");
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    try {
      // Spectral
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/spectral/Spectral-Regular.ttf")));
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/spectral/Spectral-Italic.ttf")));
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/spectral/Spectral-Bold.ttf")));
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/spectral/Spectral-Light.ttf")));
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/spectral/Spectral-Medium.ttf")));

      // Raleway
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/raleway/Raleway-SemiBold.ttf")));

      // Open Sans
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/open-sans/OpenSans-Regular.ttf")));

      // Noto Serif
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/noto-serif/NotoSerif-Regular.ttf")));

      // Noto Sans
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/noto-sans/NotoSans-Regular.ttf")));

      // Noto Mono
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/noto-mono/NotoMono-Regular.ttf")));

      // Noto Color Emoji
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/noto-color-emoji/NotoColorEmoji.ttf")));

      // Noto Emoji
      ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("res/font/noto-emoji/NotoEmoji-Regular.ttf")));
      System.out.println("\r[Startup] All fonts successfully registered.");
    } catch (Exception e) {
      System.out.println("[Warning] Fonts could not be registered. Runtime errors may occur due to missing fonts.");
    }
  }

}
