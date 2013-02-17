package org.maxgamer.maxbans.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.maxgamer.maxbans.util.Formatter;
import org.maxgamer.maxbans.util.IPAddress;
import org.maxgamer.maxbans.util.RangeBan;
import org.maxgamer.maxbans.util.Util;

public class RangeBanCommand extends CmdSkeleton{
	public RangeBanCommand() {
		super("maxbans.rangeban");
		usage = "/rb IP1-IP2... Example: /rb 192.168.2.1-192.168.2.4 bans 192.168.2.1, .2, .3 and .4";
		minArgs = 1;
	}

	@Override
	public boolean run(CommandSender sender, Command cmd, String label, String[] args) {
		String banner = Util.getName(sender);
		boolean silent = Util.isSilent(args);
		String reason = Util.buildReason(args);
		
		String[] ips = args[0].split("-");
		
		if(ips.length != 2){
			sender.sendMessage(ChatColor.RED + "Not enough IP addresses supplied! Usage: " + usage);
			return true;
		}
		
		ips[0] = ips[0].replace('*', '0');
		ips[1] = ips[1].replace("*", "255");
		
		for(int i = 0; i < ips.length; i++){
			if(!Util.isIP(ips[i])){
				sender.sendMessage(ChatColor.RED + ips[i] + " is not a valid IP address.");
				return true;
			}
		}
		
		IPAddress start = new IPAddress(ips[0]);
		IPAddress end = new IPAddress(ips[1]);
		
		RangeBan rb = new RangeBan(banner, reason, System.currentTimeMillis(), start, end);
		boolean result = plugin.getBanManager().getRanger().ban(rb);
		if(result == false){
			sender.sendMessage(ChatColor.RED + "That RangeBan overlaps another RangeBan!");
			return true;
		}
		plugin.getBanManager().announce(Formatter.secondary + banner + Formatter.primary + " RangeBanned " + Formatter.secondary + rb.toString() + Formatter.primary + ".", silent, sender);
		
		return true;
	}
	
}