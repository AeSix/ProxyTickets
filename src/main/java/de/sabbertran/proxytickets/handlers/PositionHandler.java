package de.sabbertran.proxytickets.handlers;

import de.sabbertran.proxytickets.ProxyTickets;
import de.sabbertran.proxytickets.objects.Location;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class PositionHandler {

    private ProxyTickets main;
    private HashMap<UUID, Runnable> positionRunnables;
    private HashMap<UUID, Location> localPositions;

    public PositionHandler(ProxyTickets main) {
        this.main = main;
        positionRunnables = new HashMap<UUID, Runnable>();
        localPositions = new HashMap<UUID, Location>();
    }

    public void requestPosition(ProxiedPlayer p) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("GetPosition");
            out.writeUTF(p.getName());
            out.writeUTF(p.getServer().getInfo().getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        p.getServer().sendData(ProxyTickets.ChannelName, b.toByteArray());
    }

    public void locationReceived(ProxiedPlayer p, Location loc) {
        if (positionRunnables.containsKey(p.getUniqueId())) {
            localPositions.put(p.getUniqueId(), loc);
            positionRunnables.remove(p.getUniqueId()).run();
        }
    }

    public void addPositionRunnable(ProxiedPlayer p, Runnable run) {
        positionRunnables.put(p.getUniqueId(), run);
    }

    public HashMap<UUID, Location> getLocalPositions() {
        return localPositions;
    }
}
