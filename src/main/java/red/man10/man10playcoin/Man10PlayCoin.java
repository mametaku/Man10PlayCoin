package red.man10.man10playcoin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

public final class Man10PlayCoin extends JavaPlugin implements Listener {

    // 設定パーミッション
    final String opPermission = "red.man10.playcoin.op";
    // アイテムをもらえるパーミッション
    final String getPermission = "red.man10.playcoin.get";

    // 設定
    ItemStack dropItem = null;
    boolean enableFlag;
    int itemDropIntervalTime;
    String giveCoinMessage;
    String fullInventoryMessage;
    List<String> disabledWorlds;

    //　保存データ
    HashMap<Player, Long> playerTimeMap = new HashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadSettings();

        getCommand("mplaycoin").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        // オンライン中のユーザーを登録
        playerTimeMap.clear();
        for (Player p:Bukkit.getOnlinePlayers()) {
            playerTimeMap.put(p, Instant.now().getEpochSecond());
        }

        // start timer
        getServer().getScheduler().scheduleSyncRepeatingTask((Plugin)this, new Runnable() {
            public void run() {
                giveCoinTask();
            }
        },  0L, 20L * 10);
    }

    void loadSettings(){
        FileConfiguration config = getConfig();
        enableFlag = config.getBoolean("enableFlag");
        itemDropIntervalTime = config.getInt("itemDropIntervalTime");
        giveCoinMessage = config.getString("giveCoinMessage");
        fullInventoryMessage = config.getString("fullInventoryMessage");
        dropItem = config.getItemStack("dropItem");
        disabledWorlds = config.getStringList("disabledWorlds");
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

        Player p = (Player) sender;
        if(!p.hasPermission(this.opPermission)){
            p.sendMessage("権限がありません");
            return false;
        }

        // アイテム登録

        if (args[0].equalsIgnoreCase("register")) {
            if (args.length == 1){
                    dropItem = p.getInventory().getItemInMainHand();
                    getConfig().set("dropItem",dropItem);
                    saveConfig();
                    p.sendMessage("§2§l[Man10PlayCoin]§fアイテムの登録ができました");
                    return true;
            }
        }

        //  時間設定
        if (args[0].equalsIgnoreCase("time")) {
            if(args.length != 2){
                p.sendMessage("[usage]mplaycoin time [秒数]");
                return false;
            }
            try{
                itemDropIntervalTime = Integer.parseInt(args[1]);
            }catch (NumberFormatException e){
                p.sendMessage("§2§l[Man10PlayCoin]§c数字で入力してください。");
                return true;
            }
            getConfig().set("itemDropIntervalTime",itemDropIntervalTime);
            saveConfig();
            p.sendMessage("§2§l[Man10PlayCoin]§f時間の登録ができました");
        }

        if (args[0].equalsIgnoreCase("on")) {
            enableFlag = true;
            getConfig().set("enableFlag",true);
            p.sendMessage("§2§l再開しました");
            return false;
        }
        if (args[0].equalsIgnoreCase("off")) {
            enableFlag = false;
            getConfig().set("enableFlag",false);
            p.sendMessage("§2§l停止しました");
            return false;
        }

        //
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

        p.sendMessage("§a§l ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝");
        p.sendMessage("§a§l                   [Man10PlayCoin]                   ");
        p.sendMessage("§a§l /mplaycoin register   手持ちのアイテムを設定    ");
        p.sendMessage("§a§l /mplaycoin time <time>  コインの排出間隔(秒)を設定           ");
        p.sendMessage("§a§l /mplaycoin dropMessage <Message>  メッセージを設定           ");
        p.sendMessage("§a§l /mplaycoin fullInvMessage <Message>  上に同じ           ");
        p.sendMessage("§a§l /mplaycoin off 停止          ");
        p.sendMessage("§a§l /mplaycoin on 開始          ");
        p.sendMessage("§a§l ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝");


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

        // 無効中を処理しない
        if(enableFlag == false)
            return;

        if(dropItem == null)
            return;

        // 現在の時刻を取得
        Long now = Instant.now().getEpochSecond();

        // 登録されているプレーヤを処理する
        for (Player p : playerTimeMap.keySet()) {
            if(!p.isOnline())
                continue;

            if(!p.hasPermission(getPermission))
                continue;

            // 対象プレーヤの経過時間
            Long lap = now - playerTimeMap.get(p);

            // 設定時間をこえたらアイテムを排出する
            if(lap >= itemDropIntervalTime){

                // 無効ワールドならドロップしない
                if(disabledWorlds.contains(p.getWorld().getName()))
                    continue;

                // 音を出す
                Location loc = p.getLocation();
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL,1f,1f);

                // インベントリがフルならメッセージをだす
                if (isInventoryFull(p)){
                    p.sendMessage(fullInventoryMessage.replace("%player%",p.getName()));
                    playerTimeMap.put(p,now);
                    continue;
                }

                // アイテムをあげてメッセージを表示
                p.getInventory().addItem(dropItem);
                p.sendMessage(giveCoinMessage.replace("%player%",p.getName()));
                playerTimeMap.put(p,now);
            }
        }

    }
    public boolean isInventoryFull(Player p)
    {
        return p.getInventory().firstEmpty() == -1;
    }
}
