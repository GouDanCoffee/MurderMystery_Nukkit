package cn.lanink.murdermystery.room.classic;

import cn.lanink.gamecore.utils.exception.RoomLoadException;
import cn.lanink.murdermystery.room.base.BaseRoom;
import cn.lanink.murdermystery.room.base.PlayerIdentity;
import cn.lanink.murdermystery.utils.Tools;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Level;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 经典模式房间类
 *
 * @author lt_name
 */
public class ClassicModeRoom extends BaseRoom {

    /**
     * 初始化
     *
     * @param level 世界
     * @param config 配置文件
     */
    public ClassicModeRoom(@NotNull Level level, @NotNull Config config) throws RoomLoadException {
        super(level, config);
        //经典模式人数低于三将进入死循环！
        if (minPlayers < 3) {
            this.minPlayers = 3;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void enableListener() {
        super.enableListener();
        this.murderMystery.getMurderMysteryListeners().get("ClassicGameListener").addListenerRoom(this);
    }

    @Override
    protected void victoryReward(int victory) {
        if (victory == 0) {
            return;
        }
        Player killerVictory = null;
        Set<Player> commonPeopleVictory = new HashSet<>();
        Set<Player> defeatPlayers = new HashSet<>();
        for (Map.Entry<Player, PlayerIdentity> entry : this.getPlayers().entrySet()) {
            if (victory == 3) {
                if (entry.getValue() == PlayerIdentity.KILLER) {
                    killerVictory = entry.getKey();
                }else {
                    defeatPlayers.add(entry.getKey());
                }
            }else {
                switch (entry.getValue()) {
                    case COMMON_PEOPLE:
                    case DETECTIVE:
                        commonPeopleVictory.add(entry.getKey());
                        break;
                    default:
                        defeatPlayers.add(entry.getKey());
                        break;
                }
            }
        }
        //延迟执行，防止给物品被清
        final Player finalKillKillerPlayer = this.killKillerPlayer;
        final Player finalKillerVictory = killerVictory;
        Server.getInstance().getScheduler().scheduleDelayedTask(this.murderMystery, new Task() {
            @Override
            public void onRun(int i) {
                if (finalKillKillerPlayer != null) {
                    Tools.cmd(finalKillKillerPlayer, murderMystery.getConfig().getStringList("killKillerCmd"));
                }
                if (finalKillerVictory != null) {
                    Tools.cmd(finalKillerVictory, murderMystery.getConfig().getStringList("killerVictoryCmd"));
                }
                for (Player player : commonPeopleVictory) {
                    Tools.cmd(player, murderMystery.getConfig().getStringList("commonPeopleVictoryCmd"));
                }
                for (Player player : defeatPlayers) {
                    Tools.cmd(player, murderMystery.getConfig().getStringList("defeatCmd"));
                }
            }
        }, 20);
    }

}
