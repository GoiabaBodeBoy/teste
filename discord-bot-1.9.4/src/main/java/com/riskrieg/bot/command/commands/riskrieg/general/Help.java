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

package com.riskrieg.bot.command.commands.riskrieg.general;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.bot.util.ParseUtil;
import com.riskrieg.core.api.gamemode.GameModeType;
import com.riskrieg.core.constant.Constants;
import java.util.Arrays;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class Help implements Command {

  private final Settings settings;

  public Help() {
    this.settings = new StandardSettings(
        "Display information about how to use the bot.",
        "help")
        .withColor(BotConstants.GENERIC_CMD_COLOR)
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
        .addOptions(OptionDataUtil.modes());
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      OptionMapping modeOpt = event.getOption("mode");
      if (modeOpt == null) {
        hook.sendMessageEmbeds(createBasicHelp()).queue();
        return;
      }

      GameModeType type = Arrays.stream(GameModeType.values()).filter(gmt -> gmt.displayName().equalsIgnoreCase(modeOpt.getAsString())).findAny().orElse(GameModeType.UNKNOWN);
      var mode = ParseUtil.parseModeType(type);
      if (mode == null) {
        hook.sendMessage(MessageUtil.error(settings, "Unknown game mode.")).queue();
      } else {
        hook.sendMessageEmbeds(createModeHelp(type)).queue();
      }

    });
  }

  private MessageEmbed createBasicHelp() {
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setColor(settings.embedColor());
    embedBuilder.setTitle("Help");
    StringBuilder description = new StringBuilder();
    description.append("Riskrieg is a game that lets you simulate wars, battles, alternate history, and more, all through Discord!").append("\n\n");
    description.append("You can interact with the bot via [slash commands](https://support.discord.com/hc/en-us/articles/1500000368501-Slash-Commands-FAQ).").append("\s");
    description.append("You can use `/help` to get started, and you can use `/help mode:[name]` to see specific help for the given game mode.").append("\s");
    description.append("On mobile, your device must be in the [portrait orientation](https://bugs.discord.com/T1989) to use slash commands.").append("\n");
    description.append("\n");
    description.append("Have you found a bug? Join the official Riskrieg server with the invite link below and report it in the appropriate channel!").append("\n");
    embedBuilder.setDescription(description.toString());

    StringBuilder sb = new StringBuilder();
    sb.append(BotConstants.BULLET_POINT_EMOJI + "Use `/create mode:[name]` to create a game.").append("\n");
    sb.append(BotConstants.BULLET_POINT_EMOJI + "Use `/join color:[color]` to select a color to join as.").append("\n");
    sb.append(BotConstants.BULLET_POINT_EMOJI + "Use `/map name:[name]` to select a map.").append("\s");
    sb.append("You can use `/maps` to see a list of available maps.").append("\n");
    sb.append(BotConstants.BULLET_POINT_EMOJI + "Use `/select territory:[name]` to select a starting territory.").append("\s");
    sb.append("Depending on the game mode, this may be a capital territory, or it may be a regular territory.").append("\n");
    sb.append(BotConstants.BULLET_POINT_EMOJI + "Use `/play` to start a game with the standard turn order.").append("\s");
    sb.append("If you would like to select a different turn order, use `/play order:[order]`.").append("\n");

    sb.append("\n");

    sb.append("Once a game has started, you can use `/claim territories:[name1, name2, ...]` to claim territories.").append("\s");
    sb.append("Some game modes have other features, such as alliances.").append("\n");

    sb.append("\n");

    sb.append("You can see all of a game mode's extra features by using `/help mode:[name]`.").append("\n");

    embedBuilder.addField("General Game Setup", sb.toString(), false);

    StringBuilder donateSb = new StringBuilder();
    donateSb.append("Help support the work I'm doing! It costs money and time to run and develop " + Constants.NAME + ".").append("\s");
    donateSb.append("Any amount of support is greatly appreciated.").append("\n");
    donateSb.append(BotConstants.BULLET_POINT_EMOJI + "[Sponsor on GitHub](" + BotConstants.sponsorLinkStr + ")").append("\n");
    embedBuilder.addField("Donate", donateSb.toString(), false);

    StringBuilder serverInviteSb = new StringBuilder();
    serverInviteSb.append(BotConstants.BULLET_POINT_EMOJI + "[Direct Server Invite](" + BotConstants.serverInviteStr + ")").append("\n");
    embedBuilder.addField("Server Invite", serverInviteSb.toString(), true);

    embedBuilder.setFooter(Constants.NAME + " v" + Constants.VERSION);
    return embedBuilder.build();
  }

  private MessageEmbed createModeHelp(GameModeType modeType) {
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setColor(settings.embedColor());
    embedBuilder.setTitle("Help | " + modeType.displayName() + " Mode");
    embedBuilder.setFooter(Constants.NAME + " v" + Constants.VERSION);
    return switch (modeType) {
      case UNKNOWN -> createBasicHelp();
      case CLASSIC -> createClassicHelp(embedBuilder);
      case CONQUEST -> createConquestHelp(embedBuilder);
      case REGICIDE -> createRegicideHelp(embedBuilder);
      case BRAWL -> createBrawlHelp(embedBuilder);
      case CREATIVE -> createCreativeHelp(embedBuilder);
    };
  }

  private final Field allianceControlsField = new Field("Alliance Controls",
      BotConstants.BULLET_POINT_EMOJI + "Use `/ally player:[user]` to send an alliance request to someone, or accept an alliance request that has been sent to you." + "\n"
          + BotConstants.BULLET_POINT_EMOJI + "Use `/unally player:[user]` to break an alliance, or reject an alliance request that has been sent to you." + "\n",
      false);

  private MessageEmbed createClassicHelp(EmbedBuilder embedBuilder) {
    StringBuilder description = new StringBuilder();
    description.append(BotConstants.DISABLED_EMOJI).append("**Alliances**").append("\n");
    description.append(BotConstants.DISABLED_EMOJI).append("**Capitals**").append("\n");
    description.append("\n");
    description.append("This mode can be played as outlined in the basic `/help` command.").append("\n");
    embedBuilder.setDescription(description.toString());
    return embedBuilder.build();
  }

  private MessageEmbed createConquestHelp(EmbedBuilder embedBuilder) {
    StringBuilder description = new StringBuilder();
    description.append(BotConstants.ENABLED_EMOJI).append("**Alliances**").append("\n");
    description.append(BotConstants.ENABLED_EMOJI).append("**Capitals**").append("\n");
    description.append("\n");
    description.append("**Information**").append("\n");
    description.append("If you lose your capital in this game mode, your capital will move to a random territory that you own.").append("\n");
    description.append("\n");
    description.append("This mode can be played as outlined in the basic `/help` command.").append("\n");
    embedBuilder.setDescription(description.toString());
    embedBuilder.addField(allianceControlsField);
    return embedBuilder.build();
  }

  private MessageEmbed createRegicideHelp(EmbedBuilder embedBuilder) {
    StringBuilder description = new StringBuilder();
    description.append(BotConstants.DISABLED_EMOJI).append("**Alliances**").append("\n");
    description.append(BotConstants.ENABLED_EMOJI).append("**Capitals**").append("\n");
    description.append("\n");
    description.append("**Information**").append("\n");
    description.append("You become instantly defeated in this game mode once your capital is captured. Defend it at all costs!").append("\n");
    description.append("\n");
    description.append("This mode can be played as outlined in the basic `/help` command.").append("\n");
    embedBuilder.setDescription(description.toString());
    return embedBuilder.build();
  }

  private MessageEmbed createBrawlHelp(EmbedBuilder embedBuilder) {
    StringBuilder description = new StringBuilder();
    description.append(BotConstants.ENABLED_EMOJI).append("**Alliances**").append("\n");
    description.append(BotConstants.DISABLED_EMOJI).append("**Capitals**").append("\n");
    description.append("\n");
    description.append("**Information**").append("\n");
    description.append("In this mode, all players take turns picking any unclaimed territory on the map.").append("\s");
    description.append("Once all territories are claimed, players must battle to the death!").append("\n");
    description.append("**Game Setup**").append("\n");
    description.append(BotConstants.BULLET_POINT_EMOJI + "Use `/join color:[color]` to select a color to join as.").append("\n");
    description.append(BotConstants.BULLET_POINT_EMOJI + "Use `/map name:[name]` to select a map.").append("\n");
    description.append(BotConstants.BULLET_POINT_EMOJI + "Use `/play` to start a game with the standard turn order.").append("\s");
    description.append("If you would like to select a different turn order, use `/play order:[order]`.").append("\n");
    description.append("\n");
    description.append("At this point, the selection phase has started.").append("\n");
    description.append("\n");
    description.append(BotConstants.BULLET_POINT_EMOJI + "Use `/select territory:[name]` to start selecting territories.").append("\n");
    description.append("\n");
    description.append("Once all territories have been selected, you can use `/claim` just as you would in any other game mode.").append("\n");
    description.append("\n");
    embedBuilder.setDescription(description.toString());
    embedBuilder.addField(allianceControlsField);
    return embedBuilder.build();
  }

  private MessageEmbed createCreativeHelp(EmbedBuilder embedBuilder) {
    StringBuilder description = new StringBuilder();
    description.append(BotConstants.DISABLED_EMOJI).append("**Alliances**").append("\n");
    description.append(BotConstants.DISABLED_EMOJI).append("**Capitals**").append("\n");
    description.append("\n");
    description.append("**Information**").append("\n");
    description
        .append("This game mode is for players who enjoy D&D-style games, world-building, or for those who like playing out fictional wars or alternate history through the game.")
        .append("\n");
    description.append("\n");
    description.append("In this mode, only one player can join the game with the `/join` command. This player becomes the Dungeon Master (DM).").append("\n");
    description.append("\n");
    description.append("The DM can grant or revoke territories, add and remove players from the game, and end the game at any time.").append("\s");
    description.append("Players added by the DM are dummy players that don't actually do anything on their own, and aren't controlled by anybody.").append("\s");
    description.append("This gives the DM the power to control what these dummy players do. They can represent an actual player if that is what the DM wants.").append("\s");
    description.append("While there are no alliances or capitals in the traditional sense, the DM has the power to make any territory a capital territory.").append("\s");
    description.append("Alliances can be managed by the DM manually, outside of the confines of the game. The same goes for things like dice rolls or other mechanics.")
        .append("\n");
    description.append("\n");
    description.append("**DM Controls**").append("\n");
    description.append("End Game: `/leave` OR anyone with the proper server permissions can use `/end`").append("\n");
    description.append("Add 'Player': `/dm add color:[color] name:[name]").append("\n");
    description.append("Remove 'Player': `/dm remove color:[color]`").append("\n");
    description.append("Grant Territory: `/dm grant color:[color] territories:[name1, name2, ...]`").append("\n");
    description.append("Revoke Territory: `/dm revoke color:[color] territories:[name1, name2, ...]`").append("\n");
    description.append("Change Territory Type: `/dm change territory:[names] type:[type]`").append("\n");
    description.append("\n");
    description.append("The rest is up to you! This mode was designed to allow for maximum creativity and flexibility, so have at it!").append("\n");
    embedBuilder.setDescription(description.toString());
    return embedBuilder.build();
  }


}
