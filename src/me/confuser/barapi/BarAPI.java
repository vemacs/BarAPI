package me.confuser.barapi;

import java.util.HashMap;
import java.util.UUID;

import me.confuser.barapi.nms.FakeDragon;
import me.confuser.barapi.nms.FakeWither;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Allows plugins to safely set a health bar message.
 * 
 * @author James Mortemore
 */

public class BarAPI extends JavaPlugin implements Listener {
    private static HashMap<UUID, FakeDragon> dragon_players = new HashMap<UUID, FakeDragon>();
    private static HashMap<UUID, FakeWither> wither_players = new HashMap<UUID, FakeWither>();
    private static HashMap<UUID, Integer> timers = new HashMap<UUID, Integer>();

    private static BarAPI plugin;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Loaded");

        plugin = this;

        // TestMode
        if (getConfig().getBoolean("testMode")) {
            plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

                @Override
                public void run() {
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        BarAPI.setMessage(player, ChatColor.AQUA + "Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI Testing BarAPI", 10);
                    }
                }

            }, 30L, 300L);
        }

        // Continuously fix for 1.8 clients
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : getServer().getOnlinePlayers()) {
                    if (Util.hasNewProtocol(p)) {
                        handleTeleport(p, p.getLocation());
                    }
                }
            }
        }.runTaskTimer(this, 0, 10);
    }

    @Override
    public void onDisable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            quit(player);
        }

        dragon_players.clear();
        wither_players.clear();

        for (int timerID : timers.values()) {
            Bukkit.getScheduler().cancelTask(timerID);
        }

        timers.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void PlayerLoggout(PlayerQuitEvent event) {
        quit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        quit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        handleTeleport(event.getPlayer(), event.getTo().clone());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerRespawnEvent event) {
        handleTeleport(event.getPlayer(), event.getRespawnLocation().clone());
    }

    private void handleTeleport(final Player player, final Location loc) {

        if (!hasDragonBar(player) && !hasWitherBar(player))
            return;

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                // Check if the player still has a dragon after the two ticks! ;)
                if (!hasDragonBar(player) && !hasWitherBar(player))
                    return;

                if(dragon_players.containsKey(player.getUniqueId())){
                    FakeDragon oldDragon = getDragon(player, "");

                    float health = oldDragon.health;
                    String message = oldDragon.name;

                    Util.sendPacket(player, getDragon(player, "").getDestroyPacket());

                    dragon_players.remove(player.getUniqueId());

                    FakeDragon dragon = addDragon(player, loc, message);
                    dragon.health = health;

                    sendDragon(dragon, player);
                } else if(wither_players.containsKey(player.getUniqueId())) {
                    FakeWither oldWither = getWither(player, "");

                    float health = oldWither.health;
                    String message = oldWither.name;

                    Util.sendPacket(player, getWither(player, "").getDestroyPacket());

                    dragon_players.remove(player.getUniqueId());

                    FakeWither wither = addWither(player, loc, message);
                    wither.health = health;

                    sendWither(wither, player);
                }
            }

        }, 2L);
    }

    private void quit(Player player) {
        removeBar(player);
    }

    /**
     * Set a message for all players.<br>
     * It will remain there until the player logs off or another plugin overrides it.<br>
     * This method will show a full health bar and will cancel any running timers.
     * 
     * @param message
     *            The message shown.<br>
     *            Due to limitations in Minecraft this message cannot be longer than 64 characters.<br>
     *            It will be cut to that size automatically.
     * @see BarAPI#setMessage(player, message)
     */
    public static void setMessage(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setMessage(player, message);
        }
    }

    /**
     * Set a message for the given player.<br>
     * It will remain there until the player logs off or another plugin overrides it.<br>
     * This method will show a full health bar and will cancel any running timers.
     * 
     * @param player
     *            The player who should see the given message.
     * @param message
     *            The message shown to the player.<br>
     *            Due to limitations in Minecraft this message cannot be longer than 64 characters.<br>
     *            It will be cut to that size automatically.
     */
    public static void setMessage(Player player, String message) {
        if(Util.hasNewProtocol(player)){
            FakeWither wither = getWither(player, message);

            wither.name = cleanMessage(message);
            wither.health = FakeWither.MAX_HEALTH;

            cancelTimer(player);

            sendWither(wither, player);
        } else {
            FakeDragon dragon = getDragon(player, message);

            dragon.name = cleanMessage(message);
            dragon.health = FakeDragon.MAX_HEALTH;

            cancelTimer(player);

            sendDragon(dragon, player);
        }
    }

    /**
     * Set a message for all players.<br>
     * It will remain there for each player until the player logs off or another plugin overrides it.<br>
     * This method will show a health bar using the given percentage value and will cancel any running timers.
     * 
     * @param message
     *            The message shown to the player.<br>
     *            Due to limitations in Minecraft this message cannot be longer than 64 characters.<br>
     *            It will be cut to that size automatically.
     * @param percent
     *            The percentage of the health bar filled.<br>
     *            This value must be between 0F (inclusive) and 100F (inclusive).
     * @throws IllegalArgumentException
     *             If the percentage is not within valid bounds.
     * @see BarAPI#setMessage(player, message, percent)
     */
    public static void setMessage(String message, float percent) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setMessage(player, message, percent);
        }
    }

    /**
     * Set a message for the given player.<br>
     * It will remain there until the player logs off or another plugin overrides it.<br>
     * This method will show a health bar using the given percentage value and will cancel any running timers.
     * 
     * @param player
     *            The player who should see the given message.
     * @param message
     *            The message shown to the player.<br>
     *            Due to limitations in Minecraft this message cannot be longer than 64 characters.<br>
     *            It will be cut to that size automatically.
     * @param percent
     *            The percentage of the health bar filled.<br>
     *            This value must be between 0F (inclusive) and 100F (inclusive).
     * @throws IllegalArgumentException
     *             If the percentage is not within valid bounds.
     */
    public static void setMessage(Player player, String message, float percent) {
        Validate.isTrue(0F <= percent && percent <= 100F, "Percent must be between 0F and 100F, but was: ", percent);

        if(Util.hasNewProtocol(player)){
            FakeWither wither = getWither(player, message);

            wither.name = cleanMessage(message);
            wither.health = (percent / 100f) * FakeWither.MAX_HEALTH;

            cancelTimer(player);

            sendWither(wither, player);
        } else {
            FakeDragon dragon = getDragon(player, message);

            dragon.name = cleanMessage(message);
            dragon.health = (percent / 100f) * FakeDragon.MAX_HEALTH;

            cancelTimer(player);

            sendDragon(dragon, player);
        }
    }

    /**
     * Set a message for all players.<br>
     * It will remain there for each player until the player logs off or another plugin overrides it.<br>
     * This method will use the health bar as a decreasing timer, all previously started timers will be cancelled.<br>
     * The timer starts with a full bar.<br>
     * The health bar will be removed automatically if it hits zero.
     * 
     * @param message
     *            The message shown.<br>
     *            Due to limitations in Minecraft this message cannot be longer than 64 characters.<br>
     *            It will be cut to that size automatically.
     * @param seconds
     *            The amount of seconds displayed by the timer.<br>
     *            Supports values above 1 (inclusive).
     * @throws IllegalArgumentException
     *             If seconds is zero or below.
     * @see BarAPI#setMessage(player, message, seconds)
     */
    public static void setMessage(String message, int seconds) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            setMessage(player, message, seconds);
        }
    }

    /**
     * Set a message for the given player.<br>
     * It will remain there until the player logs off or another plugin overrides it.<br>
     * This method will use the health bar as a decreasing timer, all previously started timers will be cancelled.<br>
     * The timer starts with a full bar.<br>
     * The health bar will be removed automatically if it hits zero.
     * 
     * @param player
     *            The player who should see the given timer/message.
     * @param message
     *            The message shown to the player.<br>
     *            Due to limitations in Minecraft this message cannot be longer than 64 characters.<br>
     *            It will be cut to that size automatically.
     * @param seconds
     *            The amount of seconds displayed by the timer.<br>
     *            Supports values above 1 (inclusive).
     * @throws IllegalArgumentException
     *             If seconds is zero or below.
     */
    public static void setMessage(final Player player, String message, int seconds) {
        Validate.isTrue(seconds > 0, "Seconds must be above 1 but was: ", seconds);

        if(Util.hasNewProtocol(player)){
            FakeWither wither = getWither(player, message);

            wither.name = cleanMessage(message);
            wither.health = FakeWither.MAX_HEALTH;

            final float witherHealthMinus = FakeWither.MAX_HEALTH / seconds;

            cancelTimer(player);

            timers.put(player.getUniqueId(), Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {

                @Override
                public void run() {
                    FakeWither with = getWither(player, "");
                    with.health -= witherHealthMinus;

                    if (with.health <= 1) {
                        removeBar(player);
                        cancelTimer(player);
                    } else {
                        sendWither(with, player);
                    }
                }

            }, 20L, 20L).getTaskId());

            sendWither(wither, player);
        } else {
            FakeDragon dragon = getDragon(player, message);

            dragon.name = cleanMessage(message);
            dragon.health = FakeDragon.MAX_HEALTH;

            final float dragonHealthMinus = FakeDragon.MAX_HEALTH / seconds;

            cancelTimer(player);

            timers.put(player.getUniqueId(), Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {

                @Override
                public void run() {
                    FakeDragon drag = getDragon(player, "");
                    drag.health -= dragonHealthMinus;

                    if (drag.health <= 1) {
                        removeBar(player);
                        cancelTimer(player);
                    } else {
                        sendDragon(drag, player);
                    }
                }

            }, 20L, 20L).getTaskId());

            sendDragon(dragon, player);
        }
    }

    /**
     * Checks whether the given player has a bar.
     * 
     * @param player
     *            The player who should be checked.
     * @return True, if the player has a bar, False otherwise.
     */
    @Deprecated
    public static boolean hasBar(Player player){
        return hasDragonBar(player) || hasWitherBar(player);
    }
    
    public static boolean hasDragonBar(Player player) {
        return dragon_players.get(player.getUniqueId()) != null;
    }

    public static boolean hasWitherBar(Player player) {
        return wither_players.get(player.getUniqueId()) != null;
    }


    /**
     * Removes the bar from the given player.<br>
     * If the player has no bar, this method does nothing.
     * 
     * @param player
     *            The player whose bar should be removed.
     */
    public static void removeBar(Player player) {
        if (!hasDragonBar(player) && !hasWitherBar(player))
            return;

        if(hasDragonBar(player)){
            Util.sendPacket(player, getDragon(player, "").getDestroyPacket());
            dragon_players.remove(player.getUniqueId());
            cancelTimer(player);
        } else if(hasWitherBar(player)){
            Util.sendPacket(player, getWither(player, "").getDestroyPacket());
            wither_players.remove(player.getUniqueId());
            cancelTimer(player);
        }
    }

    /**
     * Modifies the health of an existing bar.<br>
     * If the player has no bar, this method does nothing.
     * 
     * @param player
     *            The player whose bar should be modified.
     * @param percent
     *            The percentage of the health bar filled.<br>
     *            This value must be between 0F and 100F (inclusive).
     */
    public static void setHealth(Player player, float percent) {
        if (!hasDragonBar(player) && !hasWitherBar(player))
            return;

        if(hasDragonBar(player)){
            FakeDragon dragon = getDragon(player, "");
            dragon.health = (percent / 100f) * FakeDragon.MAX_HEALTH;

            cancelTimer(player);

            if (percent == 0) {
                removeBar(player);
            } else {
                sendDragon(dragon, player);
            }
        } else if(hasWitherBar(player)){
            FakeWither wither = getWither(player, "");
            wither.health = (percent / 100f) * FakeWither.MAX_HEALTH;

            cancelTimer(player);

            if (percent == 0) {
                removeBar(player);
            } else {
                sendWither(wither, player);
            }
        }
    }

    /**
     * Get the health of an existing bar.
     * 
     * @param player
     *            The player whose bar's health should be returned.
     * @return The current absolute health of the bar.<br>
     *         If the player has no bar, this method returns -1.
     */
    public static float getHealth(Player player) {
        if (!hasDragonBar(player) && !hasWitherBar(player))
            return -1;

        if(hasDragonBar(player))
            return getDragon(player, "").health;
        else if(hasWitherBar(player))
            return getWither(player, "").health;
        
        return -1;
    }

    /**
     * Get the message of an existing bar.
     * 
     * @param player
     *            The player whose bar's message should be returned.
     * @return The current message displayed to the player.<br>
     *         If the player has no bar, this method returns an empty string.
     */
    public static String getMessage(Player player) {
        if (!hasDragonBar(player) && !hasWitherBar(player))
            return "";

        if(hasDragonBar(player))
            return getDragon(player, "").name;
        else if(hasWitherBar(player))
            return getWither(player, "").name;
        
        return "";
    }

    private static String cleanMessage(String message) {
        if (message.length() > 64)
            message = message.substring(0, 63);

        return message;
    }

    private static void cancelTimer(Player player) {
        Integer timerID = timers.remove(player.getUniqueId());

        if (timerID != null) {
            Bukkit.getScheduler().cancelTask(timerID);
        }
    }

    private static void sendDragon(FakeDragon dragon, Player player) {
        Util.sendPacket(player, dragon.getMetaPacket(dragon.getWatcher()));

        if(Util.hasNewProtocol(player)){
            Util.sendPacket(player, dragon.getTeleportPacket(player.getLocation().add(player.getEyeLocation().getDirection().multiply(20))));
        } else {
            Util.sendPacket(player, dragon.getTeleportPacket(player.getLocation().add(0, -300, 0)));
        }
    }

    private static void sendWither(FakeWither wither, Player player) {
        Util.sendPacket(player, wither.getMetaPacket(wither.getWatcher()));

        if(Util.hasNewProtocol(player)){
            Util.sendPacket(player, wither.getTeleportPacket(player.getLocation().add(player.getEyeLocation().getDirection().multiply(20))));
        } else {
            Util.sendPacket(player, wither.getTeleportPacket(player.getLocation().add(0, -300, 0)));
        }
    }

    private static FakeDragon getDragon(Player player, String message) {
        if (dragon_players.containsKey(player.getUniqueId())) {
            return dragon_players.get(player.getUniqueId());
        } else {
            return addDragon(player, cleanMessage(message));
        }
    }

    private static FakeWither getWither(Player player, String message) {
        if (wither_players.containsKey(player.getUniqueId())) {
            return wither_players.get(player.getUniqueId());
        } else {
            return addWither(player, cleanMessage(message));
        }
    }

    private static FakeWither addWither(Player player, String message) {
        FakeWither wither = null;
        wither = Util.newWither(message,player.getLocation().add(player.getEyeLocation().getDirection().multiply(20)));
        Util.sendPacket(player, wither.getSpawnPacket());
        wither_players.put(player.getUniqueId(), wither);
        return wither;
    }

    private static FakeWither addWither(Player player, Location loc, String message) {
        FakeWither wither = null;
        // loc.add ?
        wither = Util.newWither(message,player.getLocation().add(player.getEyeLocation().getDirection().multiply(20)));
        Util.sendPacket(player, wither.getSpawnPacket());
        wither_players.put(player.getUniqueId(), wither);
        return wither;
    }

    private static FakeDragon addDragon(Player player, String message) {
        boolean ver_1_8 = Util.hasNewProtocol(player) ? true : false;
        FakeDragon dragon = null;
        FakeWither wither = null;

        if(ver_1_8){
            wither = Util.newWither(message,player.getLocation().add(player.getEyeLocation().getDirection().multiply(20)));
            Util.sendPacket(player, wither.getSpawnPacket());
            wither_players.put(player.getUniqueId(), wither);
        } else {
            dragon = Util.newDragon(message, player.getLocation().add(0, -300, 0));
            Util.sendPacket(player, dragon.getSpawnPacket());
            dragon_players.put(player.getUniqueId(), dragon);
        }

        return dragon;
    }

    private static FakeDragon addDragon(Player player, Location loc, String message) {
        boolean ver_1_8 = Util.hasNewProtocol(player) ? true : false;
        FakeDragon dragon = null;
        FakeWither wither = null;

        if(ver_1_8){
            wither = Util.newWither(message,player.getLocation().add(player.getEyeLocation().getDirection().multiply(20)));
            Util.sendPacket(player, wither.getSpawnPacket());
            wither_players.put(player.getUniqueId(), wither);
        } else {
            dragon = Util.newDragon(message, player.getLocation().add(0, -300, 0));
            Util.sendPacket(player, dragon.getSpawnPacket());
            dragon_players.put(player.getUniqueId(), dragon);
        }

        return dragon;
    }
}
