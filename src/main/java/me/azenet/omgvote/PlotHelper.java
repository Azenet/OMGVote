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

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.util.MainUtil;
import com.plotsquared.bukkit.util.BukkitUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlotHelper {
	public static Plot getPlotForPlayerLocation(Player p) {
		Location plotLoc = BukkitUtil.getLocation(p);
		PlotArea area = plotLoc.getPlotArea();
		Plot plot = area.getPlot(plotLoc);

		if (null == plot) {
			p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "  Vous devez être au dessus d'un terrain pour voter.");
			return null;
		}

		if (plot.getOwners().isEmpty()) {
			p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "  Ce terrain n'est pas occupé.");
			return null;
		}

		if (plot.isOwner(p.getUniqueId())) {
			p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "  Vous ne pouvez pas voter pour vous-même.");
			return null;
		}

		return plot;
	}

	public static String getOwnersForPlot(Plot plot) {
		String owner = "";
		for (UUID u : plot.getOwners()) {
			owner = owner + MainUtil.getName(u) + ",";
		}

		return owner.substring(0, owner.length() - 1);
	}
}
