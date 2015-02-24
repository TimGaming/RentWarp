package me.falhur.rentwarp.RentWarpPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import me.falhur.rentwarp.Listeners.WarpListener;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


public class RentWarpPlugin extends JavaPlugin {
    private static Plugin plugin;

    public static Economy economy = null;
    public static Permission permission = null;

    public void onDisable() {
        Bukkit.getLogger().info("[RentWarp] Has Been Disabled!");
        reloadConfig();
        saveConfig();
    }

    public static String colorize(String msg)
    {
        String coloredMsg = "";
        for(int i = 0; i < msg.length(); i++)
        {
            if(msg.charAt(i) == '&')
                coloredMsg += '§';
            else
                coloredMsg += msg.charAt(i);
        }
        return coloredMsg;
    }
    public void onEnable() {
        
        loadConfiguration();
        
        setupEconomy();
        setupPermissions();
        plugin = this;
        
        new WarpListener(this);
        
        
        File WarpFolder = new File(getDataFolder(), "");
        WarpFolder.mkdir();
        File Warp = new File(WarpFolder, "warps.yml");
        File warplist = new File(WarpFolder, "warplist.yml");

        if (!Warp.exists()) {
            FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
            warpConf.createSection("Warps");
            try {
                warpConf.save(getDataFolder() + File.separator + "warps.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!warplist.exists()) {
            FileConfiguration warpConf2 = YamlConfiguration.loadConfiguration(warplist);
            warpConf2.createSection("WarpList");
            try {
                warpConf2.save(getDataFolder() + File.separator + "warplist.yml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        
    
        
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {

                File WarpFolder = new File(getDataFolder(), "");
                WarpFolder.mkdir();

                File Warp = new File(WarpFolder, "warplist.yml");
                FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);


                List < String > WarpList = warpConf.getStringList("WarpList");

                for (String temp: WarpList) {
                    File Warps = new File(WarpFolder, "warps.yml");
                    FileConfiguration warpConf2 = YamlConfiguration.loadConfiguration(Warps);

                    File Warps2 = new File(WarpFolder, "warplist.yml");
                    FileConfiguration warpConf1 = YamlConfiguration.loadConfiguration(Warps2);

                    // check if essentials is enabled in config
                    if (getConfig().getBoolean("RentWarp.Settings.isEssentials")){
                        /* Check if warp has 7 days remaining or less and send mail! */
                        long advancedTime = (long) warpConf2.get("Warps." + temp + ".Time") - 604800000L;
                        String creator = (String) warpConf2.get("Warps." + temp + ".Creator");
                        String theCreator = Bukkit.getServer().getOfflinePlayer(UUID.fromString(creator)).getName();
                        long yourmilliseconds = (long) warpConf2.get("Warps." + temp + ".Time");
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                        Date resultdate = new Date(yourmilliseconds);
                        int RunLow = (int) warpConf2.get("Warps." + temp + ".RunLow");
                        if (System.currentTimeMillis() > advancedTime && advancedTime > 0 && RunLow != 1) {
                            
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mail send " + theCreator + " " + ChatColor.RED + "NOTICE! " + ChatColor.WHITE +"Your warp " + ChatColor.AQUA + temp + ChatColor.WHITE + " expires on " + ChatColor.AQUA + sdf.format(resultdate) + ChatColor.WHITE + ", use " + ChatColor.LIGHT_PURPLE + "/warp addtime " + ChatColor.WHITE+ "to add more time!");
                            warpConf2.set("Warps." + temp + ".RunLow", 1);
                            try {
                                warpConf2.save(getDataFolder() + File.separator + "warps.yml");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                
                    /* Check if warp expired, if so then delete */
                    long checkTime = (long) warpConf2.get("Warps." + temp + ".Time");
                    
                    if (System.currentTimeMillis() > checkTime) {
                        List < String > warplist2 = warpConf1.getStringList("WarpList");
                        warplist2.remove(temp);
                        warpConf2.set("Warps." + temp, null);
                        warpConf1.set("WarpList", warplist2);
                        try {
                            warpConf1.save(getDataFolder() + File.separator + "warplist.yml");
                            warpConf2.save(getDataFolder() + File.separator + "warps.yml");
                            Bukkit.getLogger().info("[RentWarp]: Expired warp found and deleted: [" + temp + "]");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }

                }




            }
        }, 80);



    }



    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    
    
    
    
    //-------------START TP DELAY ---------------
    
    
    private static HashMap<String, BukkitTask>warpers = new HashMap<String, BukkitTask>();//Player Name -> Warp Task
    /**
     * Schedules a Player to be teleported after the delay time
     *
     * @param player The Player being teleported
     * @param loc The location of the destination
     * @param pitch 
     * @param yaw 
     * @param warpName 
     * @param warpGreeting 
     * @return 
     */
    public static void scheduleWarp(final Player player, final Location loc, final Object yaw, final Object pitch, final String warpName, final String warpGreeting, final int warpDelay) {
      //Inform the player that they will be teleported

      if (!permission.playerHas(player, "RentWarp.Admin.CoolDownExempt")){
         player.sendMessage(ChatColor.GOLD + "You will be teleported in "+ ChatColor.RED + warpDelay + " seconds" + ChatColor.GOLD + ". Don't move.");
      }
      
      
      //Create a task to teleport the player
      BukkitRunnable runnable = new BukkitRunnable() {
          
        @Override
        public void run() {
          player.sendMessage(ChatColor.GOLD + "Teleportation commencing...");
          player.teleport(loc); 
          loc.setPitch((float) pitch);
          loc.setYaw((float) yaw);
          player.sendMessage("- " + colorize(warpGreeting));
          
          //Remove the player as a warper because they have already been teleported
          warpers.remove(player.getName());
        }
      };
      
      //Schedule the task to run later
      if (permission.playerHas(player, "RentWarp.Admin.CoolDownExempt")){
          BukkitTask task = runnable.runTaskLater(plugin, 0);
        //Keep track of the player and their warp task
          warpers.put(player.getName().toString(), task);
      }else{
          BukkitTask task = runnable.runTaskLater(plugin, 20L * warpDelay);
        //Keep track of the player and their warp task
          warpers.put(player.getName().toString(), task);
      }
      
      
      
    
    }

    /**
     * Returns true if the player is waiting to be teleported
     *
     * @param player The Player in question
     * @return true if the player is waiting to be warped
     */
    public static boolean isWarping(String player) {
      return warpers.containsKey(player);
    }

    /**
     * Cancels the warp task for the given player
     *
     * @param player The Player whose warp task will be canceled
     */
    public static void cancelWarp(String player) {
      //Check if the player is warping
      if (isWarping(player)) {
        //Remove the player as a warper
        //Cancel the task so that the player is not teleported
        warpers.remove(player).cancel();
      }
    }

    
    //-------------END TP DELAY ---------------
    
    

    
    
    
    
    
    @SuppressWarnings("deprecation")
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if ((sender instanceof Player)) {


            
            Player p = (Player) sender;
            String playerName = p.getName();

            Player target = Bukkit.getServer().getPlayer(playerName);
            String PlayerUUID = target.getUniqueId().toString();



            String noPerm = ChatColor.RED + "You do not have permission to do that!";
            if ((cmd.getName().equalsIgnoreCase("listcolors")) && args.length == 0) {
                p.sendMessage("Complete Color List: ");
                p.sendMessage(ChatColor.DARK_BLUE + " &1" + ChatColor.DARK_GREEN + " &2" + ChatColor.DARK_AQUA + " &3" +  ChatColor.DARK_RED + " &4" +  ChatColor.DARK_PURPLE + " &5" +  ChatColor.GOLD + " &6" +  ChatColor.GRAY + " &7" +  ChatColor.DARK_GRAY + " &8" +  ChatColor.BLUE + " &9" +  ChatColor.BLACK + " &0" +  ChatColor.GREEN + " &a" +  ChatColor.AQUA + " &b" +  ChatColor.RED + " &c" +  ChatColor.LIGHT_PURPLE + " &d" +  ChatColor.YELLOW + " &e" +  ChatColor.WHITE + " &f");
            }
            if ((cmd.getName().equalsIgnoreCase("warp")) && (args.length >= 1)) {



                if (args[0].equalsIgnoreCase("help")) {
                    if (permission.playerHas(p, "RentWarp.Player.Help")) {
                        if (args.length >= 0) {
                            p.sendMessage("==========" + ChatColor.GREEN + "RentWarp Help" + ChatColor.WHITE + "==========");
                            p.sendMessage(ChatColor.WHITE + "/warp help <page>" + ChatColor.YELLOW + " - View the help page");
                            p.sendMessage(ChatColor.WHITE + "/warp info <warpName>" + ChatColor.YELLOW + " - View info on a warp");
                            p.sendMessage(ChatColor.WHITE + "/warp cost" + ChatColor.YELLOW + " - Tellls you price to create warp and to add time");
                            p.sendMessage(ChatColor.WHITE + "/warp <warpName>" + ChatColor.YELLOW + " - Teleport To a warp");
                            p.sendMessage(ChatColor.WHITE + "/warp create <warpName>" + ChatColor.YELLOW + " - Create a warp");
                            p.sendMessage(ChatColor.WHITE + "/warp delete <warpName>" + ChatColor.YELLOW + " - Deletes a warp");
                            p.sendMessage(ChatColor.WHITE + "/warp move <warpName>" + ChatColor.YELLOW + " - Moves a warp to your location");
                            p.sendMessage(ChatColor.WHITE + "/warp addtime <warpName> <multiplier>" + ChatColor.YELLOW + " - Add time to your warp in 30 day increments (1 by default)");
                            p.sendMessage(ChatColor.WHITE + "/warp greeting <warpName> <msg>" + ChatColor.YELLOW + " - Set a greeting for a warp");
                            p.sendMessage(ChatColor.WHITE + "/warp List" + ChatColor.YELLOW + " - View All Warps");
                            p.sendMessage(ChatColor.WHITE + "/listcolors" + ChatColor.YELLOW + " - List available colors for warp greetings");
                            if (permission.playerHas(p, "RentWarp.Admin.RentExempt")) {
                                p.sendMessage(ChatColor.WHITE + "/warp exempt <warpName>" + ChatColor.YELLOW + " - Exempts a warp from rent");
                            }
                            if (permission.playerHas(p, "RentWarp.Admin.Reload")) {
                                p.sendMessage(ChatColor.WHITE + "/warp reload" + ChatColor.YELLOW + " - Reloads all plugin configs");
                            }
                        }
                    } else {
                        p.sendMessage(noPerm);
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (permission.playerHas(p, "RentWarp.Admin.Reload")) {
                        reloadConfig();
                        p.sendMessage(ChatColor.GREEN + "Config Reloaded!");
                    } else {
                        p.sendMessage(noPerm);
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    if (permission.playerHas(p, "RentWarp.Player.List")) {

                        File WarpFolder = new File(getDataFolder(), "");
                        WarpFolder.mkdir();
                        File Warp = new File(WarpFolder, "warplist.yml");
                        FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                        p.sendMessage(ChatColor.GOLD + " Warps: " + ChatColor.GRAY + warpConf.getStringList("WarpList"));

                    } else {
                        p.sendMessage(noPerm);
                    }
                } else if (args[0].equalsIgnoreCase("delete")) {
                    if (args.length == 2) {
                        if (permission.playerHas(p, "RentWarp.Admin.Delete")) {
                            String s = args[1];
                            args[1] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            File WarpFolder = new File(getDataFolder(), "");
                            WarpFolder.mkdir();
                            File Warp = new File(WarpFolder, "warps.yml");
                            FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                            File WarpList = new File(WarpFolder, "warplist.yml");
                            FileConfiguration warpConf2 = YamlConfiguration.loadConfiguration(WarpList);

                            if (warpConf.getString("Warps." + args[1]) != null) {

                                List < String > warplist = warpConf2.getStringList("WarpList");
                                warplist.remove(args[1]);
                                warpConf2.set("WarpList", warplist);

                                warpConf.set("Warps." + args[1], null);
                                try {
                                    warpConf2.save(getDataFolder() + File.separator + "warplist.yml");
                                    warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                    p.sendMessage("Warp " + ChatColor.AQUA + args[1] + ChatColor.WHITE + " Has Been Deleted!");
                                    Bukkit.getLogger().info("[RentWarp]: " + playerName + " deleted the warp [" + args[1] + "]");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "That warp doesnt exist!");
                            }
                        } else if (permission.playerHas(p, "RentWarp.Player.Delete")) {
                            String s = args[1];
                            args[1] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            File WarpFolder = new File(getDataFolder(), "");
                            WarpFolder.mkdir();
                            File Warp = new File(WarpFolder, "warps.yml");
                            FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                            File WarpList = new File(WarpFolder, "warplist.yml");
                            FileConfiguration warpConf2 = YamlConfiguration.loadConfiguration(WarpList);
                            if (warpConf.getString("Warps." + args[1]) != null) {
                                if (warpConf.getString("Warps." + args[1] + ".Creator").equals(PlayerUUID)) {
                                    List < String > warplist = warpConf2.getStringList("WarpList");
                                    warplist.remove(args[1]);
                                    warpConf2.set("WarpList", warplist);

                                    warpConf.set("Warps." + args[1], null);
                                    try {
                                        warpConf2.save(getDataFolder() + File.separator + "warplist.yml");
                                        warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                        p.sendMessage("Warp " + ChatColor.AQUA + args[1] + ChatColor.WHITE + " Has Been Deleted!");
                                        Bukkit.getLogger().info("[RentWarp]: " + playerName + " deleted the warp [" + args[1] + "]");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                } else {
                                    p.sendMessage(ChatColor.RED + "You do not own this warp!");
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "That warp doesn't exist!");
                            }
                        } else {
                            p.sendMessage(noPerm);
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "Usage: /warp delete <warp name>");
                    }
                } else if (args[0].equalsIgnoreCase("exempt")) {
                    if (args.length == 2) {
                        if (permission.playerHas(p, "RentWarp.Admin.RentExempt")) {
                            File WarpFolder = new File(getDataFolder(), "");
                            WarpFolder.mkdir();
                            File Warp = new File(WarpFolder, "warps.yml");
                            FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                            if (warpConf.getString("Warps." + args[1]) != null) {
                                String exempt = "Un-Exempted";
                                if ((int) warpConf.get("Warps." + args[1] + ".Exempt") == (int) 1) {
                                    long oldTime = (long) warpConf.get("Warps." + args[1] + ".Time2");
                                    warpConf.set("Warps." + args[1] + ".Time", oldTime);
                                    warpConf.set("Warps." + args[1] + ".Time2", (int) 0);
                                    warpConf.set("Warps." + args[1] + ".Exempt", (int) 0);
                                    exempt = "Un-Exempted";
                                } else {
                                    long oldTime = (long) warpConf.get("Warps." + args[1] + ".Time");
                                    long longTime = (long) 253370786400000L;
                                    warpConf.set("Warps." + args[1] + ".Time2", oldTime);
                                    warpConf.set("Warps." + args[1] + ".Time", longTime);
                                    warpConf.set("Warps." + args[1] + ".Exempt", (int) 1);
                                    exempt = "Exempted";
                                }
                                try {
                                    warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                    p.sendMessage(ChatColor.WHITE + "Warp successfully " + ChatColor.RED + exempt + ChatColor.WHITE + " from rent!");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        } else {
                            p.sendMessage(noPerm);
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "Usage: /warp exempt <warp name>");
                    }
                } else if (args[0].equalsIgnoreCase("move")) {
                    String s = args[1];
                    args[1] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                    File WarpFolder = new File(getDataFolder(), "");
                    WarpFolder.mkdir();
                    File Warp = new File(WarpFolder, "warps.yml");
                    FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                    if (warpConf.getString("Warps." + args[1]) != null) {
                        if (warpConf.getString("Warps." + args[1] + ".Creator").equals(PlayerUUID) || permission.playerHas(p, "RentWarp.Admin.Move")) {

                            warpConf.set("Warps." + args[1] + ".X", Integer.valueOf(p.getLocation().getBlockX()));
                            warpConf.set("Warps." + args[1] + ".Y", Integer.valueOf(p.getLocation().getBlockY()));
                            warpConf.set("Warps." + args[1] + ".Z", Integer.valueOf(p.getLocation().getBlockZ()));
                            warpConf.set("Warps." + args[1] + ".Yaw", Float.valueOf(p.getLocation().getYaw()));
                            warpConf.set("Warps." + args[1] + ".Pitch", Float.valueOf(p.getLocation().getPitch()));
                            try {
                                warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                p.sendMessage(ChatColor.GREEN + "Warp successfully moved!");
                                Bukkit.getLogger().info("[RentWarp]: " + playerName + " moved a new warp [" + args[1] + "] at X(" + Integer.valueOf(p.getLocation().getBlockX()) + ") Y(" + Integer.valueOf(p.getLocation().getBlockY()) + ") Z(" + Integer.valueOf(p.getLocation().getBlockZ()) + ")");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {
                            p.sendMessage(ChatColor.RED + "You do not own this warp!");
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "That warp doesn't exist!");
                    }

                } else if (args[0].equalsIgnoreCase("cost")) {
                    
                            
                    p.sendMessage(ChatColor.WHITE + "Initial cost: " + ChatColor.GREEN + getConfig().getInt("RentWarp.Economy.Cost2Create") + ChatColor.WHITE + " " + economy.currencyNamePlural());
                    p.sendMessage(ChatColor.WHITE + "Add 30 days: " + ChatColor.GREEN + getConfig().getInt("RentWarp.Economy.Cost2Renew") + ChatColor.WHITE + " " + economy.currencyNamePlural());
                        

                    

                } else if (args[0].equalsIgnoreCase("addtime")) {
                    if (args.length >= 2 && args.length < 4) {
                        if (permission.playerHas(p, "RentWarp.Player.Addtime")) {
                            String s = args[1];
                            args[1] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            int multiplier = 1;
                            if (args.length == 3 && isInt(args[2])) {
                                multiplier = Integer.parseInt(args[2]);
                            }
                            if (economy.has(playerName, getConfig().getDouble("RentWarp.Economy.Cost2Renew") * multiplier)) {
                                File WarpFolder = new File(getDataFolder(), "");
                                WarpFolder.mkdir();
                                File Warp = new File(WarpFolder, "warps.yml");
                                FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                                if (warpConf.getString("Warps." + args[1]) != null) {


                                    long newtime = (long) warpConf.get("Warps." + args[1] + ".Time") + (2592000000L * multiplier);
                                    warpConf.set("Warps." + args[1] + ".Time", newtime);
                                    try {
                                        warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                        p.sendMessage(ChatColor.WHITE + "Successfully added " + ChatColor.GREEN + multiplier + " Month(s)" + ChatColor.WHITE + " to your remaining time!");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    p.sendMessage(ChatColor.RED + "Warp does not exist!");
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "You don't have enough money to renew this warp! " + getConfig().getDouble("RentWarp.Economy.Cost2Renew") * multiplier);
                            }
                        } else {
                            p.sendMessage(noPerm);
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "Usage: /warp addtime <warp name>");
                    }
                } else if (args[0].equalsIgnoreCase("loc")) {
                    p.sendMessage(ChatColor.RED + "Current Location: " + Integer.valueOf(p.getLocation().getBlockX()) + " | " + Integer.valueOf(p.getLocation().getBlockY()) + " | " + Integer.valueOf(p.getLocation().getBlockZ()));
                } else if (args[0].equalsIgnoreCase("create")) {
                    if (args.length >= 2) {
                        if (permission.playerHas(p, "RentWarp.Player.Create")) {
                            String s = args[1];
                            args[1] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            Pattern pat = Pattern.compile("[^a-zA-Z0-9]");
                            boolean hasSpecialChar = pat.matcher(s).find();
                            if (!hasSpecialChar) {
                                if (economy.has(playerName, getConfig().getDouble("RentWarp.Economy.Cost2Create"))) {
                                    File WarpFolder = new File(getDataFolder(), "");
                                    WarpFolder.mkdir();
                                    File Warp = new File(WarpFolder, "warps.yml");
                                    FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                                    File WarpList = new File(WarpFolder, "warplist.yml");
                                    FileConfiguration warpConf2 = YamlConfiguration.loadConfiguration(WarpList);
                                    if (warpConf.getString("Warps." + args[1]) != null) {
                                        p.sendMessage(ChatColor.RED + "That warp already exists!");
                                    } else {
                                        try {
                                            Warp.createNewFile();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        String greeting = (String) "Welcome to &3" + args[1];
                                        warpConf.createSection("Warps." + args[1]);
                                        warpConf.set("Warps." + args[1] + ".World", p.getWorld().getName());
                                        warpConf.set("Warps." + args[1] + ".X", Integer.valueOf(p.getLocation().getBlockX()));
                                        warpConf.set("Warps." + args[1] + ".Y", Integer.valueOf(p.getLocation().getBlockY()));
                                        warpConf.set("Warps." + args[1] + ".Z", Integer.valueOf(p.getLocation().getBlockZ()));
                                        warpConf.set("Warps." + args[1] + ".Yaw", Float.valueOf(p.getLocation().getYaw()));
                                        warpConf.set("Warps." + args[1] + ".Pitch", Float.valueOf(p.getLocation().getPitch()));
                                        warpConf.set("Warps." + args[1] + ".Creator", PlayerUUID.toString());
                                        warpConf.set("Warps." + args[1] + ".Greeting", greeting);
                                        warpConf.set("Warps." + args[1] + ".Time", System.currentTimeMillis() + 2592000000L);
                                        warpConf.set("Warps." + args[1] + ".Time2", (int) 0);
                                        warpConf.set("Warps." + args[1] + ".Exempt", (int) 0);
                                        warpConf.set("Warps." + args[1] + ".RunLow", (int) 0);


                                        List < String > warplist = warpConf2.getStringList("WarpList");
                                        warplist.add(args[1]);
                                        warpConf2.set("WarpList", warplist);
                                        try {
                                            warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                            warpConf2.save(getDataFolder() + File.separator + "warplist.yml");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }



                                        economy.withdrawPlayer(playerName, getConfig().getDouble("RentWarp.Economy.Cost2Create"));
                                        p.sendMessage("You Paid " + ChatColor.GREEN + "$" + getConfig().getString("RentWarp.Economy.Cost2Create") + ChatColor.WHITE + " To Create a Warp named " + ChatColor.AQUA + args[1] + ChatColor.WHITE + "!");
                                        Bukkit.getLogger().info("[RentWarp]: " + playerName + " created a new warp [" + args[1] + "] at X(" + Integer.valueOf(p.getLocation().getBlockX()) + ") Y(" + Integer.valueOf(p.getLocation().getBlockY()) + ") Z(" + Integer.valueOf(p.getLocation().getBlockZ()) + ")");

                                    }

                                } else {
                                    p.sendMessage(ChatColor.RED + "You don't have enough money to buy a warp!");
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "Warp names must not have special characters!");
                            }
                        } else {
                            p.sendMessage(noPerm);

                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "Usage: /warp create <warp name>");
                    }
                } else if (args[0].equalsIgnoreCase("greeting")) {
                    if (permission.playerHas(p, "RentWarp.Player.Create")){
                        if (args.length > 2){
                            String myString = ""; //we're going to store the arguments here    

                            for(int i = 0; i < args.length; i++){ //loop threw all the arguments
                                if (i != 0 && i != 1){
                                    String arg = args[i] + " "; //get the argument, and add a space so that the words get spaced out
                                    myString = myString + arg; //add the argument to myString   
                                }
                                
                            }
                            
                            Pattern pat = Pattern.compile("[^a-zA-Z0-9 _!\\&-]");
                            boolean hasSpecialChar = pat.matcher(myString).find();
                            if (!hasSpecialChar){
                                String s = args[1];
                                args[1] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                                File WarpFolder = new File(getDataFolder(), "");
                                WarpFolder.mkdir();
                                File Warp = new File(WarpFolder, "warps.yml");
                                FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                                if (warpConf.getString("Warps." + args[1]) == null) {
                                    p.sendMessage(ChatColor.RED + "That warp doesn't exist!");
                                } else {
                                    if (warpConf.getString("Warps." + args[1] + ".Creator").equals(PlayerUUID) || permission.playerHas(p, "RentWarp.Admin.Move")){
                                        
                                            
                                            
                                            warpConf.set("Warps." + args[1] + ".Greeting", myString);
    
                                            try {
                                                warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                                p.sendMessage(ChatColor.GREEN + "Greeting Updated: " + ChatColor.WHITE + colorize(myString));
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            Bukkit.getLogger().info("[RentWarp]: " + playerName + " updated the following warps greeting [" + args[1] + "] to " + myString);
    
                                        
                                    }else{
                                        p.sendMessage(ChatColor.RED + "You do not own this warp!");
                                    }
                                }
                                
                                
                            }else{
                                p.sendMessage(ChatColor.RED + "Warp greetings must not have special characters!");
                            }
                        }else{
                            p.sendMessage(ChatColor.RED + "Usage: /warp setgreeting <warp name> <greeting message>");
                        }
                    } else {
                        p.sendMessage(noPerm);
                    }
                } else if (args[0].equalsIgnoreCase("info")) {
                    if (permission.playerHas(p, "RentWarp.Player.Info")) {
                        if (args.length == 2) {
                            String s = args[1];
                            args[1] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            File WarpFolder = new File(getDataFolder(), "");
                            WarpFolder.mkdir();
                            File Warp = new File(WarpFolder, "warps.yml");
                            FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                            if (warpConf.getString("Warps." + args[1]) != null) {
                                long yourmilliseconds = (long) warpConf.get("Warps." + args[1] + ".Time");
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                                Date resultdate = new Date(yourmilliseconds);
                                

                                p.sendMessage(ChatColor.GRAY + "--------------------------------------------------");
                                p.sendMessage(" Warp: " + ChatColor.LIGHT_PURPLE + args[1] + ChatColor.WHITE + " Information");

                                p.sendMessage(ChatColor.GRAY + "--------------------------------------------------");
                                

                                String warpCreator = warpConf.getString("Warps." + args[1] + ".Creator");
                                String greeting = warpConf.getString("Warps." + args[1] + ".Greeting");
                                String getPlayer = Bukkit.getServer().getOfflinePlayer(UUID.fromString(warpCreator)).getName();
                                p.sendMessage(ChatColor.RED + " Greeting: " + ChatColor.WHITE + colorize(greeting));
                                p.sendMessage(ChatColor.RED + " Owner: " + ChatColor.GRAY + getPlayer);


                                p.sendMessage(ChatColor.RED + " Location: " + ChatColor.GRAY + warpConf.getString("Warps." + args[1] + ".X") + ", " + warpConf.getString("Warps." + args[1] + ".Y") + ", " + warpConf.getString("Warps." + args[1] + ".Z"));

                                if (warpConf.getString("Warps." + args[1] + ".Creator").equals(PlayerUUID)) {
                                    if ((int) warpConf.get("Warps." + args[1] + ".Exempt") == (int) 1) {
                                        p.sendMessage(ChatColor.RED + " Expires On: " + ChatColor.GRAY + "Rent Exempted");
                                        
                                    } else {
                                        p.sendMessage(ChatColor.RED + " Expires On: " + ChatColor.GRAY + sdf.format(resultdate));
                                        
                                    }
                                }



                            } else {
                                p.sendMessage(ChatColor.RED + "That warp doesn't exist!");
                            }
                        } else {
                            p.sendMessage(ChatColor.RED + "Usage: /warp info <warp name>");
                        }
                    } else {
                        p.sendMessage(noPerm);
                    }
                } else if (permission.playerHas(p, "RentWarp.Player.Warp")) {
                    String s = args[0];
                    args[0] = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                    File WarpFolder = new File(getDataFolder(), "");
                    WarpFolder.mkdir();
                    File Warp = new File(WarpFolder, "warps.yml");
                    FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
                    if (warpConf.getString("Warps." + args[0]) != null) {

                    // time to teleport
                        
                        int x = warpConf.getInt("Warps." + args[0] + ".X");
                        int y = warpConf.getInt("Warps." + args[0] + ".Y");
                        int z = warpConf.getInt("Warps." + args[0] + ".Z");
                        
                        Location warp = new Location(Bukkit.getWorld(warpConf.getString("Warps." + args[0] + ".World")), x + 0.5, y, z + 0.5);

                        float yaw = warpConf.getInt("Warps." + args[0] + ".Yaw");
                        float pitch = warpConf.getInt("Warps." + args[0] + ".Pitch");
                         
                        String warpName = args[0];
                        String warpGreeting = warpConf.getString("Warps." + args[0] + ".Greeting");
                        scheduleWarp(p, warp, yaw, pitch, warpName, warpGreeting, (int) getConfig().getDouble("RentWarp.Settings.warpDelay"));
                    
                        
                    } else {
                        p.sendMessage(ChatColor.RED + "That Warp Doesnt Exist! ");
                    }
                } else {
                    p.sendMessage(noPerm);
                }

            }
        } else {
            Bukkit.getLogger().info("[RentWarp]: Only players can use rentwarp commands!");
        }
        return true;

    }


    

    

    private Boolean setupPermissions() {
        RegisteredServiceProvider < Permission > permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            permission = (Permission) permissionProvider.getProvider();
        }
        if (permission != null) {
            return Boolean.valueOf(true);
        }
        return Boolean.valueOf(false);
    }

    private Boolean setupEconomy() {
        RegisteredServiceProvider < Economy > economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = (Economy) economyProvider.getProvider();
        }
        if (economy != null) {
            return Boolean.valueOf(true);
        }
        return Boolean.valueOf(false);
    }

    public void loadConfiguration() {
        getConfig().options().copyDefaults(true);

        saveConfig();
    }
}