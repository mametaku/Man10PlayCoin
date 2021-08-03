package red.man10.man10playcoin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.sql.Time;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class Man10PlayCoin extends JavaPlugin implements Listener {

    boolean mode;
    int time;
    ItemStack item;
    Map<Player, Long> playerandtime = new HashMap<>();//GUIにいれたアイテム数をプレイヤーごとに管理する

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        // config.ymlが存在しない場合はファイルに出力します。
        saveDefaultConfig();
        // config.ymlを読み込みます。
        FileConfiguration config = getConfig();
        reloadConfig();
        getCommand("mplaycoin").setExecutor(this);
        if (!config.getBoolean("mode")) {
            getLogger().info("Man10PlayCoin is not run.");
            mode = false;
        } else {
            getLogger().info("Man10PlayCoin is run.");
            getServer().getScheduler().scheduleSyncRepeatingTask((Plugin)this, new Runnable() {
                public void run() {
                    givecoin();
                }
            },  0L, 100L);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (mode == false) return true;

        Player p = (Player) sender;
        if (!p.hasPermission("mplaycoin.use")) {
            p.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§a§l ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝");
            p.sendMessage("§a§l                   [Man10PlayCoin]                   ");
            p.sendMessage("§a§l      /mplaycoin register   手持ちのアイテムを設定    ");
            p.sendMessage("§a§l      /mplaycoin set <time>  コインの排出間隔(秒)を設定           ");
            p.sendMessage("§a§l ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝");
            return true;
        }
        if (args[0].equalsIgnoreCase("register")) {
            if (args.length == 1){
                    getConfig().set("item",p.getInventory().getItemInMainHand());
                    saveConfig();
                    p.sendMessage("§2§l[Man10PlayCoin]§fアイテムの登録ができました");
                    reloadConfig();
                    return true;
            }
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 2){
                try{
                   time = Integer.parseInt(args[1]);
                }catch (NumberFormatException e){
                    p.sendMessage("§2§l[Man10PlayCoin]§c数字で入力してください。");
                    return true;
                }
                if (p.hasPermission("Man10PlayCoin.use")) {
                    getConfig().set("time",time);
                    saveConfig();
                    p.sendMessage("§2§l[Man10PlayCoin]§f時間の登録ができました");
                    reloadConfig();
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Long time = new Date().getTime();
        playerandtime.put(p,time);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player p = event.getPlayer();
        playerandtime.remove(p);
    }

    public void givecoin(){

    }
}
