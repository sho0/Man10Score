package red.man10.man10score;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import red.man10.Man10PlayerDataArchive.Man10PlayerData;
import red.man10.Man10PlayerDataArchive.Man10PlayerDataArchiveAPI;
import red.man10.man10mysqlapi.MySQLAPI;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public final class Man10Score extends JavaPlugin {
    Man10ScoreAPI api = null;
    MySQLAPI mysql = null;
    Man10PlayerDataArchiveAPI pda = null;
    //settings
    String prefix = "&b[&dMan10Score&b]&f".replaceAll("&","§");

    long thank_coolTime = 82800;

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.saveDefaultConfig();
        mysql = new MySQLAPI(this,"Man10Score");
        mysql.execute(createScoreLog);
        api = new Man10ScoreAPI();
        loadSettingsToMemory();
        pda = new Man10PlayerDataArchiveAPI(mysql);
        thank_coolTime = getConfig().getLong("settings.thank_cooltime");
    }

    void loadSettingsToMemory(){
        prefix = getConfig().getString("settings.prefix").replaceAll("&","§");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("thank")){
            if (args.length != 1) {
                sender.sendMessage(prefix + "使い方が間違っています");
                return false;
            }
            Player send = (Player) sender;
            Player p = Bukkit.getPlayer(args[0]);
            int i = api.thankPlayer(send.getName(),send.getUniqueId(),p.getName(),p.getUniqueId());
            if(i == 2){
                long futureTimePoint = api.getLastThank(send.getUniqueId()) + thank_coolTime;
                long timeUnltillFutureTimePoint = futureTimePoint - System.currentTimeMillis()/1000;
                Date time = new Date();
                time.setTime(futureTimePoint*1000);
                send.sendMessage(prefix + "現在クールダウン中です、次のThankは" + time + "です");
                return false;
            }
        }
        if(command.getName().equalsIgnoreCase("mscore")){
            if(args.length == 0){
                help(sender);
                return true;
            }
            if(args.length == 1){
                if(args[0].equalsIgnoreCase("help")){
                    help(sender);
                    return true;
                }
                if(args[0].equalsIgnoreCase("reload")){
                    if(!sender.hasPermission("man10.score.reload")){
                        sender.sendMessage(prefix + "あなたには権限がありません");
                        return false;
                    }
                    api.loadSettingsToMemory();
                    loadSettingsToMemory();
                    sender.sendMessage(prefix + "コンフィグをリロードしました");
                    return true;
                }
            }
            if(args.length == 4){
                if(args[0].equalsIgnoreCase("give")){
                    if(!sender.hasPermission("man10.score.give")){
                        sender.sendMessage(prefix + "あなたには権限がありません");
                        return false;
                    }
                    if(pda.getUUIDFromNameMysql(args[1]) == null){
                        sender.sendMessage(prefix + "プレイヤーが存在しません");
                        return false;
                    }
                    Man10PlayerData pd = pda.getPlayerData(pda.getUUIDFromName(args[1]));
                    String name = "";
                    UUID uuid = null;
                    if(sender instanceof Player){
                        Player p = (Player) sender;
                        name = p.getName();
                        uuid = p.getUniqueId();
                    }else{
                        name = "Console";
                    }
                    try {
                        long value = Long.parseLong(args[2]);
                        int i = api.giveMan10Score(name, uuid, pd.name, pd.uuid, value,args[3]);
                        if(i == 1 || i == 2){
                            sender.sendMessage(prefix + "0以上の数字を入力してください");
                            return false;
                        }
                    }catch (NumberFormatException e){
                        sender.sendMessage(prefix + "Valueは数字を入力してください");
                        return false;
                    }
                }

                if(args[0].equalsIgnoreCase("take")){
                    if(!sender.hasPermission("man10.score.take")){
                        sender.sendMessage(prefix + "あなたには権限がありません");
                        return false;
                    }
                    if(pda.getUUIDFromNameMysql(args[1]) == null){
                        sender.sendMessage(prefix + "プレイヤーが存在しません");
                        return false;
                    }
                    Man10PlayerData pd = pda.getPlayerData(pda.getUUIDFromName(args[1]));
                    String name = "";
                    UUID uuid = null;
                    if(sender instanceof Player){
                        Player p = (Player) sender;
                        name = p.getName();
                        uuid = p.getUniqueId();
                    }else{
                        name = "Console";
                    }
                    try {
                        long value = Long.parseLong(args[2]);
                        int i = api.takeMan10Score(name, uuid, pd.name, pd.uuid, value,args[3]);
                        if(i == 1 || i == 2){
                            sender.sendMessage(prefix + "0以上の数字を入力してください");
                            return false;
                        }
                    }catch (NumberFormatException e){
                        sender.sendMessage(prefix + "Valueは数字を入力してください");
                        return false;
                    }
                }
            }
        }
        return false;
    }

    void help(CommandSender p){
        p.sendMessage("&e==========&e[&bMan10Score&e]&e==========".replaceAll("&","§"));
        p.sendMessage("§d§l/mscore help   ヘルプコマンド");
        p.sendMessage("§d§l/mscore reload コンフィグをリロードする");
        p.sendMessage("§d§l/mscore give <name> <amount> <reason> MScoreを付与する");
        p.sendMessage("§d§l/mscore take <name> <amount> <reason> MScoreをはく奪する");
    }

    String createScoreLog = "CREATE TABLE `man10_score_log` (\n" +
            "\t`id` INT NOT NULL AUTO_INCREMENT,\n" +
            "\t`name` VARCHAR(64) NOT NULL,\n" +
            "\t`uuid` VARCHAR(64) NOT NULL,\n" +
            "\t`to_name` VARCHAR(64) NOT NULL,\n" +
            "\t`to_uuid` VARCHAR(64) NOT NULL,\n" +
            "\t`value` BIGINT NOT NULL,\n" +
            "\t`reason` VARCHAR(4096) NOT NULL,\n" +
            "\t`tag` VARCHAR(64) NOT NULL,\n" +
            "\t`date_time` DATETIME NOT NULL,\n" +
            "\t`time` BIGINT NOT NULL,\n" +
            "\t PRIMARY KEY (`id`)\n" +
            ")\n" +
            "COLLATE='utf8_general_ci'\n" +
            "ENGINE=InnoDB\n" +
            ";\n";
}
