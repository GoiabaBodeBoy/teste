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

package com.riskrieg.bot.command.commands.riskrieg.setup;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.ImageUtil;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.bot.util.ParseUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.gamemode.GameMode;
import com.riskrieg.core.api.gamemode.GameModeType;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.Constants;
import com.riskrieg.core.constant.color.PlayerColor;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;

public class Create implements Command {

  private final Settings settings;

  public Create() {
    this.settings = new StandardSettings(
        "Creates a new " + Constants.NAME + " game.",
        "create")
        .withColor(Colors.BORDER_COLOR)
        .makeGuildOnly();
  }

  @NotNull
  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public CommandData commandData() {
    return Commands.slash(settings().name(), settings().description())
        .addOptions(OptionDataUtil.modes().setRequired(true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "A new game was created."); // First message has to be ephemeral, so send this.

      // Guard clauses
      Guild guild = event.getGuild();
      if (guild == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid guild.")).queue();
        return;
      }

      OptionMapping modeOpt = event.getOption("mode");
      if (modeOpt == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid game mode.")).queue();
        return;
      }

      GameModeType type = Arrays.stream(GameModeType.values()).filter(gmt -> gmt.displayName().equalsIgnoreCase(modeOpt.getAsString())).findAny().orElse(GameModeType.UNKNOWN);
      var mode = ParseUtil.parseModeType(type);
      if (mode == null) {
        hook.sendMessage(MessageUtil.error(settings, "Unknown game mode.")).queue();
        return;
      }

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.createGroup(guild.getId()).submit(group -> group.createGame(event.getChannel().getId(), mode).submit(
          game -> {
            hook.sendMessage(genericSuccess).queue(success -> {
              hook.sendMessageEmbeds(createMessage(event.getMember(), game)).addFile(generateColorChoices(), "color-choices.png", new AttachmentOption[0]).queue();
            });
          }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue()
      ), failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

  private MessageEmbed createMessage(Member creator, GameMode game) {
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setColor(settings.embedColor());
    embedBuilder.setTitle("Join Game");
    embedBuilder.setImage("attachment://color-choices.png");
    if (creator != null) {
      embedBuilder.setDescription("Creator: " + creator.getAsMention() + "\nGame Mode selected: **" + game.displayName() + "**");
    } else {
      embedBuilder.setDescription("Creator: Unknown\nGame Mode selected: **" + game.displayName() + "**");
    }
    embedBuilder.setFooter("Please select a color to join the game. You may also choose a map at any time before starting the game.");
    return embedBuilder.build();
  }

  @Nonnull
  private byte[] generateColorChoices() {
    try {
      BufferedImage colorChoicesBlank = ImageIO.read(new File(BotConstants.COLOR_CHOICES_BLANK));

      BufferedImage colorChoicesResult = ImageUtil.createCopy(ImageUtil.convert(colorChoicesBlank, BufferedImage.TYPE_INT_ARGB));

      int startX = 3;
      int startY = 3;
      int column = 0;
      int row = 0;
      int vBuffer = 0;
      int hBuffer = 3;
      int jumpY = 31;
      int jumpX = 95;

      for (PlayerColor color : RiskriegUtil.loadColorBatch().toSet()) {
        // Fill in the player color
        Graphics pcGraphics = colorChoicesResult.getGraphics();
        Color prev = pcGraphics.getColor();
        pcGraphics.setColor(color.value());
        pcGraphics.fillRect(startX + column * jumpX, startY + jumpY * row, 92, 28);
        pcGraphics.setColor(prev);
        pcGraphics.dispose();

        // Draw color name
        Graphics cnGraphics = colorChoicesResult.getGraphics();
        ImageUtil.paintTextWithBounds((Graphics2D) cnGraphics, color.name().toUpperCase(), Colors.TEXT_COLOR,
            (startX + column * jumpX) + hBuffer, (startY + jumpY * row) + 3, (startX + column * jumpX) + 91 - hBuffer, (startY + jumpY * row) + 27 - vBuffer,
            false, true, new Font("Raleway", Font.BOLD, 15), new Font("Noto Mono", Font.BOLD, 15), 12.0F, 15.0F);
        cnGraphics.dispose();
        // Move to next row, shift columns if necessary.
        row++;
        if (row % 8 == 0) {
          row = 0;
          column++;
        }
//        startY += jumpY;
      }

      return ImageUtil.convertToByteArray(colorChoicesResult);
    } catch (IOException e) {
      return new byte[0];
    }
  }

}
