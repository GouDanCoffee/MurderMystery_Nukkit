package cn.lanink.murdermystery.room.infected;

import cn.lanink.gamecore.utils.exception.RoomLoadException;
import cn.lanink.murdermystery.MurderMystery;
import cn.lanink.murdermystery.event.MurderMysteryPlayerDamageEvent;
import cn.lanink.murdermystery.event.MurderMysteryPlayerDeathEvent;
import cn.lanink.murdermystery.item.ItemManager;
import cn.lanink.murdermystery.room.base.BaseRoom;
import cn.lanink.murdermystery.room.base.PlayerIdentity;
import cn.lanink.murdermystery.tasks.Watchdog;
import cn.lanink.murdermystery.utils.Tools;
import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.Config;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 感染模式房间类
 *
 * @author lt_name
 */
public class InfectedModeRoom extends BaseRoom {

    @Getter
    private final HashMap<Player, Integer> playerRespawnTime = new HashMap<>();

    /**
     * 初始化
     *
     * @param level 世界
     * @param config 配置文件
     */
    public InfectedModeRoom(@NotNull Level level, @NotNull Config config) throws RoomLoadException {
        super(level, config);
        if (MurderMystery.debug) {
            this.minPlayers = 2;
        }
    }

    @Override
    public void initData() {
        super.initData();
        if (this.playerRespawnTime != null) {
            this.playerRespawnTime.clear();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void enableListener() {
        this.murderMystery.getMurderMysteryListeners().get("RoomLevelProtection").addListenerRoom(this);
        this.murderMystery.getMurderMysteryListeners().get("DefaultGameListener").addListenerRoom(this);
        this.murderMystery.getMurderMysteryListeners().get("DefaultChatListener").addListenerRoom(this);
        this.murderMystery.getMurderMysteryListeners().get("DefaultDamageListener").addListenerRoom(this);
        this.murderMystery.getMurderMysteryListeners().get("ClassicDamageListener").addListenerRoom(this);
        this.murderMystery.getMurderMysteryListeners().get("InfectedGameListener").addListenerRoom(this);
    }

    @Override
    public synchronized void startGame() {
        super.startGame();
        for (Player player : this.players.keySet()) {
            player.getInventory().clearAll();
            player.getUIInventory().clearAll();
            this.players.put(player, PlayerIdentity.DETECTIVE);
            player.getInventory().setItem(1, ItemManager.get(player, 1));
            player.getInventory().setItem(2, Item.get(262, 0, 16));
        }
    }

    @Override
    protected void victoryReward(int victory) {

    }

    @Override
    public void timeTask() {
        //开局20秒选出杀手
        int time = this.gameTime - (this.setGameTime - 20);
        if (time >= 0) {
            if ((time%5 == 0 && time != 0) || (time <= 5 && time != 0)) {
                for (Player player : this.getPlayers().keySet()) {
                    player.sendMessage(this.murderMystery.getLanguage(player)
                            .translateString("killerGetSwordTime").replace("%time%", time + ""));
                }
                for (Player player : this.getSpectatorPlayers()) {
                    player.sendMessage(this.murderMystery.getLanguage(player)
                            .translateString("killerGetSwordTime").replace("%time%", time + ""));
                }
                Tools.playSound(this, Sound.RANDOM_CLICK);
            }
            if (time == 0) {
                for (Player player : this.getPlayers().keySet()) {
                    player.sendMessage(this.murderMystery.getLanguage(player).translateString("killerGetSword"));
                }
                for (Player player : this.getSpectatorPlayers()) {
                    player.sendMessage(this.murderMystery.getLanguage(player).translateString("killerGetSword"));
                }
                int y = MurderMystery.RANDOM.nextInt(this.getPlayers().size());
                Player player = new ArrayList<>(this.getPlayers().keySet()).get(y);
                this.players.put(player, PlayerIdentity.KILLER);
                player.sendTitle(this.murderMystery.getLanguage(player).translateString("titleKillerTitle"),
                        this.murderMystery.getLanguage(player).translateString("titleKillerSubtitle"), 10, 40, 10);
                this.playerRespawn(player);
            }
        }

        //复活计时
        for (Map.Entry<Player, Integer> entry : this.playerRespawnTime.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
                if (entry.getValue() == 0) {
                    this.playerRespawn(entry.getKey());
                }else {
                    entry.getKey().sendTip(this.murderMystery.getLanguage(entry.getKey()).translateString("playerRespawnTime")
                            .replace("%time%", entry.getValue() + ""));
                }
            }
        }

        //杀手技能CD计算
        for (Map.Entry<Player, Integer> entry : this.killerScanCD.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }

        //计时与胜利判断
        if (this.gameTime > 0) {
            this.gameTime--;
            int playerNumber = 0;
            boolean killer = false;
            for (Map.Entry<Player, PlayerIdentity> entry : this.getPlayers().entrySet()) {
                switch (entry.getValue()) {
                    case COMMON_PEOPLE:
                    case DETECTIVE:
                        playerNumber++;
                        break;
                    case KILLER:
                        killer = true;
                        if (this.gameTime % 20 == 0) {
                            Effect effect = Effect.getEffect(1).setDuration(1000)
                                    .setAmplifier(1).setVisible(true);
                            effect.setColor(0, 255, 0);
                            entry.getKey().addEffect(effect);
                        }
                        break;
                }
            }
            if (time >= 0) {
                if (this.players.size() < 2) {
                    this.endGame();
                    return;
                }
            }
            if (killer) {
                if (playerNumber == 0) {
                    this.victory(3);
                }
            }else {
                this.victory(1);
            }
        }else {
            this.victory(1);
        }
        this.goldSpawn();
        this.goldExchange();

        Watchdog.resetTime(this);
    }

    @Override
    public void goldExchange() {
        for (Map.Entry<Player, PlayerIdentity> entry : this.players.entrySet()) {
            if (entry.getValue() == PlayerIdentity.NULL ||
                    entry.getValue() == PlayerIdentity.DEATH ||
                    entry.getValue() == PlayerIdentity.KILLER) {
                continue;
            }
            int x = 0;
            boolean needBow = true;
            for (Item item : entry.getKey().getInventory().getContents().values()) {
                if (item.getId() == 266) {
                    x += item.getCount();
                    continue;
                }
                if (item.getId() == 261) {
                    needBow = false;
                }
            }
            if (x >= 10) {
                Item gold = ItemManager.get(null, 266);
                gold.setCount(10);
                entry.getKey().getInventory().removeItem(gold);
                entry.getKey().getInventory().addItem(Item.get(262, 0, 16));
                if (needBow) {
                    entry.getKey().getInventory().addItem(Item.get(261, 0, 1));
                }
                Tools.playSound(entry.getKey(), Sound.RANDOM_LEVELUP);
            }
        }
    }

    @Override
    protected void assignIdentity() {

    }

    @Override
    public int getSurvivorPlayerNumber() {
        int x = 0;
        for (PlayerIdentity identity : this.getPlayers().values()) {
            if (identity == PlayerIdentity.DETECTIVE) {
                x++;
            }
        }
        return x;
    }

    @Override
    public void playerDamage(@NotNull Player damager, @NotNull Player player) {
        MurderMysteryPlayerDamageEvent ev = new MurderMysteryPlayerDamageEvent(this, damager, player);
        Server.getInstance().getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return;
        }
        if (this.getPlayers(damager) == PlayerIdentity.KILLER) {
            if (this.getPlayers(player) == PlayerIdentity.KILLER) {
                return;
            }
            this.players.put(player, PlayerIdentity.KILLER);
            player.sendTitle(this.murderMystery.getLanguage(player).translateString("titleKillerTitle"),
                    this.murderMystery.getLanguage(player).translateString("titleKillerSubtitle"), 10, 40, 10);
        }else {
            if (this.getPlayers(player) != PlayerIdentity.KILLER) {
                return;
            }
        }
        this.playerDeath(player);
    }

    @Override
    public void playerDeath(@NotNull Player player) {
        MurderMysteryPlayerDeathEvent ev = new MurderMysteryPlayerDeathEvent(this, player);
        Server.getInstance().getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return;
        }
        player.getInventory().clearAll();
        player.getUIInventory().clearAll();
        player.setGamemode(3);
        player.getAdventureSettings().set(AdventureSettings.Type.NO_CLIP, false);
        player.getAdventureSettings().update();
        Tools.hidePlayer(this, player);
        Tools.playSound(this, Sound.GAME_PLAYER_HURT);
        this.playerRespawnTime.put(player, 10);
    }

    public void playerRespawn(@NotNull Player player) {
        Tools.showPlayer(this, player);
        Tools.rePlayerState(player, true);
        player.getInventory().setItem(1, ItemManager.get(player, 2));
        player.getInventory().setItem(2, ItemManager.get(player, 3));
        Effect effect = Effect.getEffect(2).setAmplifier(2).setDuration(60); //缓慢
        effect.setColor(0, 255, 0);
        player.addEffect(effect);
        effect = Effect.getEffect(15).setAmplifier(2).setDuration(60); //失明
        effect.setColor(0, 255, 0);
        player.addEffect(effect);
        player.teleport(this.getRandomSpawn().get(MurderMystery.RANDOM.nextInt(this.getRandomSpawn().size())));
        Server.getInstance().getScheduler().scheduleDelayedTask(this.murderMystery, () -> {
            Effect e = Effect.getEffect(1).setDuration(1000).setAmplifier(1).setVisible(true); // 速度
            e.setColor(0, 255, 0);
            player.addEffect(e);
        }, 60, true);
    }

}
