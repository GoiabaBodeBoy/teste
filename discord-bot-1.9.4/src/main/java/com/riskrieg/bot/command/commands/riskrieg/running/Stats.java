package com.riskrieg.bot.command.commands.riskrieg.running;

import com.riskrieg.bot.BotConstants;
import com.riskrieg.bot.command.Command;
import com.riskrieg.bot.command.settings.Settings;
import com.riskrieg.bot.command.settings.StandardSettings;
import com.riskrieg.bot.util.MessageUtil;
import com.riskrieg.bot.util.OptionDataUtil;
import com.riskrieg.bot.util.ParseUtil;
import com.riskrieg.core.api.Riskrieg;
import com.riskrieg.core.api.RiskriegBuilder;
import com.riskrieg.core.api.gamemode.AlliableMode;
import com.riskrieg.core.api.gamemode.GameState;
import com.riskrieg.core.api.nation.Nation;
import com.riskrieg.core.api.player.Player;
import com.riskrieg.core.constant.Colors;
import com.riskrieg.core.constant.Constants;
import com.riskrieg.core.constant.color.PlayerColor;
import java.nio.file.Path;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class Stats implements Command {

  private final Settings settings;

  public Stats() {
    this.settings = new StandardSettings(
        "Show statistics about current players.",
        "stats")
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
        .addOptions(OptionDataUtil.colors().setRequired(false));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply(true).queue(hook -> {

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

      OptionMapping rgbOpt = event.getOption("color");
      final PlayerColor chosenColor = rgbOpt == null ? null : ParseUtil.parsePlayerColor(rgbOpt.getAsString());

      // Command execution
      Riskrieg api = RiskriegBuilder.create(Path.of(BotConstants.SAVE_PATH)).build();
      api.retrieveGroupById(guild.getId()).submit(group -> {
        group.retrieveGameById(event.getChannel().getId()).submit(game -> {
          if (game.gameState().equals(GameState.SELECTION) || game.gameState().equals(GameState.RUNNING)) {
            if (chosenColor == null) { // General stats
              // TODO: Implement
              hook.sendMessage(MessageUtil.error(settings, "This isn't implemented yet. Please select a color.")).queue();
            } else { // Specific player stats
              Player player = game.getPlayer(chosenColor);
              Nation nation = game.getNation(chosenColor);
              if (player != null && nation != null) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(chosenColor.value());
                embedBuilder.setTitle(player.name() + " | " + Constants.NAME + " Statistics");
                embedBuilder.addField("Territories", nation.territories().size() + "", true);
                if (game instanceof AlliableMode) {
                  embedBuilder.addField("Allies", nation.allies().size() + "", true);
                }
                hook.sendMessageEmbeds(embedBuilder.build()).queue();
              } else {
                hook.sendMessage(MessageUtil.error(settings, "No player with that color could be found.")).queue();
              }
            }
          } else {
            hook.sendMessage(MessageUtil.error(settings, "The game must be in an active state to use this command.")).queue();
          }
        }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());
      }, failure -> hook.sendMessage(MessageUtil.error(settings, failure.getMessage())).queue());

    });
  }

}
