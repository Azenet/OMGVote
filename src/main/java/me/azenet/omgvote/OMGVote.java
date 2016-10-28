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

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Logger;

public class OMGVote extends JavaPlugin {
	private Logger l = this.getLogger();
	private static String version = "1.0.0";
	private MySQLHelper sql = null;

	public void onDisable() {
		if (this.sql != null) {
			try {
				this.sql.disconnectDB();
			} catch (SQLException sqle) {
				l.warning("Failed to disconnect from database");
				sqle.printStackTrace();
			}
		}

		l.info("OMGVote v." + version + " unloaded");
	}

	public void onEnable() {
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(new OMGVoteEventListener(this), this);

		this.saveDefaultConfig();
		this.sql = new MySQLHelper(this);
		try {
			this.sql.connectDB();
		} catch (SQLException sqle) {
			l.severe("Failed to connect to database");
			sqle.printStackTrace();
			pm.disablePlugin(this);

			return;
		}

		l.info("OMGVote v." + version + " loaded");
	}

	public MySQLHelper getSql() {
		return sql;
	}
}
