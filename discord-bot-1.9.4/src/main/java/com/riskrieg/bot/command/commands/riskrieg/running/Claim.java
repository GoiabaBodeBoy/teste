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

package com.riskrieg.bot.command.commands.riskrieg.running;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.ConfigUtil;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.map.GameMap;
import com.riskrieg.core.api.nation.Nation;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.api.player.Player;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.internal.bundle.ClaimBundle;
import com.riskrieg.core.internal.bundle.UpdateBundle;
import com.riskrieg.map.territory.TerritoryId;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;

public class Claim implements Command {

  private final Settings settings;

  public Claim() {
    this.settings = new StandardSettings(
        "Claim some territory.",
        "claim", "attack", "take")
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
        .addSubcommands(
            new SubcommandData("auto", "Automatically attempt to claim random territories."),
            new SubcommandData("manual", "Specify which territories you would like to claim.")
                .addOption(OptionType.STRING, "territories", "Specify which territories you would like to claim.", true)
        );
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "Claim successfully processed."); // First message has to be ephemeral, so send this.

      // Guard clauses
      Member member = event.getMember();
      if (member == null) {
        hook.sendMessage(MessageUtil.error(settings, "Could not find member.")).queue();
        return;
      }

      Guild guild = event.getGuild();
      if (guild == null) {
        hook.sendMessage(MessageUtil.error(settings, "Invalid guild.")).queue();
        return;
      }

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {

          final TerritoryId[] territoriesToClaim;
          String subcommandName = event.getSubcommandName();
          if (subcommandName != null && subcommandName.equals("auto")) {
            territoriesToClaim = parseAuto(Identity.of(member.getId()), game.nations(), game.map());
          } else if (subcommandName != null && subcommandName.equals("manual")) {
            OptionMapping territoriesOption = event.getOption("territories");
            territoriesToClaim = (territoriesOption == null || territoriesOption.getAsString().isBlank()) ? new TerritoryId[0] : parseManual(territoriesOption.getAsString());
          } else {
            territoriesToClaim = new TerritoryId[0];
          }

          // TODO: Update core to handle empty territoriesToClaim better
          game.claim(Identity.of(member.getId()), territoriesToClaim).submit(claimBundle -> game.update().submit(updateBundle -> {

                String fileName = game.map().mapName().simpleName() + ".png";
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(settings.embedColor());
                embedBuilder.setTitle(game.map().mapName().displayName() + " | " + game.displayName());
                embedBuilder.setDescription(buildTurnDescription(claimBundle, updateBundle, game.isEnded()));
                embedBuilder.setImage("attachment://" + fileName);

                if (game.isEnded()) {
                  embedBuilder.setFooter("Thank you for playing!");

                  hook.sendMessage(genericSuccess).queue(success -> {
                    hook.sendMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                    group.deleteGame(event.getChannel().getId());
                  });
                } else {
                  String claimStr = "They may claim " + updateBundle.claims() + " " + (updateBundle.claims() == 1 ? "territory" : "territories") + " this turn.";
                  embedBuilder.setFooter("It is " + updateBundle.currentTurnPlayer().name() + "'s turn. " + claimStr);

                  if (ConfigUtil.canMention(hook)) {
                    hook.sendMessage(genericSuccess).queue(success -> {
                      ConfigUtil.sendWithMention(hook, updateBundle.currentTurnPlayer().identity().toString(), message -> {
                        message.editMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                      });
                    });
                  } else {
                    hook.sendMessage(genericSuccess).queue(success -> {
                      hook.sendMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                    });
                  }
                  group.saveGame(event.getChannel().getId(), game).submit();

                }

              }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue()
          ), failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

  private TerritoryId[] parseManual(String input) {
    if (input.isBlank()) {
      return new TerritoryId[0];
    }
    Set<TerritoryId> ids = new HashSet<>();
    String[] idStrings = input.split("[\s,|/\\\\]+");
    for (String idString : idStrings) {
      if (!idString.isBlank()) {
        ids.add(new TerritoryId(idString.toUpperCase()));
      }
    }
    return ids.toArray(new TerritoryId[0]);
  }

  private TerritoryId[] parseAuto(Identity identity, Collection<Nation> nations, GameMap gameMap) { // TODO: Sometimes returns an empty array
    Nation nation = nations.stream().filter(n -> n.identity().equals(identity)).findAny().orElse(null);
    if (nation == null) {
      return new TerritoryId[0];
    }
    List<TerritoryId> neighbors = new ArrayList<>(nation.neighbors(gameMap));
    neighbors.removeIf(tid -> nations.stream().anyMatch(n ->
        nation.allies().contains(n.identity()) && n.allies().contains(nation.identity()) && n.territories().contains(tid)
    ));
    Collections.shuffle(neighbors);
    return neighbors.subList(0, nation.getClaimAmount(gameMap, nations)).toArray(new TerritoryId[0]);
  }

  private String buildTurnDescription(ClaimBundle claimBundle, UpdateBundle updateBundle, boolean isEnded) {
    StringBuilder description = new StringBuilder();
    if (claimBundle.claimed().size() > 0) {
      description.append("**").append(updateBundle.previousPlayer().name()).append("** has claimed: ")
          .append(claimBundle.claimed().stream().map(TerritoryId::value).collect(Collectors.joining(", ")).trim()).append("\n");
    }
    if (claimBundle.taken().size() > 0) {
      description.append("**").append(updateBundle.previousPlayer().name()).append("** has taken: ")
          .append(claimBundle.taken().stream().map(TerritoryId::value).collect(Collectors.joining(", ")).trim()).append("\n");
    }
    if (claimBundle.defended().size() > 0) {
      // TODO: Say who defended what
      description.append("Territories defended: ").append(claimBundle.defended().stream().map(TerritoryId::value).collect(Collectors.joining(", ")).trim()).append("\n");
    }
    description.append("\n");
    for (Player player : updateBundle.defeated()) {
      description.append("**").append(player.name()).append("** has been defeated!").append("\n");
    }
    description.append("\n");
    if (isEnded) {
      switch (updateBundle.reason()) {
        case DEFEAT -> description.append("**").append(updateBundle.currentTurnPlayer().name()).append("** has won the game!");
        case STALEMATE -> description.append("A stalemate has been reached! The game is now over.");
        case ALLIED_VICTORY -> description.append("Allied victory! The remaining players have won the game.");
        default -> description.append("The game is now over.");
      }
    }
    return description.toString().trim();
  }

}
