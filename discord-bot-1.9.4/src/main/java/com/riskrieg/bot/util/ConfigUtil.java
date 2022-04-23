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
import com.riskrieg.bot.config.ServerConfig;
import java.io.IOException;
import java.util.function.Consumer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.internal.utils.PermissionUtil;

public class ConfigUtil {

  public static boolean canMention(InteractionHook hook) {
    Guild guild = hook.getInteraction().getGuild();
    if (guild != null) {
      ServerConfig config = new ServerConfig(guild.getIdLong());
      try {
        config = JacksonUtil.read(config.path(), ServerConfig.class);
      } catch (IOException ignored) {
        // Can continue using default values, save the config now though.
        try {
          JacksonUtil.write(config.path(), ServerConfig.class, config);
        } catch (IOException ignored1) {
          // Don't worry about if it doesn't save, doesn't really matter.
        }
      }

      if (config != null && config.mentionOnTurn()) {
        return switch (hook.getInteraction().getChannelType()) {
          default -> false;
          case GUILD_PUBLIC_THREAD, GUILD_PRIVATE_THREAD -> true;
          case TEXT -> PermissionUtil.checkPermission(hook.getInteraction().getTextChannel(), guild.getSelfMember(), Permission.VIEW_CHANNEL);
        };
      }

    }
    return false;
  }

  public static void sendWithMention(InteractionHook hook, String memberId) {
    sendWithMention(hook, memberId, message -> {
    });
  }

  public static void sendWithMention(InteractionHook hook, String memberId, Consumer<Message> consumer) {
    if (canMention(hook)) {
      Guild guild = hook.getInteraction().getGuild();
      if (guild != null) {
        guild.retrieveMemberById(memberId).queue(member -> {
          if (member != null) {
            hook.setEphemeral(false).sendMessage(member.getAsMention() + " it is your turn.").queue(consumer);
          }
        });
      }
    }
  }

  public static void sendMentionIfEnabled(InteractionHook hook, String memberId) {
    Guild guild = hook.getInteraction().getGuild();
    if (guild != null && canMention(hook)) {
      guild.retrieveMemberById(memberId).queue(member -> {
        if (member != null) {
          hook.setEphemeral(false).sendMessage(member.getAsMention() + " it is your turn.").queue();
        }
      });
    }
  }

}
