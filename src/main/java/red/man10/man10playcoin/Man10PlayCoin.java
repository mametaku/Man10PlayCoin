package red.man10.man10playcoin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import sun.security.krb5.Config;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

public final class Man10PlayCoin extends JavaPlugin implements Listener {

    boolean enableFlag;
    int itemDropIntervalTime;
    String giveCoinMessage;
    String fullInventoryMessage;
    ItemStack item;
    HashMap<Player, Long> playerTimeMap = new HashMap<>();
    List<String> disabledWorlds;
    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);
        // config.ymlが存在しない場合はファイルに出力します。
        saveDefaultConfig();
        // config.ymlを読み込みます。
        FileConfiguration config = getConfig();
        reloadConfig();


        enableFlag = true;
        getCommand("mplaycoin").setExecutor(this);
        if (!config.getBoolean("enableFlag")) {
            getLogger().info("Man10PlayCoin is disabled.");
            enableFlag = false;
            return;
        }

        itemDropIntervalTime = getConfig().getInt("itemDropIntervalTime");
        giveCoinMessage = getConfig().getString("giveCoinMessage");
        fullInventoryMessage = getConfig().getString("fullInventoryMessage");
        item = getConfig().getItemStack("item");
        for (int i=0; i<getConfig().getString("worlds").length(); ++i) {
            disabledWorlds.add(getConfig().getString("worlds"));
        }
        // initialize hashmap when reload
        playerTimeMap.clear();
        for (Player p:Bukkit.getOnlinePlayers()) {
            playerTimeMap.put(p, Instant.now().getEpochSecond());
        }


        getServer().getScheduler().scheduleSyncRepeatingTask((Plugin)this, new Runnable() {
            public void run() {
                giveCoinTask();
            }
        },  0L, 20L);
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

        if (!enableFlag) return true;

        Player p = (Player) sender;
        if (!p.hasPermission("mplaycoin.use")) {
            p.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§a§l ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝");
            p.sendMessage("§a§l                   [Man10PlayCoin]                   ");
            p.sendMessage("§a§l /mplaycoin register   手持ちのアイテムを設定    ");
            p.sendMessage("§a§l /mplaycoin time <time>  コインの排出間隔(秒)を設定           ");
            p.sendMessage("§a§l /mplaycoin givecoinmessage <Message>  メッセージを設定           ");
            p.sendMessage("§a§l /mplaycoin fullinventorymessage <Message>  上に同じ           ");
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
        if (args[0].equalsIgnoreCase("time")) {
            if (args.length == 2){
                try{
                    itemDropIntervalTime = Integer.parseInt(args[1]);
                }catch (NumberFormatException e){
                    p.sendMessage("§2§l[Man10PlayCoin]§c数字で入力してください。");
                    return true;
                }
                if (p.hasPermission("Man10PlayCoin.use")) {
                    getConfig().set("time",itemDropIntervalTime);
                    saveConfig();
                    p.sendMessage("§2§l[Man10PlayCoin]§f時間の登録ができました");
                    reloadConfig();
                    return true;
                }
            }
        }
        if (args[0].equalsIgnoreCase("givecoinmessage")) {
            if (args.length == 2){
                giveCoinMessage = (args[1]);
                getConfig().set("giveCoinMessage",args[1]);
                saveConfig();
                p.sendMessage("§2§l[Man10PlayCoin]§fメッセージの登録ができました");
                reloadConfig();
                return true;
            }
        }
        if (args[0].equalsIgnoreCase("fullinventorymessage")) {
            if (args.length == 2){
                fullInventoryMessage = (args[1]);
                getConfig().set("fullInventoryMessage",args[1]);
                saveConfig();
                p.sendMessage("§2§l[Man10PlayCoin]§fメッセージの登録ができました");
                reloadConfig();
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerTimeMap.put(event.getPlayer(),Instant.now().getEpochSecond());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        playerTimeMap.remove(event.getPlayer());
    }

    //
    public void giveCoinTask(){
        if(enableFlag == false)
            return;

        // get unix time
        Long now = Instant.now().getEpochSecond();

        for (Player p : playerTimeMap.keySet()) {
            if(!p.isOnline())
                continue;
            Long last = playerTimeMap.get(p);
            Long lap = now - last;

            p.sendMessage("time:"+lap);
            if(lap >= itemDropIntervalTime){
                p.sendMessage("give item to player");
                if(p.getWorld().getName().equals(disabledWorlds)) {
                    return;
                }
                if (isInventoryFull(p)){
                    p.sendMessage(fullInventoryMessage);
                    Location loc = p.getLocation();
                    loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL,1f,1f);
                    playerTimeMap.put(p,now);
                    return;
                }
                p.getInventory().addItem(item);
                p.sendMessage(giveCoinMessage);
                // set time
                playerTimeMap.put(p,now);
            }
        }

    }
    public boolean isInventoryFull(Player p)
    {
        return p.getInventory().firstEmpty() == -1;
    }
}
