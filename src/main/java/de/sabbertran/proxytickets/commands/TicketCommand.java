package de.sabbertran.proxytickets.commands;

import de.sabbertran.proxytickets.ProxyTickets;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TicketCommand extends Command {
    private ProxyTickets main;

    public TicketCommand(ProxyTickets main) {
        super("ticket");
        this.main = main;
    }

    public void execute(CommandSender sender, String[] args) {
        if (main.getPermissionHandler().hasPermission(sender, "proxytickets.ticket")) {
            if (sender instanceof ProxiedPlayer) {
                final ProxiedPlayer p = (ProxiedPlayer) sender;
                if (args.length > 0) {
                    StringBuilder messageBuilder = new StringBuilder();
                    for (String s : args)
                        messageBuilder.append(" " + s);
                    final String message = messageBuilder.toString().substring(1);
                    main.getPositionHandler().requestPosition(p);
                    main.getPositionHandler().addPositionRunnable(p, new Runnable() {
                        public void run() {
                            main.getTicketHandler().openTicket(p, main.getPositionHandler().getLocalPositions().remove(p.getUniqueId()), message);
                        }
                    });
                } else {
                    main.getCommandHandler().sendUsage(sender, this);
                }
            } else {
                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.noplayer"));
            }
        } else {
            main.getPermissionHandler().sendMissingPermissionInfo(sender);
        }
    }
}
