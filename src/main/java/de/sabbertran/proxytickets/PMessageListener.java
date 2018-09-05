package de.sabbertran.proxytickets;

import de.sabbertran.proxytickets.objects.Location;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

public class PMessageListener implements Listener {
    private ProxyTickets main;

    public PMessageListener(ProxyTickets main) {
        this.main = main;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent ev) {

        if (ev.getTag().equals(ProxyTickets.ChannelName)) {
            ByteArrayInputStream stream = new ByteArrayInputStream(ev.getData());
            DataInputStream in = new DataInputStream(stream);
            try {
                String subchannel = in.readUTF();
                if (subchannel.equals("Permissions")) {
                    String player = in.readUTF();
                    String permission;
                    try {
                        while ((permission = in.readUTF()) != null) {
                            if (!main.getPermissionHandler().getPermissions().containsKey(player))
                                main.getPermissionHandler().getPermissions().put(player, new ArrayList<String>());
                            main.getPermissionHandler().getPermissions().get(player).add(permission.toLowerCase());
                        }
                    } catch (EOFException ex) {

                    }
                } else if (subchannel.equals("Position")) {
                    ProxiedPlayer p = main.getProxy().getPlayer(in.readUTF());
                    if (p != null) {
                        ServerInfo server = main.getProxy().getServerInfo(in.readUTF());
                        String world = in.readUTF();
                        double x = Double.parseDouble(in.readUTF());
                        double y = Double.parseDouble(in.readUTF());
                        double z = Double.parseDouble(in.readUTF());
                        float pitch = Float.parseFloat(in.readUTF());
                        float yaw = Float.parseFloat(in.readUTF());

                        Location loc = new Location(server, world, x, y, z, pitch, yaw);
                        main.getPositionHandler().locationReceived(p, loc);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}