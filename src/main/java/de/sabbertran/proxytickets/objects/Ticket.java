package de.sabbertran.proxytickets.objects;

import de.sabbertran.proxytickets.handlers.TicketHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class Ticket {
    private TicketHandler handler;
    private int id, status;
    private CachedPlayer player, claimedBy;
    private Date created;
    private Location location;
    private String text, answer;
    private List<Comment> comments;

    public Ticket(TicketHandler handler, CachedPlayer player, Date created, Location location, String text, CachedPlayer claimedBy) {
        this(handler, 0, player, created, location, text, claimedBy);
    }

    public Ticket(final TicketHandler handler, final int status, final CachedPlayer player, final Date created, final Location location, final String text, final CachedPlayer claimedBy) {
        this(handler, -1, status, player, created, location, text, claimedBy, null);
        handler.getMain().getProxy().getScheduler().runAsync(handler.getMain(), new Runnable() {
            public void run() {
                try {
                    PreparedStatement pst = handler.getMain().getSQLConnection().prepareStatement("INSERT INTO tickets (player, status, created, server, world, x, y, z, pitch, yaw, text, claimedBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                    pst.setString(1, player.getUUID());
                    pst.setInt(2, status);
                    pst.setTimestamp(3, new Timestamp(created.getTime()));
                    pst.setString(4, location.getServer().getName());
                    pst.setString(5, location.getWorld());
                    pst.setDouble(6, location.getX());
                    pst.setDouble(7, location.getY());
                    pst.setDouble(8, location.getZ());
                    pst.setDouble(9, location.getPitch());
                    pst.setDouble(10, location.getYaw());
                    pst.setString(11, text);
                    pst.setString(12, claimedBy != null ? claimedBy.getUUID() : null);
                    pst.executeUpdate();
                    ResultSet rs = pst.getGeneratedKeys();
                    if (rs.next())
                        id = rs.getInt(1);
                    sendCreationMessages();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public Ticket(final TicketHandler handler, final int id, int status, CachedPlayer player, Date created, Location location, String text, CachedPlayer claimedBy, String answer) {
        this.handler = handler;
        this.id = id;
        this.player = player;
        this.created = created;
        this.location = location;
        this.text = text;
        this.claimedBy = claimedBy;
        this.answer = answer;
        this.status = status;
        comments = new LinkedList<Comment>();

        try {
            PreparedStatement pst = handler.getMain().getSQLConnection().prepareStatement("SELECT p.uuid, p.name, c.date, c.text, c.server, c.world, c.id, c.x, c.y, c.z, c.pitch, c.yaw, c.isread FROM comments c INNER JOIN players p ON c.player = p.uuid WHERE c.ticket = ?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                CachedPlayer cp = handler.getMain().getCachedPlayerHandler().getCachedPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                Location loc = new Location(handler.getMain().getProxy().getServerInfo(rs.getString("server")), rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("pitch"), rs.getFloat("yaw"));
                Comment c = new Comment(handler, rs.getInt("id"), cp, rs.getTimestamp("date"), rs.getString("text"), loc, rs.getBoolean("isread"));
                comments.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void sendCreationMessages() {
        handler.getMain().getMessageHandler().sendMessage(player.getBungeePlayer(), handler.getMain().getTicketHandler().translateTicketVariables(handler.getMain().getMessageHandler().getMessage("ticket.created.success"), this));
        for (ProxiedPlayer team : handler.getMain().getProxy().getPlayers())
            if (handler.getMain().getPermissionHandler().hasPermission(team, "proxytickets.ticket.receiveteaminfo"))
                handler.getMain().getMessageHandler().sendMessage(team, handler.getMain().getTicketHandler().translateTicketVariables(handler.getMain().getMessageHandler().getMessage("ticket.created.teaminfo"), this));
    }

    public int getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public CachedPlayer getPlayer() {
        return player;
    }

    public Date getCreated() {
        return created;
    }

    public Location getLocation() {
        return location;
    }

    public String getText() {
        return text;
    }

    public CachedPlayer getClaimedBy() {
        return claimedBy;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setClaimedBy(CachedPlayer claimedBy) {
        this.claimedBy = claimedBy;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
