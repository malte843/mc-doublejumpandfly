package de.malte.doublejumpfly;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Main extends JavaPlugin implements Listener, CommandExecutor {
    private ArrayList<Player> flying = new ArrayList<>();
    private ArrayList<Player> cooldown = new ArrayList<>();
    private final String prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
    private final String msgNoPerm = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgNoPerm")).replace("{0}", prefix);
    private final String msgFlyOnSelf = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgFlyOnSelf")).replace("{0}", prefix);
    private final String msgFlyOffSelf = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgFlyOffSelf")).replace("{0}", prefix);
    private final String msgFlyOnOther = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgFlyOnOther")).replace("{0}", prefix);
    private final String msgFlyOffOther = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgFlyOffOther")).replace("{0}", prefix);
    private final String msgFlyOnByOther = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgFlyOnByOther")).replace("{0}", prefix);
    private final String msgFlyOffByOther = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgFlyOffByOther")).replace("{0}", prefix);
    private final String msgPlayerNotOnline = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgPlayerNotOnline").replace("{0}", prefix));
    private final String msgCantInteractWithYourself = ChatColor.translateAlternateColorCodes('&', getConfig().getString("msgCantInteractWithYourself").replace("{0}", prefix));
    private final boolean doubleJumpEnabled = getConfig().getBoolean("doubleJumpEnabled");
    private final boolean doubleJumpSound = getConfig().getBoolean("doubleJumpSound.enabled");
    private final boolean doubleJumpPermissionEnabled = getConfig().getBoolean("doubleJumpEnabled");
    private final String doubleJumpPermission = getConfig().getString("doubleJumpPermission");
    private final String flyPermissionSelf = getConfig().getString("flyPermissionSelf");
    private final String flyPermissionOther = getConfig().getString("flyPermissionOther");
    private final List<String> doubleJumpDisabledWorlds = getConfig().getStringList("doubleJumpDisbledWorlds");
    private final double soundVolume = getConfig().getDouble("doubleJumpSound.volume");
    private final double soundPitch = getConfig().getDouble("doubleJumpSound.pitch");
    private final double doubleJumpStrength = getConfig().getDouble("doubleJumpStrength");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("fly").setExecutor(this);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        if (args.length == 0) {
            if (p.hasPermission(flyPermissionSelf)) {
                if (flying.contains(p)) {
                    flying.remove(p);
                    p.sendMessage(msgFlyOffSelf);
                } else {
                    flying.add(p);
                    p.setAllowFlight(true);
                    p.sendMessage(msgFlyOnSelf);
                }
            } else {
                p.sendMessage(msgNoPerm);
            }
        } else {
            if (p.hasPermission(flyPermissionOther)) {
                Player t = Bukkit.getPlayer(args[0]);
                if (t == null) {
                    p.sendMessage(msgPlayerNotOnline.replace("{1}", args[0]));
                    return true;
                }
                if (t.getUniqueId() == p.getUniqueId()) {
                    p.sendMessage(msgCantInteractWithYourself);
                    return true;
                }
                if (flying.contains(t)) {
                    flying.remove(t);
                    t.sendMessage(msgFlyOffByOther.replace("{1}", p.getName()));
                    p.sendMessage(msgFlyOffOther.replace("{1}", t.getName()));
                } else {
                    flying.add(t);
                    t.setAllowFlight(true);
                    t.sendMessage(msgFlyOnByOther.replace("{1}", p.getName()));
                    p.sendMessage(msgFlyOnOther.replace("{1}", t.getName()));
                }
            } else {
                p.sendMessage(msgNoPerm);
            }
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if ((!doubleJumpPermissionEnabled || event.getPlayer().hasPermission(doubleJumpPermission)) && doubleJumpEnabled) event.getPlayer().setAllowFlight(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if ((event.getPlayer().getGameMode() == GameMode.SURVIVAL || event.getPlayer().getGameMode() == GameMode.ADVENTURE) && !flying.contains(event.getPlayer())) {
            event.setCancelled(true);
            if (cooldown.contains(event.getPlayer())) return;
            event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().setY(doubleJumpStrength));
            if (doubleJumpSound) event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_CAT_HISS, (float) soundVolume, (float) soundPitch);
            cooldown.add(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        event.getPlayer().setAllowFlight(flying.contains(event.getPlayer()));
        if (event.getPlayer().isOnGround()) cooldown.remove(event.getPlayer());
        if (!doubleJumpDisabledWorlds.contains(event.getPlayer().getWorld().getName()) && doubleJumpEnabled && (!doubleJumpPermissionEnabled || event.getPlayer().hasPermission(doubleJumpPermission)))
            event.getPlayer().setAllowFlight(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.CREATIVE && (!flying.contains(event.getPlayer()))) flying.add(event.getPlayer());
        if ((event.getNewGameMode() == GameMode.SURVIVAL || event.getNewGameMode() == GameMode.ADVENTURE) && !flying.contains(event.getPlayer()) && !doubleJumpDisabledWorlds.contains(event.getPlayer().getWorld().getName()) && doubleJumpEnabled)
            if (!doubleJumpPermissionEnabled || event.getPlayer().hasPermission(doubleJumpPermission)) event.getPlayer().setAllowFlight(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        flying.remove(event.getPlayer());
    }

    private void debug (String... args) {
        boolean debugmode = false;
        if (!debugmode) return;
        String msg = "";
        for (String s : args) {
            msg += s + " ";
        }
        Bukkit.getConsoleSender().sendMessage(prefix + msg);
    }
}
