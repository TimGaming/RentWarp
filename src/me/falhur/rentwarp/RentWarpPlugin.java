package me.fahlur.rentwarp;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.earth2me.essentials.Essentials;


public class rentwarp extends JavaPlugin {
	private static Plugin plugin;
	public static Economy economy = null;
	public static Permission permission = null;
	public static Essentials ess3 = null;
	PluginDescriptionFile pdfFile = this.getDescription();

	public void onDisable() {
		Bukkit.getLogger().info("[RentWarp] Has Been Disabled!");
		loadConfiguration();
		plugin = null;
	}

	public static String colorize(String msg) {
		String coloredMsg = "";
		for (int i = 0; i < msg.length(); i++) {
			if (msg.charAt(i) == '&') coloredMsg += '§';
			else coloredMsg += msg.charAt(i);
		}
		return coloredMsg;
	}
	
	public static void registerEvents(org.bukkit.plugin.Plugin plugin, Listener... listeners) {
		for (Listener listener : listeners) {
			Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
		}
	}
	public static Plugin getPlugin() {
		return plugin;
	}
	
	@Override
	public void onEnable() {
		loadConfiguration();
		loadWarpsFile();
		setupEconomy();
		setupPermissions();
		plugin = this;
		registerEvents(this, new WarperPlayerListener());
		ess3 = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
	    
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			
			public void run() {
				FileConfiguration warpConf = warpConfig();
				Set<String> s = warpConf.getConfigurationSection("Warps").getKeys(false);
				for (String temp: s) {
					
					
					long advancedTime = (long) warpConf.get("Warps." + temp + ".Time") - 604800000L;
					String Owner = (String) warpConf.get("Warps." + temp + ".Owner");
					Player player = (Player) Bukkit.getServer().getOfflinePlayer(UUID.fromString(Owner));
					String theOwner = Bukkit.getServer().getOfflinePlayer(UUID.fromString(Owner)).getName();
					long yourmilliseconds = (long) warpConf.get("Warps." + temp + ".Time");
					SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
					Date resultdate = new Date(yourmilliseconds);
					int Exempt = warpConf.getInt("Warps." + temp + ".Exempt");
					
					int RunLow = (int) warpConf.get("Warps." + temp + ".RunLow");
					if (System.currentTimeMillis() > advancedTime && advancedTime > 0 && RunLow != 1 && RunLow != 2 && Exempt != 1 && !permission.playerHas(player, "RentWarp.Warp.Exempt")) {
						
						// check if essentials is enabled
						if (ess3 != null) {
							    ess3.getUser(theOwner).addMail(ChatColor.RED + "NOTICE! " + ChatColor.WHITE + "Your warp " + ChatColor.AQUA + temp + ChatColor.WHITE + " expires on " + ChatColor.AQUA + sdf.format(resultdate) + ChatColor.WHITE + ", use " + ChatColor.LIGHT_PURPLE + "/addtime <warpName> " + ChatColor.WHITE + "to add more time!");								//Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mail send " + theOwner + " " + ChatColor.RED + "NOTICE! " + ChatColor.WHITE + "Your warp " + ChatColor.AQUA + temp + ChatColor.WHITE + " expires on " + ChatColor.AQUA + sdf.format(resultdate) + ChatColor.WHITE + ", use " + ChatColor.LIGHT_PURPLE + "/addtime <warpName> " + ChatColor.WHITE + "to add more time!");						
						}
						warpConf.set("Warps." + temp + ".RunLow", 1);
						try {
							warpConf.save(getDataFolder() + File.separator + "warps.yml");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					/* Check if warp expired, if so then Expire it */
					long checkTime = (long) warpConf.get("Warps." + temp + ".Time");
					long lastChanceDuration = 86400000 * (int) getConfig().getInt("Settings.lastChanceDuration");
					
					if (System.currentTimeMillis() > checkTime && RunLow == 1 && RunLow != 2 && Exempt != 1 && !permission.playerHas(player, "RentWarp.Warp.Exempt")) {
						warpConf.set("Warps." + temp + ".RunLow", 2);
						
						try {
							warpConf.save(getDataFolder() + File.separator + "warps.yml");
							Bukkit.getLogger().info("[RentWarp]: Warp : [" + temp + "] has expired!");
							
							// check if essentials is enabled
							if (ess3 != null) {
									long newDate = yourmilliseconds + lastChanceDuration;
									Date newResultdate = new Date(newDate);
									ess3.getUser(theOwner).addMail(ChatColor.RED + "NOTICE! " + ChatColor.WHITE + "Your warp " + ChatColor.AQUA + temp + ChatColor.WHITE + " has expired and you have until " + ChatColor.AQUA + sdf.format(newResultdate) + ChatColor.WHITE + " before your warp is deleted, use " + ChatColor.LIGHT_PURPLE + "/addtime <warpName> " + ChatColor.WHITE + "to add more time!");									//Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "mail send " + theOwner + " " + ChatColor.RED + "NOTICE! " + ChatColor.WHITE + "Your warp " + ChatColor.AQUA + temp + ChatColor.WHITE + " has expired and you have until " + ChatColor.AQUA + sdf.format(newResultdate) + ChatColor.WHITE + " before your warp is deleted, use " + ChatColor.LIGHT_PURPLE + "/addtime <warpName> " + ChatColor.WHITE + "to add more time!");
							}
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
					
					/* Check if warp is expired and past lastChanceDurationPeriod, if so delete it */

					if (System.currentTimeMillis() > checkTime + lastChanceDuration && (int) warpConf.get("Warps." + temp + ".RunLow") == 2 && Exempt != 1 && !permission.playerHas(player, "RentWarp.Warp.Exempt")) {
						warpConf.set("Warps." + temp, null);
						try {
							warpConf.save(getDataFolder() + File.separator + "warps.yml");
							Bukkit.getLogger().info("[RentWarp]: Expired warp exceeded lastChanceDuration and has been deleted : [" + temp + "]");
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
					
					
					
					
					
				}
			}
		}, 80);
	}
	
	private static HashMap < String, BukkitTask > warpers = new HashMap < String, BukkitTask > (); //Player Name -> Warp Task
	
	
	public void scheduleWarp(final Player player, final Location loc, final Object yaw, final Object pitch, final String warpName, final String warpGreeting, final int warpDelay) {
		if (!permission.playerHas(player, "RentWarp.Admin.CoolDownExempt")) {
			player.sendMessage(ChatColor.GOLD + "You will be teleported in " + ChatColor.RED + warpDelay + " seconds" + ChatColor.GOLD + ". Don't move.");
			Location atPlayersLocation = player.getLocation();
			player.getWorld().playEffect(atPlayersLocation, Effect.POTION_SWIRL, 5, 10);
			player.getWorld().playEffect(atPlayersLocation, Effect.POTION_SWIRL, 5, 10);
			player.getWorld().playEffect(atPlayersLocation, Effect.POTION_SWIRL, 5, 10);
			player.getWorld().playEffect(atPlayersLocation, Effect.POTION_SWIRL, 5, 10);
			player.getWorld().playEffect(atPlayersLocation, Effect.POTION_SWIRL, 5, 10);
			player.getWorld().playEffect(atPlayersLocation, Effect.POTION_SWIRL, 5, 10);
			player.getWorld().playEffect(atPlayersLocation, Effect.POTION_SWIRL, 5, 10);
			
		}
		
		BukkitRunnable runnable = new BukkitRunnable() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void run() {	
				player.sendMessage(ChatColor.GOLD + "Teleportation commencing...");
				Location atPlayersLocation = player.getLocation();
				player.getWorld().playEffect(atPlayersLocation, Effect.INSTANT_SPELL, 1);
				player.getWorld().playEffect(atPlayersLocation, Effect.INSTANT_SPELL, 1);
				player.getWorld().playEffect(atPlayersLocation, Effect.INSTANT_SPELL, 1);
				player.getWorld().playEffect(atPlayersLocation, Effect.INSTANT_SPELL, 1);
				if (ess3 != null){
                	ess3.getUser(player).setLastLocation();
				}
				player.teleport(loc);	
				player.playSound(loc, Sound.ENDERMAN_TELEPORT, 10, 1);
				player.playEffect(loc, Effect.ENDER_SIGNAL, 0);
				player.sendMessage("- " + colorize(warpGreeting));
				warpers.remove(player.getName());
			}
		};
		
		//Schedule the task to run later
		if (permission.playerHas(player, "RentWarp.Admin.CoolDownExempt")) {
			BukkitTask task = runnable.runTaskLater(plugin, 0);
			//Keep track of the player and their warp task
			warpers.put(player.getName().toString(), task);
		} else {
			BukkitTask task = runnable.runTaskLater(plugin, 20L * warpDelay);
			//Keep track of the player and their warp task
			warpers.put(player.getName().toString(), task);
		}
	}

	public static boolean isWarping(String player) {
		return warpers.containsKey(player);
	}
	
	public static void cancelWarp(String player) {
		//Check if the player is warping
		if (isWarping(player)) {
			//Remove the player as a warper
			//Cancel the task so that the player is not teleported
			warpers.remove(player).cancel();
		}
	}
	
	
	
	public static boolean isInt(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static String formatWarpName(String warpName) {
        return warpName.substring(0).toLowerCase();
	}
	
	public FileConfiguration warpConfig(){
		File WarpFolder = new File(getDataFolder(), "");
        WarpFolder.mkdir();
        File Warp = new File(WarpFolder, "warps.yml");
        FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
        return warpConf;
	}
	
	public static boolean specialChars(String string){
		Pattern pat = Pattern.compile("[^a-zA-Z0-9]");
        boolean hasSpecialChar = pat.matcher(string).find();
		if (!hasSpecialChar){
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean greetingSpecialChars(String string){
		Pattern pat = Pattern.compile("[^a-zA-Z0-9 _!\\&-]");
        boolean hasSpecialChar = pat.matcher(string).find();
		if (!hasSpecialChar){
			return true;
		}else{
			return false;
		}
	}
	
	

	
	private boolean validPosition(Location loc, World world, boolean isWarpTo, Player player, String warpName)
	{
		
		int yBelow = (int) (loc.getY() - 1);
		int yFeet = (int) (loc.getY());
		int yHead = (int) (loc.getY() + 1);
		String p = player.getName();
		String nloc = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
		
		Material below = world.getBlockAt(loc.getBlockX(), yBelow, loc.getBlockZ()).getType();
		Material feet = world.getBlockAt(loc.getBlockX(), yFeet, loc.getBlockZ()).getType();
		Material head = world.getBlockAt(loc.getBlockX(), yHead, loc.getBlockZ()).getType();
		
		
		if (head != Material.AIR) {
			if (isWarpTo == false){
				Bukkit.getLogger().info("[RentWarp]: " + p + " attempted to create an obstructed warp at [ " + nloc + " ]");
			}else{
				Bukkit.getLogger().info("[RentWarp]: " + p + " was prevented to warp to [ " + warpName + " ]");
			}
			return false;	
		}
		if (
				feet == Material.STATIONARY_LAVA || 
				feet == Material.STATIONARY_WATER || 
				feet == Material.CACTUS || 
				feet == Material.FIRE
			) {
			if (isWarpTo == false){
				Bukkit.getLogger().info("[RentWarp]: " + p + " attempted to create unsafe warp at [ " + nloc + " ]");
			}else{
				Bukkit.getLogger().info("[RentWarp]: " + p + " was prevented to warp to [ " + warpName + " ]");
			}
			return false;
		}
		if (
				below == Material.STATIONARY_LAVA || 
				below == Material.STATIONARY_WATER || 
				below == Material.CACTUS || 
				below == Material.FIRE || 
				below == Material.AIR
			) {
			
				if (below == Material.AIR) {
					int nbelow = loc.getWorld().getHighestBlockYAt(loc);
					below = world.getBlockAt(loc.getBlockX(), nbelow, loc.getBlockZ()).getType();
					if (below == Material.AIR){
						below = world.getBlockAt(loc.getBlockX(), nbelow+1, loc.getBlockZ()).getType();
						if (below == Material.AIR){
							below = world.getBlockAt(loc.getBlockX(), nbelow-1, loc.getBlockZ()).getType();
						}
					}
				}
			
			if (isWarpTo == false){
				int nbelow = loc.getWorld().getHighestBlockYAt(loc);
				int fallDistance = loc.getBlockY()-nbelow;
				Bukkit.getLogger().info("[RentWarp]: " + p + " attempted to create unsafe warp at [ " + nloc + " ] - Reason: " + fallDistance + " air blocks above " + below);
			}else{
				int nbelow = loc.getWorld().getHighestBlockYAt(loc);
				int fallDistance = loc.getBlockY()-nbelow;
				Bukkit.getLogger().info("[RentWarp]: " + p + " was prevented to warp to [ " + warpName + " ] - Reason: " + fallDistance + " air blocks above " + below);
			}
			return false;
		}
	  	
		
		return true;
	}
	
	
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if ((sender instanceof Player)) {
			
			// Collect Player Information
			Player p = (Player) sender;
			String playerName = p.getName();
			Player target = Bukkit.getServer().getPlayer(playerName);
			String PlayerUUID = target.getUniqueId().toString();

			
			
			// Preset Messages
			String noPerm = ChatColor.RED + "You do not have permission to do that!";
			String noExist = ChatColor.RED + "That warp doesnt exist!";
			String notOwner = ChatColor.RED + "You do not own this warp!";
			String noSpecialChars = ChatColor.RED + "Warp names must not have special characters!";
			String noMoney = ChatColor.RED + "You don't have enough money to buy a warp!";
			String noMoneyTime = ChatColor.RED + "You don't have enough money to extend your warps time!";
			String exists = ChatColor.RED + "That warp already exists!";
			String noCmd = ChatColor.RED + "That parameter does not exist!";
			String expiredWarp = ChatColor.RED + "The warp your trying to access is no longer available!";	
			String unSafeWarpCreation = ChatColor.RED + "The warp you are trying to create is in an unsafe location!";	
			String unSafeWarpLocation = ChatColor.RED + "The warp you are trying to go to is either unsafe or obstructed!";	
			String reachedMax = ChatColor.RED + "You can't add time that far into the future!";
			String alreadyExempt = ChatColor.RED + "You are exempted from warp fees! You do not need to /addtime!";
			String warpAlreadyExempt = ChatColor.RED + "This warp is exempted from fees! You do not need to /addtime to this warp!";
			
			
			if ((cmd.getName().equalsIgnoreCase("rentwarp"))) {
				if (permission.playerHas(p, "RentWarp.Player.rentwarpcommand")){
					if (args.length == 0){
						p.sendMessage(ChatColor.WHITE + "[RentWarp] " + pdfFile.getName() + " Version " + pdfFile.getVersion());
					}else{
						if (args[0].equalsIgnoreCase("reload")){
							if (permission.playerHas(p, "RentWarp.Admin.Reload")){
								reloadConfig();
		                        p.sendMessage(ChatColor.WHITE + "[RentWarp] " + ChatColor.GREEN + "Config has been reloaded!");
							} else { p.sendMessage(noPerm); }
						} else if (args[0].equalsIgnoreCase("help")) {
							if (permission.playerHas(p, "RentWarp.Player.Help")) {
								p.sendMessage("==========" + ChatColor.GREEN + "RentWarp Help" + ChatColor.WHITE + "==========");
								p.sendMessage(ChatColor.GOLD + "/rentwarp" + ChatColor.WHITE + " - Shows plugin name and version");
								if (permission.playerHas(p, "RentWarp.Admin.Reload")) {
			                        p.sendMessage(ChatColor.GOLD + "/rentwarp reload" + ChatColor.WHITE + " - Reloads all plugin configs");
			                    }
								if (permission.playerHas(p, "RentWarp.Admin.RentExempt")) {
			                        p.sendMessage(ChatColor.GOLD + "/warpexempt <warpName>" + ChatColor.WHITE + " - Exempts a warp from rent");
			                    }
								p.sendMessage(ChatColor.GOLD + "/rentwarp help" + ChatColor.WHITE + " - View the help page");
			                    p.sendMessage(ChatColor.GOLD + "/warpinfo <warpName>" + ChatColor.WHITE + " - View info on a warp");
			                    p.sendMessage(ChatColor.GOLD + "/warpcost" + ChatColor.WHITE + " - Tellls you price to create warp and to add time");
			                    p.sendMessage(ChatColor.GOLD + "/warp <warpName>" + ChatColor.WHITE + " - Teleport To a warp");
			                    p.sendMessage(ChatColor.GOLD + "/setwarp <warpName>" + ChatColor.WHITE + " - Create a warp");
			                    p.sendMessage(ChatColor.GOLD + "/delwarp <warpName>" + ChatColor.WHITE + " - Deletes a warp");
			                    p.sendMessage(ChatColor.GOLD + "/movewarp <warpName>" + ChatColor.WHITE + " - Moves a warp to your location");
			                    p.sendMessage(ChatColor.GOLD + "/addtime <warpName> <multiplier>" + ChatColor.WHITE + " - Add time to your warp in 30 day increments (1 by default)");
			                    p.sendMessage(ChatColor.GOLD + "/warpgreet <warpName> <msg>" + ChatColor.WHITE + " - Set a greeting for a warp");
			                    p.sendMessage(ChatColor.GOLD + "/warps" + ChatColor.WHITE + " - View All Warps");
			                    p.sendMessage(ChatColor.GOLD + "/mywarps" + ChatColor.WHITE + " - View Your Warps");
			                    p.sendMessage(ChatColor.GOLD + "/colors" + ChatColor.WHITE + " - List available colors for warp greetings");
							} else { p.sendMessage(noPerm); } 
		                    
					    } else { p.sendMessage(noCmd); }
					}
				}else{ p.sendMessage(noPerm); }			
			}
			
			
			
			
			
			// Colors Command
			if ((cmd.getName().equalsIgnoreCase("colors")) && args.length == 0) {
				p.sendMessage("Complete Color List: ");
				p.sendMessage(ChatColor.DARK_BLUE + " &1" + ChatColor.DARK_GREEN + " &2" + ChatColor.DARK_AQUA + " &3" + ChatColor.DARK_RED + " &4" + ChatColor.DARK_PURPLE + " &5" + ChatColor.GOLD + " &6" + ChatColor.GRAY + " &7" + ChatColor.DARK_GRAY + " &8" + ChatColor.BLUE + " &9" + ChatColor.BLACK + " &0" + ChatColor.GREEN + " &a" + ChatColor.AQUA + " &b" + ChatColor.RED + " &c" + ChatColor.LIGHT_PURPLE + " &d" + ChatColor.YELLOW + " &e" + ChatColor.WHITE + " &f");
			} // Colors Command
	

			// Warp Info Command
			if ((cmd.getName().equalsIgnoreCase("warpinfo"))) {
				if (permission.playerHas(p, "RentWarp.Player.Info")) {
                    if (args.length == 1) {
                        String warpName = formatWarpName(args[0]);
                        FileConfiguration warpConf = warpConfig();
                        if (warpConf.getString("Warps." + warpName) != null) {
                            long yourmilliseconds = (long) warpConf.get("Warps." + warpName + ".Time");
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
                            Date resultdate = new Date(yourmilliseconds);
                            p.sendMessage(ChatColor.GRAY + "--------------------------------------------------");
                            p.sendMessage(" Warp: " + ChatColor.LIGHT_PURPLE + warpName + ChatColor.WHITE + " Information");
                            p.sendMessage(ChatColor.GRAY + "--------------------------------------------------");
                            String warpOwner = warpConf.getString("Warps." + warpName + ".Owner");
                            Player player = (Player) Bukkit.getServer().getOfflinePlayer(UUID.fromString(warpOwner));
                            String greeting = warpConf.getString("Warps." + warpName + ".Greeting");
                            String getPlayer = Bukkit.getServer().getOfflinePlayer(UUID.fromString(warpOwner)).getName();
                            p.sendMessage(ChatColor.RED + " Greeting: " + ChatColor.WHITE + colorize(greeting));
                            p.sendMessage(ChatColor.RED + " Owner: " + ChatColor.GRAY + getPlayer);
                            p.sendMessage(ChatColor.RED + " Location: " + ChatColor.GRAY + warpConf.getString("Warps." + warpName + ".X") + ", " + warpConf.getString("Warps." + warpName + ".Y") + ", " + warpConf.getString("Warps." + warpName + ".Z"));
                           
                            if (warpConf.getString("Warps." + warpName + ".Owner").equals(PlayerUUID) || permission.playerHas(p, "RentWarp.Warp.Exempt")) {
                                if ((int) warpConf.get("Warps." + warpName + ".Exempt") == (int) 1 || permission.playerHas(player, "RentWarp.Warp.Exempt")) {
                                    p.sendMessage(ChatColor.RED + " Expires On: " + ChatColor.GRAY + "Rent Exempted");
                                } else {
                                	if (warpConf.getInt("Warps." + warpName + ".RunLow") <= 1){
                                		p.sendMessage(ChatColor.RED + " Expires On: " + ChatColor.GRAY + sdf.format(resultdate));
                                	}else{
                                		p.sendMessage(ChatColor.RED + " Expires On: " + ChatColor.GRAY + "Expired");
                                		long checkTime = (long) warpConf.get("Warps." + warpName + ".Time");
                    					long lastChanceDuration = 86400000 * (int) getConfig().getInt("Settings.lastChanceDuration");
                    					long lastChanceTime = checkTime + lastChanceDuration;
                                		Date lastChanceResultTime = new Date(lastChanceTime);
                                		p.sendMessage(ChatColor.RED + " Reactivate before " + ChatColor.GOLD + sdf.format(lastChanceResultTime) + ChatColor.RED + " or your warp will be deleted!");
                                	}
                                    
                                }
                            }
                        } else { p.sendMessage(noExist); }
                    } else { p.sendMessage(ChatColor.RED + "Usage: /warpinfo <warp name>");  }
                } else { p.sendMessage(noPerm); }
			} // Warp Info Command

			
			// Warp addtime Command
			if ((cmd.getName().equalsIgnoreCase("addtime"))) {
				if (permission.playerHas(p, "RentWarp.Player.Addtime")) {
					if (!permission.playerHas(p, "RentWarp.Warp.Exempt")) {
						if (args.length >= 1 && args.length < 3) {
		                    	String warpName = formatWarpName(args[0]);
		                    	FileConfiguration warpConf = warpConfig();
		                    	if (warpConf.getString("Warps." + warpName) != null) {
		                    		if (warpConf.getInt("Warps." + warpName + ".Exempt") != 1){
				                    	if (warpConf.getString("Warps." + warpName + ".Owner").equals(PlayerUUID) || permission.playerHas(p, "RentWarp.Admin.Addtime")) {
				                    		int multiplier = 1;
				                            if (args.length == 2 && isInt(args[1])) {
				                                multiplier = Integer.parseInt(args[1]);
				                            }
				                            
				                           int limit = (int) getConfig().getDouble("Settings.limitFutureAdd");
				                           long expireTime = (long) warpConf.get("Warps." + warpName + ".Time");
				                           long addTime = 2592000000L * multiplier;
				                           long limitMult = 2592000000L * limit;
				                           long currentTime = System.currentTimeMillis();
				                           boolean notMaxed = true;
				                           long maxTime = (long) currentTime + limitMult;
				                           long checkTime = (long) expireTime + addTime;
		
				                           
				                           if (checkTime > maxTime){
				                        	   notMaxed = false;
				                           }
				                           
				                           if (notMaxed){  //Check LimiteTime Not yet implemented
					                           if (economy.has(playerName, getConfig().getDouble("Economy.Cost2Renew") * multiplier)) {
					                                    long newtime = (long) warpConf.get("Warps." + warpName + ".Time") + (2592000000L * multiplier);
					                                    warpConf.set("Warps." + warpName + ".Time", newtime);
					                                    warpConf.set("Warps." + warpName + ".RunLow", 0);
					                                    try {
					                                        warpConf.save(getDataFolder() + File.separator + "warps.yml");
					                                        p.sendMessage(ChatColor.GREEN + "" + getConfig().getDouble("Economy.Cost2Renew") * multiplier + " " + economy.currencyNamePlural() + ChatColor.WHITE + " have been deducted from your account!");
					                                        economy.withdrawPlayer(playerName, getConfig().getDouble("Economy.Cost2Create") * multiplier);
					                                        p.sendMessage(ChatColor.WHITE + "Successfully added " + ChatColor.GREEN + multiplier + " Month(s)" + ChatColor.WHITE + " to your remaining time!");
					                                    } catch (IOException e) {
					                                        e.printStackTrace();
					                                    }  
					                           } else { p.sendMessage(noMoneyTime + " " + getConfig().getDouble("Economy.Cost2Renew") * multiplier); }
				                           } else { p.sendMessage(reachedMax); }
				                    	} else { p.sendMessage(notOwner); }
		                    		} else { p.sendMessage(warpAlreadyExempt); }

		                    	} else { p.sendMessage(noExist); }
		                    
		                } else { p.sendMessage(ChatColor.RED + "Usage: /addtime <warp name>"); }
					} else { p.sendMessage(alreadyExempt); }	
				} else { p.sendMessage(noPerm); }
			} // Warp addtime Command
			
			
			// Warp Cost Command
			if ((cmd.getName().equalsIgnoreCase("warpcost"))) {
				if(permission.playerHas(p, "RentWarp.Player.Create")){
					p.sendMessage(ChatColor.WHITE + "Initial cost: " + ChatColor.GREEN + getConfig().getInt("Economy.Cost2Create") + ChatColor.WHITE + " " + economy.currencyNamePlural());
	                p.sendMessage(ChatColor.WHITE + "Add 30 days: " + ChatColor.GREEN + getConfig().getInt("Economy.Cost2Renew") + ChatColor.WHITE + " " + economy.currencyNamePlural());
				} else { p.sendMessage(noPerm); }
			} // Warp Cost Command
			
			// Warp Exempt Command
			if ((cmd.getName().equalsIgnoreCase("warpexempt"))) {
                    if (permission.playerHas(p, "RentWarp.Admin.RentExempt")) {
                    	if (args.length == 1) {
	                        String warpName = formatWarpName(args[0]);
	                        FileConfiguration warpConf = warpConfig();
	                        if (warpConf.getString("Warps." + warpName) != null) {
	                            String exempt = "Un-Exempted";
	                            if ((int) warpConf.get("Warps." + warpName + ".Exempt") == (int) 1) {
	                                warpConf.set("Warps." + warpName + ".Exempt", (int) 0);
	                                exempt = "Un-Exempted";
	                            } else {
	                                warpConf.set("Warps." + warpName + ".Exempt", (int) 1);
	                                exempt = "Exempted";
	                            }
	                            try {
	                                warpConf.save(getDataFolder() + File.separator + "warps.yml");
	                                p.sendMessage(ChatColor.WHITE + "Warp successfully " + ChatColor.RED + exempt + ChatColor.WHITE + " from rent!");
	                            } catch (IOException e) {
	                                e.printStackTrace();
	                            }
	                        } else { p.sendMessage(noExist); }
                    	} else { p.sendMessage(ChatColor.RED + "Usage: /warpexempt <warp name>"); }    
                    } else { p.sendMessage(noPerm); }

			} // Warp Exempt Command
			
			
			

			
			// delwarp Command
			if ((cmd.getName().equalsIgnoreCase("delwarp"))) {
				if (permission.playerHas(p, "RentWarp.Player.Delete") || permission.playerHas(p, "RentWarp.Admin.Delete")) {
					if (args.length == 1){
							String warpName = formatWarpName(args[0]);
							FileConfiguration warpConf = warpConfig();
	                        if (warpConf.getString("Warps." + warpName) != null) {
	                            if (warpConf.getString("Warps." + warpName + ".Owner").equals(PlayerUUID) || permission.playerHas(p, "RentWarp.Admin.Delete")) {
	                                warpConf.set("Warps." + warpName, null);
	                                try {
	                                    warpConf.save(getDataFolder() + File.separator + "warps.yml");
	                                    p.sendMessage("Warp " + ChatColor.AQUA + warpName + ChatColor.WHITE + " Has Been Deleted!");
	                                    Bukkit.getLogger().info("[RentWarp]: " + playerName + " deleted the warp [" + warpName + "]");
	                                } catch (IOException e) {
	                                    e.printStackTrace();
	                                }
	
	                            } else { p.sendMessage(notOwner); }
	                        } else { p.sendMessage(noExist); }
						
					}else{ p.sendMessage(ChatColor.RED + "Usage: /delwarp <warpName>"); }
				}else{ p.sendMessage(noPerm); }
			} // delwarp Command
		
		
			// setwarp Command
			if ((cmd.getName().equalsIgnoreCase("setwarp"))) {
				if (permission.playerHas(p, "RentWarp.Player.Create")) {
					if (args.length == 1){
						
							String warpName = formatWarpName(args[0]);
		                    if (specialChars(warpName)) {
		                        if (economy.has(playerName, getConfig().getDouble("Economy.Cost2Create"))) {
		                        	
		                        	if (validPosition(p.getLocation(), p.getWorld(), false, p, warpName)){
			                            File WarpFolder = new File(getDataFolder(), "");
			                            WarpFolder.mkdir();
			                            File Warp = new File(WarpFolder, "warps.yml");
			                            FileConfiguration warpConf = YamlConfiguration.loadConfiguration(Warp);
			                            if (warpConf.getString("Warps." + warpName) == null) {
			                            		try {
				                                    Warp.createNewFile();
				                                } catch (IOException e) {
				                                    e.printStackTrace();
				                                }
				                                String greeting = (String) "Welcome&3 %name% &fto &3" + warpName;
				                                warpConf.createSection("Warps." + warpName);
				                                warpConf.set("Warps." + warpName + ".World", p.getWorld().getName());
				                                warpConf.set("Warps." + warpName + ".X", Integer.valueOf(p.getLocation().getBlockX()));
				                                warpConf.set("Warps." + warpName + ".Y", Integer.valueOf(p.getLocation().getBlockY()));
				                                warpConf.set("Warps." + warpName + ".Z", Integer.valueOf(p.getLocation().getBlockZ()));
				                                warpConf.set("Warps." + warpName + ".Yaw", Float.valueOf(p.getLocation().getYaw()));
				                                warpConf.set("Warps." + warpName + ".Pitch", Float.valueOf(p.getLocation().getPitch()));
				                                warpConf.set("Warps." + warpName + ".Owner", PlayerUUID.toString());
				                                warpConf.set("Warps." + warpName + ".Greeting", greeting);
				                                warpConf.set("Warps." + warpName + ".Time", System.currentTimeMillis() + 2592000000L);
				                                warpConf.set("Warps." + warpName + ".Exempt", (int) 0);
				                                warpConf.set("Warps." + warpName + ".RunLow", (int) 0);
				                                
				                                try {
				                                    warpConf.save(getDataFolder() + File.separator + "warps.yml");
				                                    if (!permission.playerHas(p, "RentWarp.Warp.Exempt")){
				                                    	economy.withdrawPlayer(playerName, getConfig().getDouble("Economy.Cost2Create"));
				                                    	p.sendMessage("You Paid " + ChatColor.GREEN + "$" + getConfig().getString("Economy.Cost2Create") + " " + economy.currencyNamePlural() + ChatColor.WHITE + " To Create a Warp named " + ChatColor.AQUA + warpName + ChatColor.WHITE + "!");
				                                    }else{
				                                    	p.sendMessage("You Paid " + ChatColor.GREEN + "$0 " + economy.currencyNamePlural() + ChatColor.WHITE + " To Create a Warp named " + ChatColor.AQUA + warpName + ChatColor.WHITE + "!");
				                                    }
				                                    
				                                    Bukkit.getLogger().info("[RentWarp]: " + playerName + " created a warp [" + warpName + "] at X(" + Integer.valueOf(p.getLocation().getBlockX()) + ") Y(" + Integer.valueOf(p.getLocation().getBlockY()) + ") Z(" + Integer.valueOf(p.getLocation().getBlockZ()) + ")");
				                                } catch (IOException e) {
				                                    e.printStackTrace();
				                                }
			                            }else{ p.sendMessage(exists); }
		                        } else { 
		                        	p.sendMessage(unSafeWarpCreation); 
		                        }
		                        } else { p.sendMessage(noMoney); }
		                    } else { p.sendMessage(noSpecialChars); }
		                
							
		                   					
					}else{ p.sendMessage(ChatColor.RED + "Usage: /setwarp <warpName>"); }
				} else { p.sendMessage(noPerm); }
			} // setwarp Command
			
			// MoveWarp Command
			if ((cmd.getName().equalsIgnoreCase("movewarp"))) {
				if(permission.playerHas(p, "RentWarp.Player.Move")){
					String warpName = formatWarpName(args[0]);
					FileConfiguration warpConf = warpConfig();
	                if (warpConf.getString("Warps." + warpName) != null) {
	                    if (warpConf.getString("Warps." + warpName + ".Owner").equals(PlayerUUID) || permission.playerHas(p, "RentWarp.Admin.Move")) {
	                    	if (warpConf.getInt("Warps." + warpName + ".RunLow") <= 1){
	                    		warpConf.set("Warps." + warpName + ".X", Integer.valueOf(p.getLocation().getBlockX()));
		                        warpConf.set("Warps." + warpName + ".Y", Integer.valueOf(p.getLocation().getBlockY()));
		                        warpConf.set("Warps." + warpName + ".Z", Integer.valueOf(p.getLocation().getBlockZ()));
		                        warpConf.set("Warps." + warpName + ".Yaw", Float.valueOf(p.getLocation().getYaw()));
		                        warpConf.set("Warps." + warpName + ".Pitch", Float.valueOf(p.getLocation().getPitch()));
		                        try {
		                            warpConf.save(getDataFolder() + File.separator + "warps.yml");
		                            p.sendMessage(ChatColor.GREEN + "Warp successfully moved!");
		                            Bukkit.getLogger().info("[RentWarp]: " + playerName + " moved a warp [" + warpName + "] at X(" + Integer.valueOf(p.getLocation().getBlockX()) + ") Y(" + Integer.valueOf(p.getLocation().getBlockY()) + ") Z(" + Integer.valueOf(p.getLocation().getBlockZ()) + ")");
		                        } catch (IOException e) {
		                            e.printStackTrace();
		                        }
	                    	} else { p.sendMessage(expiredWarp); }
	                    } else { p.sendMessage(notOwner); }
	                } else { p.sendMessage(noExist); }
				} else { p.sendMessage(noPerm); }
			} // MoveWarp Command
			
			
			// WarpGreet Command
			if ((cmd.getName().equalsIgnoreCase("warpgreet"))) {
				if (permission.playerHas(p, "RentWarp.Player.Greeting")) {
                    if (args.length >= 1) {
                        String myString = ""; //we're going to store the arguments here    
                        for (int i = 0; i < args.length; i++) { //loop threw all the arguments
                            if (i != 0 && i != 0) {
                                String arg = args[i] + " "; //get the argument, and add a space so that the words get spaced out
                                myString = myString + arg; //add the argument to myString   
                            }
                        }
                        if (greetingSpecialChars(myString)) {
                            String warpName = formatWarpName(args[0]);
                            FileConfiguration warpConf = warpConfig();
                            if (warpConf.getString("Warps." + warpName) != null) {
                                if (warpConf.getString("Warps." + warpName + ".Owner").equals(PlayerUUID) || permission.playerHas(p, "RentWarp.Admin.Greet")) {
                                	if (warpConf.getInt("Warps." + warpName + ".RunLow") <= 1){
                                		if (args.length == 1){
                                            myString = (String) "Welcome&3 %name% &fto &3" + warpName;
                                        }
                                        warpConf.set("Warps." + warpName + ".Greeting", myString);
                                        try {
                                            warpConf.save(getDataFolder() + File.separator + "warps.yml");
                                            if (args.length == 1){
                                                p.sendMessage(ChatColor.GRAY + "Greeting successfully reset to default!");
                                            }else{
                                                p.sendMessage(ChatColor.GREEN + "Greeting Updated: " + ChatColor.WHITE + colorize(myString));
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        Bukkit.getLogger().info("[RentWarp]: " + playerName + " updated the following warps greeting [" + warpName + "] to " + myString);
                                	} else { p.sendMessage(expiredWarp); }
                                    
                                } else { p.sendMessage(notOwner); }
                            }else{ p.sendMessage(noExist); }
                        } else { p.sendMessage(noSpecialChars); }
                    } else { p.sendMessage(ChatColor.RED + "Usage: /warpgreet <warp name> <greeting message>"); }
                } else { p.sendMessage(noPerm); }
			} // WarpGreet Command
			
			// My Warp, List my warps
			if ((cmd.getName().equalsIgnoreCase("mywarps"))) {
				if(permission.playerHas(p, "RentWarp.Player.Create")){
					FileConfiguration warpConf = warpConfig();
					p.sendMessage(ChatColor.RED + "MyWarps");
					int i = 1;
					for(String parent : warpConf.getConfigurationSection("Warps").getKeys(false))
					{
					  if(warpConf.get("Warps." + parent + ".Owner").equals(PlayerUUID))
					  {
						  long yourmilliseconds = (long) warpConf.get("Warps." + parent + ".Time");
						  SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
						  Date resultdate = new Date(yourmilliseconds);
	
						  String result = ChatColor.WHITE + sdf.format(resultdate);  
						  if (warpConf.getInt("Warps." + parent + ".Exempt") == 1) {
							result = ChatColor.GOLD + "Rent Exempted";
						  }else if (warpConf.getInt("Warps." + parent + ".RunLow") == 2) {
							 result = ChatColor.RED + "Expired";
						  }
							
						  p.sendMessage(ChatColor.GRAY + "" + i + ". " + ChatColor.WHITE +  parent + ChatColor.GRAY +  " -- Expires on: " + result);
	
						  i++;
					  }
					}
				} else { p.sendMessage(noPerm); }
				
			} // My Warp, List my warps

			// Warp Command
			if ((cmd.getName().equalsIgnoreCase("warp"))) {
				if (permission.playerHas(p, "RentWarp.Player.Warp")) {
					if (args.length != 0){
						
		                    String warpName = formatWarpName(args[0]);
		                    FileConfiguration warpConf = warpConfig();
		                    if (warpConf.getString("Warps." + warpName) != null) {
		                    	if (warpConf.getInt("Warps." + warpName + ".RunLow") <= 1) {
		                    		
		                    		
			                    		int x = warpConf.getInt("Warps." + warpName + ".X");
			                            int y = warpConf.getInt("Warps." + warpName + ".Y");
			                            int z = warpConf.getInt("Warps." + warpName + ".Z");
			                   
			                    
			                            float yaw = warpConf.getInt("Warps." + warpName + ".Yaw");
			                            float pitch = warpConf.getInt("Warps." + warpName + ".Pitch");
			                            Location warp = new Location(Bukkit.getWorld(warpConf.getString("Warps." + warpName + ".World")), x + 0.5, y, z + 0.5, yaw, pitch);
			                            if (validPosition(warp, Bukkit.getWorld(warpConf.getString("Warps." + warpName + ".World")), true, p, warpName)){
				                            String warpGreeting = warpConf.getString("Warps." + warpName + ".Greeting");
				                            String result = warpGreeting.replaceAll("%name%", playerName);
				                            
				                            scheduleWarp(p, warp, yaw, pitch, warpName, result, (int) getConfig().getDouble("Settings.warpDelay"));
			                            } else { 
			                            	p.sendMessage(unSafeWarpLocation); 
			                            }
		                    	} else { p.sendMessage(expiredWarp); }
		                    } else { p.sendMessage(noExist); }
						
					}else{ 
						if (permission.playerHas(p, "RentWarp.Player.List")) {
							if(args.length == 0) {
								FileConfiguration warpConf = warpConfig();
			                    Set<String> s = warpConf.getConfigurationSection("Warps").getKeys(false);
			                    int itemcount = s.size();
			                    p.sendMessage(ChatColor.GOLD + "Warps (" + ChatColor.GRAY + itemcount + ChatColor.GOLD + "): " + ChatColor.GRAY + s); 
							}else{ p.sendMessage(ChatColor.RED + "Usage /warps"); }
		                } else {  p.sendMessage(noPerm); }
					}
				}else{ p.sendMessage(noPerm); }
			} // Warp Command
			

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
	
	public void loadWarpsFile() {
		File WarpFolder = new File(getDataFolder(), "");
		WarpFolder.mkdir();
		File Warp = new File(WarpFolder, "warps.yml");
		if (!Warp.exists()) {
			FileConfiguration warpConf = warpConfig();
			warpConf.createSection("Warps");
			try {
				warpConf.save(getDataFolder() + File.separator + "warps.yml");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	}
}
