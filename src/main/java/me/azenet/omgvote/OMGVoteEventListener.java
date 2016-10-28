/*
    OMGVote
    Copyright (C) 2016 azenet

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.azenet.omgvote;

import com.intellectualcrafters.plot.object.Plot;
import me.azenet.omgvote.exception.VoteFailedException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Wool;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

public class OMGVoteEventListener implements Listener {
	private OMGVote plugin;

	public OMGVoteEventListener(OMGVote plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerSpawn(PlayerSpawnLocationEvent ev) {
		this.setPlayerInventory(ev.getPlayer());
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent ev) {
		this.setPlayerInventory(ev.getPlayer());
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent ev) {
		this.setPlayerInventory(ev.getPlayer());
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent ev) {
		ev.setCancelled(true);

		if (ev.getInventory().getName().startsWith("Voter") && ev.getWhoClicked() instanceof Player && ev.getCurrentItem() != null && ev.getCurrentItem().getType().equals(Material.WOOL)) {
			playerClickedConfirmationInventory((Player) ev.getWhoClicked(), ev.getCurrentItem());
		} else if (ev.getInventory() instanceof PlayerInventory && ev.getWhoClicked() instanceof Player) {
			onVoteItemInteraction((Player) ev.getWhoClicked(), ev.getCursor());
		}
	}

	@EventHandler
	public void onDropItem(PlayerDropItemEvent ev) {
		ev.setCancelled(true);
	}

	private void playerClickedConfirmationInventory(Player p, ItemStack is) {
		if (((Wool) is.getData()).getColor().equals(DyeColor.GREEN)) {
			Plot plot = PlotHelper.getPlotForPlayerLocation(p);
			String owner = PlotHelper.getOwnersForPlot(plot);

			try {
				plugin.getSql().castVote(p.getName(), plot);
			} catch (VoteFailedException vfe) {
				p.closeInventory();
				p.sendMessage(vfe.getMessage());
				return;
			} catch (SQLException sqle) {
				p.closeInventory();
				p.sendMessage(ChatColor.RED + "Une erreur est survenue.");
				return;
			}

			int cnt;
			try {
				cnt = plugin.getSql().getVoteCountForPlayer(p.getName());
			} catch (SQLException sqle) {
				sqle.printStackTrace();
				p.sendMessage(ChatColor.RED + "Une erreur est survenue.");
				return;
			}

			int maxVotes = plugin.getConfig().getInt("max-votes");
			int left = maxVotes - cnt;

			p.sendMessage(ChatColor.GREEN + "  Vous avez voté pour le terrain de " + owner + ".");
			if (left > 0) {
				p.sendMessage(ChatColor.GOLD + "  Il vous reste " + ChatColor.GOLD + left + ChatColor.GOLD + " vote" + (left > 1 ? "s" : "") + ".");
			} else {
				p.sendMessage(ChatColor.GOLD + "  Vous n'avez plus de votes à attribuer.");
			}
		}

		p.closeInventory();
		setPlayerInventory(p);
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent ev) {
		ev.setCancelled(true);
	}

	@EventHandler
	public void onPlayerSwap(PlayerSwapHandItemsEvent ev) {
		ev.setCancelled(true);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent ev) {
		ItemStack item = ev.getPlayer().getInventory().getItemInMainHand();
		if (item != null && !item.getType().equals(Material.AIR)) {
			ev.setCancelled(true);
			onVoteItemInteraction(ev.getPlayer(), item);
		}
	}

	@EventHandler
	public void onPlayerFoodEvent(FoodLevelChangeEvent ev) {
		ev.setCancelled(true);
	}

	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent ev) {
		if (!ev.getPlayer().isOp()) {
			ev.setCancelled(true);
		}
	}

	private void onVoteItemInteraction(Player p, ItemStack is) {
		if (is.getType().equals(Material.GOLDEN_CARROT)) {
			openVoteConfirmationInventory(p);
		} else if (is.getType().equals(Material.BARRIER)) {
			int maxVotes = plugin.getConfig().getInt("max-votes");
			p.sendMessage(ChatColor.RED + "Vous avez déjà voté pour " + maxVotes + " terrains.");
		}
	}

	private void openVoteConfirmationInventory(Player p) {
		Plot plot = PlotHelper.getPlotForPlayerLocation(p);
		String owner = PlotHelper.getOwnersForPlot(plot);

		Inventory voteInv = Bukkit.createInventory(p, 9, "Voter pour le terrain de " + owner + " ?");

		int cnt;
		try {
			cnt = plugin.getSql().getVoteCountForPlayer(p.getName());
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			p.sendMessage(ChatColor.RED + "Une erreur est survenue.");
			return;
		}

		int maxVotes = plugin.getConfig().getInt("max-votes");
		int left = maxVotes - cnt;
		if (left <= 0) {
			setPlayerInventory(p);
			return;
		}

		ItemStack yes = new ItemStack(Material.WOOL, 1, DyeColor.GREEN.getWoolData());
		ItemMeta im = yes.getItemMeta();
		im.setDisplayName(ChatColor.GREEN + "Oui");
		if (left > 1) {
			int leftAfter = left - 1;
			im.setLore(Arrays.asList(ChatColor.GOLD + "Il vous restera", ChatColor.GREEN + "" + leftAfter + ChatColor.GOLD + " vote" + (leftAfter > 1 ? "s" : "") + "."));
		} else {
			im.setLore(Collections.singletonList(ChatColor.GOLD + "Il s'agit de votre dernier vote."));
		}
		yes.setItemMeta(im);

		ItemStack no = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
		im = no.getItemMeta();
		im.setDisplayName(ChatColor.RED + "Non");
		no.setItemMeta(im);

		voteInv.setItem(2, yes);
		voteInv.setItem(6, no);

		p.closeInventory();
		p.openInventory(voteInv);
	}

	private void setPlayerInventory(Player p) {
		ItemStack is;
		try {
			int cnt = plugin.getSql().getVoteCountForPlayer(p.getName());
			int maxVotes = plugin.getConfig().getInt("max-votes");
			if (cnt < maxVotes) {
				int left = maxVotes - cnt;
				is = new ItemStack(Material.GOLDEN_CARROT, left);
				ItemMeta im = is.getItemMeta();
				im.setDisplayName(ChatColor.GREEN + "Voter");
				im.setLore(Arrays.asList(
						ChatColor.GOLD + "Vous pouvez encore voter",
						ChatColor.GOLD + "pour " + ChatColor.GREEN + left + ChatColor.GOLD + " terrain" + (left > 1 ? "s" : "") + "."
				));
				is.setItemMeta(im);
			} else {
				is = new ItemStack(Material.BARRIER);
				ItemMeta im = is.getItemMeta();
				im.setDisplayName(ChatColor.RED + "Voter");
				im.setLore(Arrays.asList(ChatColor.GOLD + "Vous avez épuisé", ChatColor.GOLD + "vos " + ChatColor.GREEN + maxVotes + ChatColor.GOLD + " votes."));
				is.setItemMeta(im);
			}
		} catch (SQLException ex) {
			p.kickPlayer("Internal server error");
			ex.printStackTrace();
			throw new RuntimeException("SQL failed for player " + p.getName());
		}

		p.getInventory().clear();
		p.getInventory().setArmorContents(new ItemStack[]{null, null, null, null});
		p.getInventory().setItem(4, is);
		p.setFoodLevel(20);
		p.setSaturation(20F);
	}
}
