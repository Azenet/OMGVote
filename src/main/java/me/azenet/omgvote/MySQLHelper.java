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
import org.bukkit.ChatColor;

import java.sql.*;

public class MySQLHelper {
	private Connection db;
	private OMGVote plugin;

	public MySQLHelper(OMGVote plugin) {
		this.plugin = plugin;
	}

	private boolean tableExists(Connection c, String s) throws SQLException {
		ResultSet rs = c.getMetaData().getTables(null, null, s, null);

		while (rs.next()) {
			String tableName = rs.getString("TABLE_NAME");
			if (tableName != null && tableName.equals(s)) {
				return true;
			}
		}

		return false;
	}

	private void setUpDB() throws SQLException {
		if (!tableExists(db, "omgvote_votes")) {
			plugin.getLogger().info("Creating SQL data...");
			PreparedStatement reqVotes = db.prepareStatement("CREATE TABLE omgvote_votes(" +
					"id INT(11) UNSIGNED AUTO_INCREMENT PRIMARY KEY," +
					"cell VARCHAR(255) NOT NULL," +
					"owner VARCHAR(255) NOT NULL," +
					"voter VARCHAR(255) NOT NULL" +
					");");

			reqVotes.executeUpdate();
		}
	}

	public void connectDB() throws SQLException {
		if (!isConnected()) {
			plugin.getLogger().info("Connecting to database...");

			String host = this.plugin.getConfig().getString("sql.host", "localhost");
			Integer port = this.plugin.getConfig().getInt("sql.port", 3306);
			String dbname = this.plugin.getConfig().getString("sql.db");
			String username = this.plugin.getConfig().getString("sql.user");
			String password = this.plugin.getConfig().getString("sql.pass");

			this.db = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbname, username, password);
			this.db.setAutoCommit(true);

			this.setUpDB();
		}
	}

	public void disconnectDB() throws SQLException {
		if (isConnected()) {
			plugin.getLogger().info("Disconnecting from database...");
			db.close();
		}
	}

	public boolean isConnected() {
		return db != null;
	}

	public int getVoteCountForPlayer(String playerName) throws SQLException {
		playerName = playerName.toLowerCase();

		PreparedStatement req = db.prepareStatement("SELECT COUNT(*) as votes FROM omgvote_votes WHERE voter = ?");
		req.setString(1, playerName);

		ResultSet rs = req.executeQuery();
		if (rs.next()) {
			return rs.getInt("votes");
		} else {
			return 0;
		}
	}

	public boolean castVote(String voter, Plot plot) throws SQLException, VoteFailedException {
		voter = voter.toLowerCase();

		int cnt = plugin.getSql().getVoteCountForPlayer(voter);
		int maxVotes = plugin.getConfig().getInt("max-votes");
		int left = maxVotes - cnt;

		if (left <= 0) {
			throw new VoteFailedException(ChatColor.RED + "  Vous ne pouvez plus voter.");
		}

		String owner = PlotHelper.getOwnersForPlot(plot);

		PreparedStatement req = db.prepareStatement("INSERT INTO omgvote_votes(cell, owner, voter) VALUES(?, ?, ?);");
		req.setString(1, plot.getId().x + ";" + plot.getId().y);
		req.setString(2, owner);
		req.setString(3, voter);

		req.executeUpdate();

		return true;
	}
}
