package me.falhur.rentwarp.Listeners;

import me.falhur.rentwarp.RentWarpPlugin.RentWarpPlugin;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WarpListener implements Listener {
    public WarpListener(RentWarpPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
          @EventHandler (priority = EventPriority.MONITOR)
          public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            String playerName = player.getName();

            //We only care about this event if the player is flagged as warping
            if (RentWarpPlugin.isWarping(playerName)) {
              //Compare the block locations rather than the player locations
              //This allows a player to move their head without canceling the warp
              Block blockFrom = event.getFrom().getBlock();
              Block blockTo = event.getTo().getBlock();

              //Cancel the warp if the player moves to a different block
              if (!blockFrom.equals(blockTo)) {
                RentWarpPlugin.cancelWarp(playerName);
                player.sendMessage(ChatColor.RED + "Warping canceled because you moved!");
              }
            
          }
        }
}