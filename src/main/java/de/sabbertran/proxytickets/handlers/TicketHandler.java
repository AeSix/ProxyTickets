package de.sabbertran.proxytickets.handlers;

import de.sabbertran.proxytickets.ProxyTickets;
import de.sabbertran.proxytickets.objects.CachedPlayer;
import de.sabbertran.proxytickets.objects.Comment;
import de.sabbertran.proxytickets.objects.Location;
import de.sabbertran.proxytickets.objects.Ticket;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TicketHandler {
    private TicketHandler instance;
    private ProxyTickets main;
    private List<Ticket> openTickets;
    private HashMap<UUID, Boolean> pendingPurges;

    public TicketHandler(ProxyTickets main) {
        this.instance = this;
        this.main = main;
        openTickets = new LinkedList<Ticket>();
        pendingPurges = new HashMap<UUID, Boolean>();
    }

    public void sendOpenTicketList(CommandSender p, int start, int amount) {
        if (openTickets.size() > 0) {
            if (start < openTickets.size()) {
                main.getMessageHandler().sendMessage(p, main.getMessageHandler().getMessage("ticket.list.header")
                        .replace("%page%", String.valueOf((start / main.getTicketsPerPage()) + 1))
                        .replace("%maxPage%", String.valueOf(((int) Math.ceil(((double) openTickets.size()) / main.getTicketsPerPage()))))
                        .replace("%showing%", String.valueOf(main.getTicketsPerPage()))
                        .replace("%open%", String.valueOf(openTickets.size())));
                int count = 0, sent = 0;
                ListIterator<Ticket> iterator = openTickets.listIterator(openTickets.size());
                while (iterator.hasPrevious()) {
                    Ticket t = iterator.previous();
                    if (count >= start && sent < amount) {
                        main.getMessageHandler().sendMessage(p, translateTicketVariables(main.getMessageHandler().getMessage("ticket.list.entry." + t.getStatus()), t));
                        sent++;
                    } else if (sent >= amount)
                        break;
                    count++;
                }
                main.getMessageHandler().sendMessage(p, main.getMessageHandler().getMessage("ticket.list.footer"));
            } else {
                main.getMessageHandler().sendMessage(p, main.getMessageHandler().getMessage("ticket.list.wrongpage").replace("%pages%", String.valueOf(((int) Math.ceil(((double) openTickets.size()) / main.getTicketsPerPage())))));
            }
        } else {
            main.getMessageHandler().sendMessage(p, main.getMessageHandler().getMessage("ticket.list.noopen"));
        }
    }

    public void sendTicket(CommandSender p, Ticket t) {
        main.getMessageHandler().sendMessage(p, translateTicketVariables(main.getMessageHandler().getMessage("ticket." + t.getStatus()), t));
    }

    public void sendTicketComments(CommandSender p, Ticket t) {
        if (t.getComments().size() > 0) {
            main.getMessageHandler().sendMessage(p, translateTicketVariables(main.getMessageHandler().getMessage("ticket.comments.header"), t));
            ListIterator<Comment> iterator = t.getComments().listIterator(t.getComments().size());
            while (iterator.hasPrevious()) {
                Comment c = iterator.previous();
                main.getMessageHandler().sendMessage(p, translateTicketVariables(main.getMessageHandler().getMessage("ticket.comments.entry"), t, c));
            }
            main.getMessageHandler().sendMessage(p, translateTicketVariables(main.getMessageHandler().getMessage("ticket.comments.footer"), t));
        } else {
            main.getMessageHandler().sendMessage(p, translateTicketVariables(main.getMessageHandler().getMessage("ticket.comments.nocomments"), t));
        }
    }

    public String translateTicketVariables(String text, Ticket t) {
        return translateTicketVariables(text, t, null);
    }

    public String translateTicketVariables(String text, Ticket t, Comment c) {
        text = text.replace("%id%", String.valueOf(t.getId()));
        text = text.replace("%author%", t.getPlayer().getName());
        text = text.replace("%created%", main.getDateFormat().format(t.getCreated()));
        text = text.replace("%server%", t.getLocation().getServer().getName());
        text = text.replace("%world%", t.getLocation().getWorld());
        text = text.replace("%x%", String.valueOf(t.getLocation().getX()));
        text = text.replace("%y%", String.valueOf(t.getLocation().getY()));
        text = text.replace("%z%", String.valueOf(t.getLocation().getZ()));
        text = text.replace("%pitch%", String.valueOf(t.getLocation().getPitch()));
        text = text.replace("%yaw%", String.valueOf(t.getLocation().getYaw()));
        text = text.replace("%text%", t.getText());
        text = text.replace("%claimedBy%", t.getClaimedBy() != null ? t.getClaimedBy().getName() : "");
        text = text.replace("%answer%", t.getAnswer() != null ? t.getAnswer() : "");
        if (c != null) {
            text = text.replace("%commentAuthor%", c.getPlayer().getName());
            text = text.replace("%commentCreated%", main.getDateFormat().format(c.getDate()));
            text = text.replace("%commentText%", c.getText());
            text = text.replace("%commentServer%", c.getLocation().getServer().getName());
            text = text.replace("%commentWorld%", c.getLocation().getWorld());
            text = text.replace("%commentX%", String.valueOf(c.getLocation().getX()));
            text = text.replace("%commentY%", String.valueOf(c.getLocation().getY()));
            text = text.replace("%commentZ%", String.valueOf(c.getLocation().getZ()));
            text = text.replace("%commentPitch%", String.valueOf(c.getLocation().getPitch()));
            text = text.replace("%commentYaw%", String.valueOf(c.getLocation().getYaw()));
        }
        return text;
    }

    public void loadOpenTickets() {
        openTickets.clear();
        main.getProxy().getScheduler().runAsync(main, new Runnable() {
            public void run() {
                try {
                    String sql = "SELECT t.id, t.status, t.created, t.server, t.world, t.x, t.y, t.z, t.pitch, t.yaw, t.text, t.answer, p1.uuid AS authorUUID, p1.name AS authorName, p2.uuid AS claimedByUUID, p2.name AS claimedByName " +
                            "FROM tickets t " +
                            "INNER JOIN players p1 ON t.player = p1.uuid " +
                            "LEFT JOIN players p2 ON t.claimedBy = p2.uuid " +
                            "WHERE t.status = 0 OR t.status = 1";
                    PreparedStatement pst = main.getSQLConnection().prepareStatement(sql);
                    ResultSet rs = pst.executeQuery();
                    while (rs.next()) {
                        Location loc = new Location(main.getProxy().getServerInfo(rs.getString("server")), rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("pitch"), rs.getFloat("yaw"));
                        CachedPlayer author = main.getCachedPlayerHandler().getCachedPlayer(UUID.fromString(rs.getString("authorUUID")), rs.getString("authorName"));
                        CachedPlayer claimedBy = (rs.getString("claimedByUUID") != null && rs.getString("claimedByName") != null) ? claimedBy = main.getCachedPlayerHandler().getCachedPlayer(UUID.fromString(rs.getString("claimedByUUID")), rs.getString("claimedByName")) : null;
                        Ticket t = new Ticket(instance, rs.getInt("id"), rs.getInt("status"), author, rs.getTimestamp("created"), loc, rs.getString("text"), claimedBy, rs.getString("answer"));
                        openTickets.add(t);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isLoadedTicket(int id) {
        for (Ticket t : openTickets)
            if (t.getId() == id)
                return true;
        return false;
    }

    public Ticket getTicket(int id) {
        for (Ticket t : openTickets)
            if (t.getId() == id)
                return t;

        try {
            String sql = "SELECT t.id, t.status, t.created, t.server, t.world, t.x, t.y, t.z, t.pitch, t.yaw, t.text, t.answer, p1.uuid AS authorUUID, p1.name AS authorName, p2.uuid AS claimedByUUID, p2.name AS claimedByName\n" +
                    "FROM tickets t\n" +
                    "INNER JOIN players p1 ON t.player = p1.uuid\n" +
                    "LEFT JOIN players p2 ON t.claimedBy = p2.uuid\n" +
                    "WHERE t.id = ?";
            PreparedStatement pst = main.getSQLConnection().prepareStatement(sql);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Location loc = new Location(main.getProxy().getServerInfo(rs.getString("server")), rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"), rs.getFloat("pitch"), rs.getFloat("yaw"));
                CachedPlayer author = main.getCachedPlayerHandler().getCachedPlayer(UUID.fromString(rs.getString("authorUUID")), rs.getString("authorName"));
                CachedPlayer claimedBy = (rs.getString("claimedByUUID") != null && rs.getString("claimedByName") != null) ? claimedBy = main.getCachedPlayerHandler().getCachedPlayer(UUID.fromString(rs.getString("claimedByUUID")), rs.getString("claimedByName")) : null;
                Ticket t = new Ticket(instance, rs.getInt("id"), rs.getInt("status"), author, rs.getTimestamp("created"), loc, rs.getString("text"), claimedBy, rs.getString("answer"));
                return t;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Ticket openTicket(ProxiedPlayer player, Location loc, String text) {
        CachedPlayer cp = main.getCachedPlayerHandler().getCachedPlayer(player.getUniqueId(), player.getName());
        Ticket t = new Ticket(this, cp, new Date(), loc, text, null);
        openTickets.add(t);
        return t;
    }

    public void claimTicket(final Ticket t, final CachedPlayer p) {
        t.setStatus(1);
        t.setClaimedBy(p);
        main.getProxy().getScheduler().runAsync(main, new Runnable() {
            public void run() {
                try {
                    PreparedStatement pst = main.getSQLConnection().prepareStatement("UPDATE tickets SET status = ?, claimedBy = ? WHERE id = ?");
                    pst.setInt(1, 1);
                    pst.setString(2, p.getUUID());
                    pst.setInt(3, t.getId());
                    pst.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void unclaimTicket(final Ticket t) {
        t.setStatus(0);
        t.setClaimedBy(null);
        main.getProxy().getScheduler().runAsync(main, new Runnable() {
            public void run() {
                try {
                    PreparedStatement pst = main.getSQLConnection().prepareStatement("UPDATE tickets SET status = ?, claimedBy = ? WHERE id = ?");
                    pst.setInt(1, 0);
                    pst.setString(2, null);
                    pst.setInt(3, t.getId());
                    pst.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void closeTicket(final Ticket t, final CachedPlayer p, final String answer) {
        t.setStatus(2);
        t.setClaimedBy(p);
        t.setAnswer(answer);
        final int id = t.getId();
        main.getProxy().getScheduler().runAsync(main, new Runnable() {
            public void run() {
                try {
                    PreparedStatement pst = main.getSQLConnection().prepareStatement("UPDATE tickets SET status = ?, claimedBy = ?, answer = ? WHERE id = ?");
                    pst.setInt(1, 2);
                    pst.setString(2, p.getUUID());
                    pst.setString(3, answer);
                    pst.setInt(4, id);
                    pst.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        openTickets.remove(t);
    }

    public void teleportToTicket(ProxiedPlayer p, Ticket t) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Teleport");
            out.writeUTF(p.getName());
            out.writeUTF(t.getLocation().getWorld());
            out.writeUTF(String.valueOf(t.getLocation().getX()));
            out.writeUTF(String.valueOf(t.getLocation().getY()));
            out.writeUTF(String.valueOf(t.getLocation().getZ()));
            out.writeUTF(String.valueOf(t.getLocation().getPitch()));
            out.writeUTF(String.valueOf(t.getLocation().getYaw()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        t.getLocation().getServer().sendData(ProxyTickets.ChannelName, b.toByteArray());
        if (p.getServer().getInfo() != t.getLocation().getServer())
            p.connect(t.getLocation().getServer());
    }

    public Comment commentTicket(final Ticket t, final CachedPlayer p, final String comment, final Location loc) {
        final boolean read = p.equals(t.getPlayer()) ? (t.getClaimedBy() != null ? t.getClaimedBy().getBungeePlayer() != null : true) : (t.getPlayer().getBungeePlayer() != null ? true : false);
        final Comment c = new Comment(this, -1, p, new Date(), comment, loc, read);
        t.getComments().add(c);
        main.getProxy().getScheduler().runAsync(main, new Runnable() {
            public void run() {
                try {
                    PreparedStatement pst = main.getSQLConnection().prepareStatement("INSERT INTO comments (ticket, player, date, text, server, world, x, y, z, pitch, yaw, isread) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                    pst.setInt(1, t.getId());
                    pst.setString(2, p.getUUID());
                    pst.setTimestamp(3, new Timestamp(c.getDate().getTime()));
                    pst.setString(4, comment);
                    pst.setString(5, loc.getServer().getName());
                    pst.setString(6, loc.getWorld());
                    pst.setDouble(7, loc.getX());
                    pst.setDouble(8, loc.getY());
                    pst.setDouble(9, loc.getZ());
                    pst.setFloat(10, loc.getPitch());
                    pst.setFloat(11, loc.getYaw());
                    pst.setBoolean(12, read);
                    pst.executeUpdate();
                    ResultSet rs = pst.getGeneratedKeys();
                    if (rs.next())
                        c.setId(rs.getInt(1));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        return c;
    }

    public HashMap<Ticket, Comment> getOpenCommentsFromOpenTickets(CachedPlayer p) {
        HashMap<Ticket, Comment> ret = new HashMap<Ticket, Comment>();
        for (Ticket t : openTickets)
            if (t.getPlayer().equals(p) || p.equals(t.getClaimedBy()))
                for (Comment c : t.getComments())
                    if (!c.isRead() && !c.getPlayer().equals(p))
                        ret.put(t, c);
        return ret;
    }

    public boolean createPurgeRequest(final ProxiedPlayer p, boolean all) {
        if (!pendingPurges.containsKey(p.getUniqueId())) {
            pendingPurges.put(p.getUniqueId(), all);
            main.getProxy().getScheduler().schedule(main, new Runnable() {
                public void run() {
                    pendingPurges.remove(p.getUniqueId());
                }
            }, 60, TimeUnit.SECONDS);
            return true;
        } else
            return false;
    }

    public boolean purgeDatabase(ProxiedPlayer p) {
        if (pendingPurges.containsKey(p.getUniqueId())) {
            if (pendingPurges.remove(p.getUniqueId())) {
                main.getProxy().getScheduler().runAsync(main, new Runnable() {
                    public void run() {
                        try {
                            PreparedStatement pst = main.getSQLConnection().prepareStatement("TRUNCATE TABLE comments");
                            pst.executeUpdate();
                            pst = main.getSQLConnection().prepareStatement("TRUNCATE TABLE tickets");
                            pst.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
                openTickets.clear();
            } else {
                main.getProxy().getScheduler().runAsync(main, new Runnable() {
                    public void run() {
                        try {
                            PreparedStatement pst = main.getSQLConnection().prepareStatement("DELETE FROM comments WHERE ticket IN (SELECT id FROM tickets WHERE status = 2)");
                            pst.executeUpdate();
                            pst = main.getSQLConnection().prepareStatement("DELETE FROM tickets WHERE status = 2");
                            pst.executeUpdate();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            return true;
        } else
            return false;
    }

    public ProxyTickets getMain() {
        return main;
    }
}
