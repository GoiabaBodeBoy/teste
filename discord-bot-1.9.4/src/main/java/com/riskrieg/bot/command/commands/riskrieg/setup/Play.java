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
import com.riskrieg.bot.util.ConfigUtil;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.bot.util.RiskriegUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.order.FullRandomOrder;
import com.riskrieg.core.api.order.StandardOrder;
import com.riskrieg.core.api.order.StandardRandomizedOrder;
import com.riskrieg.core.api.order.StandardReversedOrder;
import com.riskrieg.core.api.order.StandardReversedRandomizedOrder;
import com.riskrieg.core.api.order.TurnOrder;
import com.riskrieg.core.api.player.Identity;
import com.riskrieg.core.constant.Colors;
import java.nio.file.Path;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;

public class Play implements Command {

  private final Settings settings;

  public Play() {
    this.settings = new StandardSettings(
        "Start the game.",
        "play", "begin", "start")
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
        .addOptions(OptionDataUtil.turnOrders());
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

      Message genericSuccess = MessageUtil.success(settings, "The game has been started."); // First message has to be ephemeral, so send this.

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

      OptionMapping turnOrderOption = event.getOption("order");
      final TurnOrder turnOrder = turnOrderOption == null ? new StandardRandomizedOrder() : switch (turnOrderOption.getAsString()) {
        case "standard-reversed" -> new StandardReversedOrder();
        case "standard-randomized" -> new StandardRandomizedOrder();
        case "standard-reversed-randomized" -> new StandardReversedRandomizedOrder();
        case "full-random" -> new FullRandomOrder();
        default -> new StandardOrder();
      };

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {
          // TODO: Add player ID to start method so that this check isn't necessary
          if (game.players().stream().anyMatch(player -> player.identity().equals(Identity.of(member.getId())))) {
            game.start(turnOrder).submit(player -> {

              String desc = "Turn order: **" + turnOrder.displayName() + "**.\n*" + turnOrder.description() + "*" + "\n\n" + "A **" + game.displayName() + "** game has begun!";
              String fileName = game.map().mapName().simpleName() + ".png";

              EmbedBuilder embedBuilder = new EmbedBuilder();
              embedBuilder.setColor(settings.embedColor());
              embedBuilder.setTitle(game.map().mapName().displayName());
              embedBuilder.setDescription(desc);
              embedBuilder.setFooter("It is " + player.name() + "'s turn.");
              embedBuilder.setImage("attachment://" + fileName);

              if (ConfigUtil.canMention(hook)) {
                hook.sendMessage(genericSuccess).queue(success -> {
                  ConfigUtil.sendWithMention(hook, player.identity().toString(), message -> {
                    message.editMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                  });
                });
              } else {
                hook.sendMessage(genericSuccess).queue(success -> {
                  hook.sendMessageEmbeds(embedBuilder.build()).addFile(RiskriegUtil.constructMapImageData(game), fileName, new AttachmentOption[0]).queue();
                });
              }
              group.saveGame(event.getChannel().getId(), game).submit();

            }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
          } else {
            hook.sendMessage(MessageUtil.error(settings, "The game can only be started by players in the game.")).queue();
          }
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
