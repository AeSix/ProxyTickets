package de.sabbertran.proxytickets;

import de.sabbertran.proxytickets.objects.Comment;
import de.sabbertran.proxytickets.objects.Ticket;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Events implements Listener {
    private ProxyTickets main;
    private ArrayList<ProxiedPlayer> justJoined;

    public Events(ProxyTickets main) {
        this.main = main;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent ev) {
        ProxiedPlayer p = ev.getPlayer();
        try {
            PreparedStatement pst = main.getSQLConnection().prepareStatement("SELECT uuid, name FROM players WHERE uuid = ?");
            pst.setString(1, p.getUniqueId().toString());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                if (!rs.getString("name").equals(p.getName())) {
                    PreparedStatement pst2 = main.getSQLConnection().prepareStatement("UPDATE players SET name = ? WHERE uuid = ?");
                    pst2.setString(1, p.getName());
                    pst2.setString(2, p.getUniqueId().toString());
                    pst2.executeUpdate();
                }
            } else {
                PreparedStatement pst2 = main.getSQLConnection().prepareStatement("INSERT INTO players (uuid, name) VALUES (?, ?)");
                pst2.setString(1, p.getUniqueId().toString());
                pst2.setString(2, p.getName());
                pst2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        HashMap<Ticket, Comment> openComments = main.getTicketHandler().getOpenCommentsFromOpenTickets(main.getCachedPlayerHandler().getCachedPlayer(p));
        if (!openComments.isEmpty()) {
            main.getMessageHandler().sendMessage(p, main.getMessageHandler().getMessage("ticket.comments.unread").replace("%amount%", String.valueOf(openComments.size())));
            for (Map.Entry<Ticket, Comment> entry : openComments.entrySet()) {
                main.getMessageHandler().sendMessage(p, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comments.entry"), entry.getKey(), entry.getValue()));
                entry.getValue().setRead(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent ev) {
        main.getPermissionHandler().resetPermissions(ev.getPlayer());
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent ev) {
        final ProxiedPlayer p = ev.getPlayer();

        main.getProxy().getScheduler().schedule(main, new Runnable() {
            public void run() {
                main.getPermissionHandler().resetPermissions(p);
                main.getPermissionHandler().updatePermissions(p);
            }
        }, 500, TimeUnit.MILLISECONDS);
    }
}
