package de.sabbertran.proxytickets.objects;

import de.sabbertran.proxytickets.handlers.TicketHandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

public class Comment {
    private TicketHandler handler;
    private int id;
    private CachedPlayer player;
    private Date date;
    private String text;
    private Location loc;
    private boolean read;

    public Comment(TicketHandler handler, int id, CachedPlayer player, Date date, String text, Location loc, boolean read) {
        this.handler = handler;
        this.id = id;
        this.player = player;
        this.date = date;
        this.text = text;
        this.loc = loc;
        this.read = read;
    }

    public int getId() {
        return id;
    }

    public CachedPlayer getPlayer() {
        return player;
    }

    public Date getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public Location getLocation() {
        return loc;
    }

    public boolean isRead() {
        return read;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setRead(final boolean read) {
        this.read = read;
        handler.getMain().getProxy().getScheduler().runAsync(handler.getMain(), new Runnable() {
            public void run() {
                try {
                    PreparedStatement pst = handler.getMain().getSQLConnection().prepareStatement("UPDATE comments SET isread = ? WHERE id = ?");
                    pst.setBoolean(1, read);
                    pst.setInt(2, id);
                    pst.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
