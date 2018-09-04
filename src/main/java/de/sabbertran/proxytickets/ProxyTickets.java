package de.sabbertran.proxytickets;

import com.google.common.io.ByteStreams;
import de.sabbertran.proxytickets.handlers.*;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.List;

public class ProxyTickets extends Plugin {
    private Configuration config;
    private List<String> sql;
    private Connection sql_connection;
    private MessageHandler messageHandler;
    private PermissionHandler permissionHandler;
    private PositionHandler positionHandler;
    private CommandHandler commandHandler;
    private CachedPlayerHandler cachedPlayerHandler;
    private TicketHandler ticketHandler;
    private SimpleDateFormat dateFormat;
    private int ticketsPerPage;

    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        File messagesFile = new File(getDataFolder(), "messages.yml");
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
                InputStream is = getResourceAsStream("config.yml");
                OutputStream os = new FileOutputStream(configFile);
                ByteStreams.copy(is, os);
            }
            if (!messagesFile.exists()) {
                messagesFile.createNewFile();
                InputStream is = getResourceAsStream("messages.yml");
                OutputStream os = new FileOutputStream(messagesFile);
                ByteStreams.copy(is, os);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create file", e);
        }
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sql = config.getStringList("ProxyTickets.SQL");
        dateFormat = new SimpleDateFormat(config.getString("ProxyTickets.Messages.TimeFormat"));
        ticketsPerPage = config.getInt("ProxyTickets.TicketList.TicketsPerPage");

        setupDatabase();

        messageHandler = new MessageHandler(this);
        permissionHandler = new PermissionHandler(this);
        positionHandler = new PositionHandler(this);
        commandHandler = new CommandHandler(this);
        cachedPlayerHandler = new CachedPlayerHandler(this);
        ticketHandler = new TicketHandler(this);

        commandHandler.registerCommands();
        ticketHandler.loadOpenTickets();
        messageHandler.readMessagesFromFile();
        permissionHandler.readAvailablePermissionsFromFile();

        getProxy().getPluginManager().registerListener(this, new Events(this));
        getProxy().registerChannel("minecats:proxytickets");
        getProxy().getPluginManager().registerListener(this, new PMessageListener(this));

        getLogger().info(getDescription().getName() + " " + getDescription().getVersion() + " by " + getDescription().getAuthor() + " enabled");
    }

    public void onDisable() {
        getLogger().info(getDescription().getName() + " " + getDescription().getVersion() + " by " + getDescription().getAuthor() + " disabled");
    }

    public void setupDatabase() {
        if (sql != null && sql.size() == 5 && !sql.get(4).equals("Password")) {
            try {
                Statement st = getSQLConnection().createStatement();
                st.execute("CREATE TABLE IF NOT EXISTS players (uuid CHAR(36) NOT NULL, name VARCHAR(16) NOT NULL, blockedUntil TIMESTAMP NULL, UNIQUE (uuid))");
                st.execute("CREATE TABLE IF NOT EXISTS tickets (id INT NOT NULL AUTO_INCREMENT, player CHAR(36) NOT NULL, status INT NOT NULL DEFAULT 0, created TIMESTAMP NOT NULL, server VARCHAR(255) NOT NULL, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, pitch DOUBLE NOT NULL, yaw DOUBLE NOT NULL, text TEXT NOT NULL, claimedBy CHAR(36) NULL, answer TEXT NULL, PRIMARY KEY (id), FOREIGN KEY (player) REFERENCES players (uuid), FOREIGN KEY (claimedBy) REFERENCES players (uuid))");
                st.execute("CREATE TABLE IF NOT EXISTS comments (id INT NOT NULL AUTO_INCREMENT, ticket INT NOT NULL, player CHAR(36) NOT NULL, date TIMESTAMP NOT NULL, text TEXT NOT NULL, server VARCHAR(255) NOT NULL, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, pitch DOUBLE NOT NULL, yaw DOUBLE NOT NULL, isread BOOLEAN NOT NULL, PRIMARY KEY (id), FOREIGN KEY (ticket) REFERENCES tickets (id), FOREIGN KEY (player) REFERENCES players (uuid))");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            getLogger().info("Error while setting up the SQL Connection! Please check you SQL data!");
        }
    }

    public Connection getSQLConnection() {
        try {
            if (sql_connection == null || sql_connection.isClosed()) {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
                sql_connection = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sql_connection;
    }

    public PermissionHandler getPermissionHandler() {
        return permissionHandler;
    }

    public PositionHandler getPositionHandler() {
        return positionHandler;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public CachedPlayerHandler getCachedPlayerHandler() {
        return cachedPlayerHandler;
    }

    public TicketHandler getTicketHandler() {
        return ticketHandler;
    }

    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public int getTicketsPerPage() {
        return ticketsPerPage;
    }
}
