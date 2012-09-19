package org.maxgamer.maxbans.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.maxgamer.maxbans.MaxBans;

/**
 * Suggested database format:
 * Table: Bans
 * 	Name	Reason	Banner	Time	Expires	
 * 	Chuck	Spam	Azgod	143134	143956
 * 
 * Where,
 * 		Name (Lowercase) is the name of the player to ban,
 * 		Reason is the reason of the ban,
 * 		Banner is the admin (Lowercase) who banned them,
 * 		Time is the time the ban was created,
 * 		Expires is the time when the ban runs out.
 * 		If expires = 0, then it never expires.
 * 
 * Table: IPBans
 * 	IP		Reason	Banner	Time	Expires
 * 	127.*	Spam	Azgod	143134	143956
 * 
 * Where,
 * 		IP is the players IP,
 * 		Reason is the ban reason,
 * 		Banner (Lowercase) is the admin who banned them,
 * 		Time is the time the ban was created,
 * 		Expires is the time the ban will expire (0 = Never)
 * 
 * ====>	OLD	   <====
 * Table: IPHistory
 * Name	Time	IP			Count
 * Azgod	141134	127.0.0.1	1
 * Azgod	143234	192.168.2.1	15
 * Darek	143144	127.0.0.1	3
 * Daxter	133134	127.0.0.1	8
 * 
 * Where,
 * 		Name (Lowercase) is player name,
 * 		Time is last login on that IP,
 * 		IP is the ip address,
 * 		Count is the number of times they've logged in from that IP.
 * 
 * ====>	New	   <====
 * Table: IPHistory
 * Name		IP
 * Azgod	127.0.0.1
 * Azgod	192.168.2.1
 * Frizire	127.0.0.1
 * Daxter	192.168.2.15
 * 
 * Where,
 * 		Name + IP combo is unique
 * 		Name is the lowercase name
 * 		IP is the ip theyve been recorded on.
 *
 * Table: Mutes
 * Name		Muter	Time	Expires
 * Catty	Frizire	204234	0
 * 
 * Where,
 * 		Name is the muted player. (Lowercase)
 * 		Muter is the guy who muted them (Like banner)
 * 		Time is the time they were muted
 * 		Expires is the time that the mute expires. (0 = never)
 */
//TODO: Write create DB tables methods
public class Database {
	private Buffer buffer;
	private MaxBans plugin;
	private File dbFile;
	private DatabaseWatcher dbw;
	private int dbwID = 0;
	
	public Database(MaxBans plugin, File file){
		this.plugin = plugin;
		this.dbFile = file;
		this.buffer = new Buffer(this);
		this.dbw = new DatabaseWatcher(this);
	}
	
	/**
	 * Reschedules the db watcher
	 */
	public void scheduleWatcher(){
		Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, this.dbw, 300);
	}

	public Plugin getPlugin() {
		return this.plugin;
	}
	public Buffer getBuffer(){
		return this.buffer;
	}
	
	/**
	 * Returns true if the table exists
	 * @param table The table to check for
	 * @return True if the table is found
	 */
	public boolean hasTable(String table){
		String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+table+"'";
		try {
			PreparedStatement ps = this.getConnection().prepareStatement(query);
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()){
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			this.plugin.getLogger().severe(ChatColor.RED + "Could not create bans table.");
			return false;
		}
		return false;
	}
	
	/**
	 * Creates the bans table
	 */
	public void createBanTable(){
		String query = "CREATE TABLE 'bans' ( 'name'  TEXT(30) NOT NULL, 'reason'  TEXT(100), 'banner'  TEXT(30), 'time'  INTEGER NOT NULL DEFAULT 0, 'expires'  INTEGER NOT NULL DEFAULT 0 );";
		try {
			Statement st = this.getConnection().createStatement();
			st.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
			this.plugin.getLogger().severe(ChatColor.RED + "Could not create bans table.");
		}
	}
	
	/**
	 * Creates the IPBan table
	 */
	public void createIPBanTable(){
		String query = "CREATE TABLE 'ipbans' ( 'ip'  TEXT(20) NOT NULL, 'reason'  TEXT(100), 'banner'  TEXT(30), 'time'  INTEGER NOT NULL DEFAULT 0, 'expires'  INTEGER NOT NULL DEFAULT 0 );";
		try {
			Statement st = this.getConnection().createStatement();
			st.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
			this.plugin.getLogger().severe(ChatColor.RED + "Could not create ipbans table.");
		}
	}
	
	/**
	 * Creates the mutes table
	 */
	public void createMuteTable(){
		String query = "CREATE TABLE 'mutes' ( 'name'  TEXT(30) NOT NULL, 'muter'  TEXT(30), 'time'  INTEGER DEFAULT 0, 'expires'  INTEGER DEFAULT 0 );";
		try {
			Statement st = this.getConnection().createStatement();
			st.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
			this.plugin.getLogger().severe(ChatColor.RED + "Could not create mutes table.");
		}
	}
	
	/**
	 * Creates the iphistory table
	 */
	public void createIPHistoryTable(){
		String query = "CREATE TABLE 'iphistory' ( 'name'  TEXT(30) NOT NULL, 'ip'  TEXT(20) NOT NULL, PRIMARY KEY ('name', 'ip') );";
		try {
			Statement st = this.getConnection().createStatement();
			st.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
			this.plugin.getLogger().severe(ChatColor.RED + "Could not create iphistory table.");
		}
	}

	/**
	 * Gets the database connection for
	 * executing queries on.
	 * @return The database connection
	 */
	public Connection getConnection(){
		if(!this.dbFile.exists()){
			plugin.getLogger().info("CRITICAL: Database does not exist");
			try {
				this.dbFile.createNewFile();
				Class.forName("org.sqlite.JDBC");
				Connection dbCon = DriverManager.getConnection("jdbc:sqlite:" + this.dbFile);
				return dbCon;
			} 
			catch (IOException e) {
				e.printStackTrace();
				plugin.getLogger().info("Could not create file " + this.dbFile.toString());
			} 
			catch (ClassNotFoundException e) {
				e.printStackTrace();
				plugin.getLogger().info("You need the SQLite JBDC library.  Put it in MinecraftServer/lib folder.");
			} catch (SQLException e) {
				e.printStackTrace();
				plugin.getLogger().info("SQLite exception on initialize " + e);
			}
		}
		try{
			Class.forName("org.sqlite.JDBC");
			return DriverManager.getConnection("jdbc:sqlite:" + this.dbFile);
		}
		catch(SQLException e){
			e.printStackTrace();
			plugin.getLogger().info("SQLite exception on initialize.");
		}
		catch(ClassNotFoundException e){
			e.printStackTrace();
			plugin.getLogger().info("SQLite library not found, was it removed?");
		}
		return null;
	}
	
	
}
