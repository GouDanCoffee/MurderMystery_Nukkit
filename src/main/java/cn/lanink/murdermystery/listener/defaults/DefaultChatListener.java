package cn.lanink.murdermystery.listener.defaults;

import cn.lanink.murdermystery.listener.BaseMurderMysteryListener;
import cn.lanink.murdermystery.room.base.BaseRoom;
import cn.lanink.murdermystery.room.base.PlayerIdentity;
import cn.lanink.murdermystery.room.base.RoomStatus;
import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;

/**
 * @author lt_name
 */
@SuppressWarnings("unused")
public class DefaultChatListener extends BaseMurderMysteryListener<BaseRoom> {

    /**
     * 玩家执行命令事件
     * @param event 事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (player == null || message == null) {
            return;
        }
        message = message.replace("/", "").split(" ")[0];
        BaseRoom room = this.getListenerRooms().get(player.getLevel().getFolderName());
        if (room == null || !room.isPlaying(player)) {
            return;
        }
        if (this.murderMystery.getCmdUser().equalsIgnoreCase(message) ||
                this.murderMystery.getCmdAdmin().equalsIgnoreCase(message)) {
            return;
        }
        for (String string : this.murderMystery.getCmdUserAliases()) {
            if (string.equalsIgnoreCase(message)) {
                return;
            }
        }
        for (String string : this.murderMystery.getCmdAdminAliases()) {
            if (string.equalsIgnoreCase(message)) {
                return;
            }
        }
        for (String string : this.murderMystery.getCmdWhitelist()) {
            if (string.equalsIgnoreCase(message)) {
                return;
            }
        }
        event.setMessage("");
        event.setCancelled(true);
        player.sendMessage(this.murderMystery.getLanguage(player)
                .translateString("useCmdInRoom"));
    }

    /**
     * 发送消息事件
     * @param event 事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (player == null || message == null) {
            return;
        }
        BaseRoom room = this.getListenerRooms().get(player.getLevel().getFolderName());
        if (room == null || (!room.isPlaying(player) && !room.isSpectator(player))) {
            for (BaseRoom r : this.murderMystery.getRooms().values()) {
                for (Player p : r.getPlayers().keySet()) {
                    event.getRecipients().remove(p);
                }
                for (Player p : r.getSpectatorPlayers()) {
                    event.getRecipients().remove(p);
                }
            }
            return;
        }
        if (room.isSpectator(player)) {
            for (Player p : room.getSpectatorPlayers()) {
                p.sendMessage(this.murderMystery.getLanguage(p).translateString("playerSpectatorChat")
                        .replace("%player%", player.getName())
                        .replace("%message%", message));
            }
        }else if (room.getPlayers(player) == PlayerIdentity.DEATH) {
            HashSet<Player> players = new HashSet<>(room.getPlayers().keySet());
            players.addAll(room.getSpectatorPlayers());
            for (Player p : players) {
                if (room.getPlayers(p) == PlayerIdentity.DEATH) {
                    if (room.getStatus() == RoomStatus.GAME) {
                        p.sendMessage(this.murderMystery.getLanguage(p).translateString("playerDeathChat")
                                .replace("%player%", player.getName())
                                .replace("%message%", message));
                    }else {
                        p.sendMessage(this.murderMystery.getLanguage(p).translateString("playerChat")
                                .replace("%player%", player.getName())
                                .replace("%message%", message));
                    }
                }
            }
        }else {
            for (Player p : room.getPlayers().keySet()) {
                p.sendMessage(this.murderMystery.getLanguage(p).translateString("playerChat")
                        .replace("%player%", player.getName())
                        .replace("%message%", message));
            }
            for (Player p : room.getSpectatorPlayers()) {
                p.sendMessage(this.murderMystery.getLanguage(p).translateString("playerChat")
                        .replace("%player%", player.getName())
                        .replace("%message%", message));
            }
        }
        event.setMessage("");
        event.setCancelled(true);
    }

}
