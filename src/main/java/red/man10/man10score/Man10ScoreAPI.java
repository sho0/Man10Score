package red.man10.man10score;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import red.man10.Man10PlayerDataArchive.Man10PlayerData;
import red.man10.Man10PlayerDataArchive.Man10PlayerDataArchive;
import red.man10.Man10PlayerDataArchive.Man10PlayerDataArchiveAPI;
import red.man10.man10mysqlapi.MySQLAPI;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by sho on 2017/07/19.
 */
public class Man10ScoreAPI {
    MySQLAPI mysql = null;
    Man10PlayerDataArchiveAPI pda = null;

    public Man10ScoreAPI(){
        mysql = new MySQLAPI((JavaPlugin) Bukkit.getPluginManager().getPlugin("Man10Score"),"Man10Score");
        pda = new Man10PlayerDataArchiveAPI(mysql);
        loadSettingsToMemory();
    }

    static String prefix = "&b[&dMan10Score&b]&f".replaceAll("&","§");
    static int thank_point_amount = 1;
    static long thank_coolTime = 82800;
    static boolean thank_broadcast_enabled = true;
    static String thank_broadcast_message = "%PREFIX%%NAMEFROM%さんは、%NAMETO%に感謝しました。%NAMETO%は%POINTS%ポイントゲット (現在:%CURRENT%）".replaceAll("&","§");
    static boolean score_take_broadcast_enabled =true;
    static String score_take_message = "%PREFIX%&c%NAMETO%さんは、%NAMEFROM%さんから『%REASON%』の理由で%POINTS%ポイント減らされました";
    static boolean score_give_broadcast_enabled = true;
    static String score_give_message = "%PREFIX%&a%NAMETO%さんは、%NAMEFROM%さんから『%REASON%』の理由で%POINTS%ポイント表彰されました";

    File file = new File(Bukkit.getPluginManager().getPlugin("Man10Score").getDataFolder() + File.separator + "config.yml");
    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

    void loadSettingsToMemory(){
        prefix = config.getString("settings.prefix").replaceAll("&","§");
        thank_point_amount = config.getInt("settings.thank_point_amount");
        thank_coolTime = config.getLong("settings.thank_cooltime");
        thank_broadcast_enabled = config.getBoolean("settings.thank_broadcast_enabled");
        thank_broadcast_message = config.getString("settings.thank_broadcast_message").replaceAll("&","§");
        score_take_broadcast_enabled = config.getBoolean("settings.score_take_broadcast_enabled");
        score_take_message = config.getString("settings.score_take_message");
        score_give_broadcast_enabled = config.getBoolean("settings.score_give_broadcast_enabled");
        score_give_message = config.getString("settings.score_give_message");
    }

    public long getMan10Score(UUID uuid){
        return pda.getPlayerData(uuid).man10Score;
    }

    public int refreshMan10ScoreData(UUID uuid){
        boolean b = pda.setPlayerData(uuid,pda.getPlayerDataFromMysql(uuid));
        if(b){
            return 0;
        }
        return 1;
    }

    int giveMan10Score(String name,UUID uuid,String nameTo,UUID uuidTo,long value,String reason){
        if(value == 0){
            return 1;
        }
        if(value < 0){
            return 2;
        }
        createMan10Score(name,uuid,nameTo,uuidTo,value,reason,"Give");
        if(score_give_broadcast_enabled) {
            String message = score_give_message;
            message = message.replaceAll("%REASON%",reason);
            message = message.replaceAll("%POINTS%", String.valueOf(value));
            message = message.replaceAll("%NAMETO%",nameTo).replaceAll("%POINTS%", String.valueOf(value)).replaceAll("%REASON%",reason);
            message = message.replaceAll("%NAMEFROM%",name);
            message = message.replaceAll("%PREFIX%",prefix);
            message = message.replaceAll("&","§");
            message = message.replaceAll("%CURRENT%", String.valueOf(getMan10Score(uuidTo)));

            Bukkit.broadcastMessage(message);
        }
        return 0;
    }

    int takeMan10Score(String name,UUID uuid,String nameTo,UUID uuidTo,long value,String reason){
        if(value == 0){
            return 1;
        }
        if(value < 0){
            return 2;
        }
        createMan10Score(name,uuid,nameTo,uuidTo,-value,reason,"Take");
        if(score_take_broadcast_enabled){
            String message = score_take_message;
            if(score_take_message == null){
            }

            message = message.replaceAll("%REASON%",reason);

            message = message.replaceAll("%POINTS%", String.valueOf(value));

            message = message.replaceAll("%NAMETO%",nameTo).replaceAll("%POINTS%", String.valueOf(value)).replaceAll("%REASON%",reason);

            message = message.replaceAll("%NAMEFROM%",name);

            message = message.replaceAll("%PREFIX%",prefix);

            message = message.replaceAll("&","§");

            message = message.replaceAll("%CURRENT%", String.valueOf(getMan10Score(uuidTo)));
            Bukkit.broadcastMessage(message);
        }
        return 0;
    }

    void createMan10Score(String name,UUID uuid,String nameTo,UUID uuidTo,long value,String reason,String tag){
        ResultSet rs = mysql.query("SELECT man10_score FROM pda_player_data WHERE uuid ='" + uuidTo + "'");
        long v = 0;
        try {
            while (rs.next()){
                v = rs.getLong("man10_score");
            }
            rs.close();
            mysql.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        long res = v + value;
        mysql.execute("UPDATE pda_player_data SET man10_score ='" + res + "' WHERE uuid ='" + uuidTo + "'");
        mysql.execute("INSERT INTO man10_score_log VALUES('0','" + name + "','" + uuid + "','" + nameTo + "','" + uuidTo + "','" + value + "','" + reason + "','" + tag + "','" + pda.currentTimeNoBracket() + "','" + System.currentTimeMillis()/1000 + "');");
    }

    public long getLastThank(UUID uuid){
        ResultSet rs = mysql.query("SELECT time FROM man10_score_log WHERE uuid ='" + uuid + "' and tag = 'Thank' ORDER BY id DESC LIMIT 1;");
        try {
            while (rs.next()){
                return rs.getLong("time");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int thankPlayer(String name,UUID uuid,String nameTo,UUID uuidTo){
        if(getLastThank(uuid) != 0) {
            long val = System.currentTimeMillis() / 1000 - getLastThank(uuid);
            if (val < thank_coolTime) {
                return 2;
            }
        }
        Man10PlayerData pd = pda.getPlayerData(uuidTo);
        pd.man10Score = pd.man10Score + thank_point_amount;
        boolean b = mysql.execute("INSERT INTO man10_score_log VALUES('0','" + name + "','" + uuid + "','" + nameTo + "','" + uuidTo + "','" + thank_point_amount + "','" + "Thank" + "','" + "Thank" + "','" + pda.currentTimeNoBracket() + "','" + System.currentTimeMillis()/1000 + "');");
        pda.updatePlayerDataToMysql(uuidTo,pd);
        if(thank_broadcast_enabled){
            Bukkit.broadcastMessage(thank_broadcast_message.replaceAll("%PREFIX%",prefix).replaceAll("%NAMEFROM%",name).replaceAll("%NAMETO%",nameTo).replaceAll("%POINTS%", String.valueOf(thank_point_amount)).replaceAll("%CURRENT%", String.valueOf(getMan10Score(uuidTo))));
        }
        if(b){
            return 0;
        }
        return 1;
    }


    public int silentGiveMan10Score(String name, UUID uuid, String nameTo, UUID uuidTo, long value, String reason) {
        if (value == 0L) {
            return 1;
        }
        if (value < 0L) {
            return 2;
        }
        createMan10Score(name, uuid, nameTo, uuidTo, value, reason, "Give");
        return 0;
    }

    public int silentTakeMan10Score(String name, UUID uuid, String nameTo, UUID uuidTo, long value, String reason) {
        if (value == 0L) {
            return 1;
        }
        if (value < 0L) {
            return 2;
        }
         createMan10Score(name, uuid, nameTo, uuidTo, -value, reason, "Take");
        return 0;
    }

    public int fuckPlayer(String name, UUID uuid, String nameTo, UUID uuidTo,String reason) {
        createMan10Score(name, uuid, nameTo, uuidTo, 0, reason, "Fuck");
        return 0;
    }

}
