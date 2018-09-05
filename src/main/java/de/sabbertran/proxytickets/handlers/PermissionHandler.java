package de.sabbertran.proxytickets.handlers;

import de.sabbertran.proxytickets.ProxyTickets;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PermissionHandler {

    private ProxyTickets main;
    private List<String> availablePermissions;
    private HashMap<String, List<String>> permissions;

    public PermissionHandler(ProxyTickets main) {
        this.main = main;
        permissions = new HashMap<String, List<String>>();
        availablePermissions = new ArrayList<String>();
    }

    public void updatePermissions(ProxiedPlayer p) {

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("GetPermissions");
            out.writeUTF(p.getName());
            for (String perm : availablePermissions)
                out.writeUTF(perm);
        } catch (IOException e) {
            e.printStackTrace();
        }
        p.getServer().sendData(ProxyTickets.ChannelName, b.toByteArray());
    }

    public void readAvailablePermissionsFromFile() {
        try {
            BufferedReader read = new BufferedReader(new InputStreamReader(main.getClass().getResourceAsStream
                    ("/availablePermissions.yml")));
            String line;
            while ((line = read.readLine()) != null) {
                line = line.trim();
                if (!line.trim().equals("") && !line.startsWith("#"))
                    availablePermissions.add(line.trim());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetPermissions(CommandSender player) {
        permissions.remove(player.getName());
    }

    public void resetPermissions(String player) {
        permissions.remove(player);
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        return hasPermission(sender, permission, false);
    }

    public boolean hasPermission(CommandSender sender, String permission, boolean ignoreSternchen) {
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer p = (ProxiedPlayer) sender;
            if (permissions.containsKey(p.getName())) {
                if (permissions.get(p.getName()).contains(permission.toLowerCase()) ||
                        (!ignoreSternchen && permissions.get(p.getName()).contains("*")) ||
                        (!ignoreSternchen && permissions.get(p.getName()).contains(permission.toLowerCase() + ".*")))
                    return true;
                else if (!ignoreSternchen) {
                    String check = "";
                    for (String s : permission.toLowerCase().split("\\.")) {
                        check = check + s + ".";
                        if (permissions.get(p.getName()).contains(check + "*")) {
                            main.getLogger().info(sender.getName() + " has '" + check + "*'");
                            return true;
                        }
                    }
                }
            }
        } else
            return true;

        ProxyTickets.log.info("hasPermission failed to find user "+ sender.getName()+" perm, checking list for:  " + permission);
        for(String perm : permissions.keySet()){
            ProxyTickets.log.info("hasPermission: Key: "+ perm + " Perm: " + permissions.get(perm));
        }

        return false;
    }

    public boolean hasPermission(String player, String permission) {

        ProxyTickets.log.info("ProxyTickets - Player: " + player + " permissions: " + permission);

        if (permissions.containsKey(player)) {
            if (permissions.get(player).contains(permission.toLowerCase()) || permissions.get(player)
                    .contains("*"))
                return true;
            else {
                String check = "";
                for (String s : permission.toLowerCase().split("\\.")) {
                    check = check + s + ".";
                    if (permissions.get(player).contains(check + "*"))
                        return true;
                }
            }
        }
        return false;
    }

    public void sendMissingPermissionInfo(CommandSender sender) {
        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.nopermission"));
    }

    public HashMap<String, List<String>> getPermissions() {
        return permissions;
    }

    public List<String> getAvailablePermissions() {
        return availablePermissions;
    }
}
