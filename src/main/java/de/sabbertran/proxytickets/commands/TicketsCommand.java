package de.sabbertran.proxytickets.commands;

import de.sabbertran.proxytickets.ProxyTickets;
import de.sabbertran.proxytickets.objects.CachedPlayer;
import de.sabbertran.proxytickets.objects.Comment;
import de.sabbertran.proxytickets.objects.Ticket;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TicketsCommand extends Command {
    private ProxyTickets main;

    public TicketsCommand(ProxyTickets main) {
        super("tickets");
        this.main = main;
    }

    public void execute(final CommandSender sender, String[] args) {

        if (args.length == 0) {
            if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.list"))
                main.getTicketHandler().sendOpenTicketList(sender, 0, main.getTicketsPerPage());
            else
                main.getPermissionHandler().sendMissingPermissionInfo(sender);
        } else if (args.length == 1) {
            if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.info")) {
                try {
                    final int id = Integer.parseInt(args[0]);
                    if (main.getTicketHandler().isLoadedTicket(id)) {
                        main.getTicketHandler().sendTicket(sender, main.getTicketHandler().getTicket(id));
                    } else {
                        main.getProxy().getScheduler().runAsync(main, new Runnable() {
                            public void run() {
                                Ticket t = main.getTicketHandler().getTicket(id);
                                if (t != null)
                                    main.getTicketHandler().sendTicket(sender, t);
                                else
                                    main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                            }
                        });
                    }
                } catch (NumberFormatException ex) {
                    main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[0]));
                }
            } else
                main.getPermissionHandler().sendMissingPermissionInfo(sender);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("page")) {
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.list")) {
                    try {
                        int page = Integer.parseInt(args[1]);
                        main.getTicketHandler().sendOpenTicketList(sender, (page - 1) * main.getTicketsPerPage(), main.getTicketsPerPage());
                    } catch (NumberFormatException ex) {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else if (args[0].equalsIgnoreCase("claim")) {
                //TODO not at status 2
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.claim")) {
                    if (sender instanceof ProxiedPlayer) {
                        final CachedPlayer p = main.getCachedPlayerHandler().getCachedPlayer(sender);
                        try {
                            final int id = Integer.parseInt(args[1]);
                            if (main.getTicketHandler().isLoadedTicket(id)) {
                                Ticket t = main.getTicketHandler().getTicket(id);
                                if (t.getStatus() == 0 || main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.claim.others")) {
                                    main.getTicketHandler().claimTicket(t, p);
                                    main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.claim.success"), t));
                                    ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                    if (creator != null)
                                        main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.claim.userinfo"), t).replace("%player%", sender.getName()));
                                    main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.claim.teaminfo"), t).replace("%player%", sender.getName()), "proxytickets.tickets.claim.receiveteaminfo");
                                } else {
                                    main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.claim.onlyopen"));
                                }
                            } else {
                                main.getProxy().getScheduler().runAsync(main, new Runnable() {
                                    public void run() {
                                        Ticket t = main.getTicketHandler().getTicket(id);
                                        if (t != null) {
                                            if (t.getStatus() == 0 || main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.claim.others")) {
                                                main.getTicketHandler().claimTicket(t, p);
                                                main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.claim.success"), t));
                                                ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                                if (creator != null)
                                                    main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.claim.userinfo"), t).replace("%player%", sender.getName()));

                                                main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.claim.teaminfo"), t).replace("%player%", sender.getName()), "proxytickets.tickets.claim.receiveteaminfo");
                                            } else {
                                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.claim.onlyopen"));
                                            }
                                        } else {
                                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                                        }
                                    }
                                });
                            }
                        } catch (NumberFormatException ex) {
                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                        }
                    } else {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.noplayer"));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else if (args[0].equalsIgnoreCase("unclaim")) {
                //TODO only at status 1
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.unclaim")) {
                    if (sender instanceof ProxiedPlayer) {
                        final CachedPlayer p = main.getCachedPlayerHandler().getCachedPlayer(sender);
                        try {
                            final int id = Integer.parseInt(args[1]);
                            if (main.getTicketHandler().isLoadedTicket(id)) {
                                Ticket t = main.getTicketHandler().getTicket(id);
                                if (t.getClaimedBy().equals(p) || main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.unclaim.others")) {
                                    if (t.getStatus() == 1) {
                                        main.getTicketHandler().unclaimTicket(t);
                                        main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.unclaim.success"), t));
                                        ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                        if (creator != null)
                                            main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.unclaim.userinfo"), t).replace("%player%", sender.getName()));
                                        main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.unclaim.teaminfo"), t).replace("%player%", sender.getName()), "proxytickets.tickets.unclaim.receiveteaminfo");
                                    } else {
                                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.unclaim.onlyclaimed"));
                                    }
                                } else {
                                    main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.unclaim.onlyown"));
                                }
                            } else {
                                main.getProxy().getScheduler().runAsync(main, new Runnable() {
                                    public void run() {
                                        Ticket t = main.getTicketHandler().getTicket(id);
                                        if (t != null) {
                                            if (t.getClaimedBy().equals(p) || main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.unclaim.others")) {
                                                if (t.getStatus() == 1) {
                                                    main.getTicketHandler().unclaimTicket(t);
                                                    main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.unclaim.success"), t));
                                                    ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                                    if (creator != null)
                                                        main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.unclaim.userinfo"), t).replace("%player%", sender.getName()));
                                                    main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.unclaim.teaminfo"), t).replace("%player%", sender.getName()), "proxytickets.tickets.unclaim.receiveteaminfo");
                                                } else {
                                                    main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.unclaim.onlyclaimed"));
                                                }
                                            } else {
                                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.unclaim.onlyown"));
                                            }
                                        } else {
                                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                                        }
                                    }
                                });
                            }
                        } catch (NumberFormatException ex) {
                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                        }
                    } else {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.noplayer"));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else if (args[0].equalsIgnoreCase("close")) {
                //TODO not at status 2
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close")) {
                    try {
                        final int id = Integer.parseInt(args[1]);
                        final CachedPlayer player = main.getCachedPlayerHandler().getCachedPlayer(sender);
                        if (main.getTicketHandler().isLoadedTicket(id)) {
                            final Ticket t = main.getTicketHandler().getTicket(id);
                            if (player.equals(t.getClaimedBy()) || (t.getClaimedBy() != null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.others")) || (t.getClaimedBy() == null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.unclaimed"))) {
                                main.getTicketHandler().closeTicket(t, main.getCachedPlayerHandler().getCachedPlayer(sender), "");
                                main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.success.withoutanswer"), t));
                                ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                if (creator != null)
                                    main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.userinfo.withoutanswer"), t).replace("%player%", sender.getName()));
                                main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.teaminfo.withoutanswer"), t).replace("%player%", sender.getName()), "proxytickets.tickets.close.receiveteaminfo");
                            } else {
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.close.onlyown"));
                            }
                        } else {
                            main.getProxy().getScheduler().runAsync(main, new Runnable() {
                                public void run() {
                                    Ticket t = main.getTicketHandler().getTicket(id);
                                    if (t != null) {
                                        if (t.getClaimedBy().equals(player) || (t.getClaimedBy() != null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.others")) || (t.getClaimedBy() == null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.unclaimed"))) {
                                            main.getTicketHandler().closeTicket(t, main.getCachedPlayerHandler().getCachedPlayer(sender), "");
                                            main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.success.withoutanswer"), t));
                                            ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                            if (creator != null)
                                                main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.userinfo.withoutanswer"), t).replace("%player%", sender.getName()));
                                            main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.teaminfo.withoutanswer"), t).replace("%player%", sender.getName()), "proxytickets.tickets.close.receiveteaminfo");
                                        } else {
                                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.close.onlyown"));
                                        }
                                    } else {
                                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                                    }
                                }
                            });
                        }
                    } catch (NumberFormatException ex) {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else if (args[0].equalsIgnoreCase("tp")) {
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.tp")) {
                    if (sender instanceof ProxiedPlayer) {
                        final ProxiedPlayer p = (ProxiedPlayer) sender;
                        try {
                            final int id = Integer.parseInt(args[1]);
                            if (main.getTicketHandler().isLoadedTicket(id)) {
                                main.getTicketHandler().teleportToTicket(p, main.getTicketHandler().getTicket(id));
                            } else {
                                main.getProxy().getScheduler().runAsync(main, new Runnable() {
                                    public void run() {
                                        Ticket t = main.getTicketHandler().getTicket(id);
                                        if (t != null)
                                            main.getTicketHandler().teleportToTicket(p, t);
                                        else
                                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                                    }
                                });
                            }
                        } catch (NumberFormatException ex) {
                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                        }
                    } else {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.noplayer"));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else if (args[0].equalsIgnoreCase("comments")) {
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.comments")) {
                    try {
                        final int id = Integer.parseInt(args[1]);
                        if (main.getTicketHandler().isLoadedTicket(id)) {
                            main.getTicketHandler().sendTicketComments(sender, main.getTicketHandler().getTicket(id));
                        } else {
                            main.getProxy().getScheduler().runAsync(main, new Runnable() {
                                public void run() {
                                    Ticket t = main.getTicketHandler().getTicket(id);
                                    if (t != null)
                                        main.getTicketHandler().sendTicketComments(sender, t);
                                    else {
                                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                                    }
                                }
                            });
                        }
                    } catch (NumberFormatException ex) {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else if (args[0].equalsIgnoreCase("purge")) {
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.purge")) {
                    if (sender instanceof ProxiedPlayer) {
                        ProxiedPlayer p = (ProxiedPlayer) sender;
                        if (args[1].equalsIgnoreCase("all")) {
                            if (main.getTicketHandler().createPurgeRequest(p, true))
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.purge.requestsuccess.all"));
                            else
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.purge.alreadyrequested"));
                        } else if (args[1].equalsIgnoreCase("closed")) {
                            if (main.getTicketHandler().createPurgeRequest(p, false))
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.purge.requestsuccess.closed"));
                            else
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.purge.alreadyrequested"));
                        } else if (args[1].equalsIgnoreCase("confirm")) {
                            if (main.getTicketHandler().purgeDatabase(p))
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.purge.success"));
                            else
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.purge.norequest"));
                        } else {
                            main.getCommandHandler().sendUsage(sender, this);
                        }
                    } else {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.noplayer"));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else {
                main.getCommandHandler().sendUsage(sender, this);
            }
        } else if (args.length >= 3) {
            if (args[0].equalsIgnoreCase("close")) {
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close")) {
                    try {
                        final int id = Integer.parseInt(args[1]);
                        StringBuilder answerBuilder = new StringBuilder();
                        for (int i = 2; i < args.length; i++)
                            answerBuilder.append(args[i] + " ");
                        final String answer = answerBuilder.toString().trim();
                        final CachedPlayer player = main.getCachedPlayerHandler().getCachedPlayer(sender);
                        if (main.getTicketHandler().isLoadedTicket(id)) {
                            Ticket t = main.getTicketHandler().getTicket(id);
                            if (player.equals(t.getClaimedBy()) || (t.getClaimedBy() != null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.others")) || (t.getClaimedBy() == null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.unclaimed"))) {
                                main.getTicketHandler().closeTicket(t, main.getCachedPlayerHandler().getCachedPlayer(sender), answer);
                                main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.success"), t));
                                ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                if (creator != null)
                                    main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.userinfo"), t).replace("%player%", sender.getName()));

                                main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.teaminfo"), t).replace("%player%", sender.getName()), "proxytickets.tickets.close.receiveteaminfo");
                            } else {
                                main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.close.onlyown"));
                            }
                        } else {
                            main.getProxy().getScheduler().runAsync(main, new Runnable() {
                                public void run() {
                                    Ticket t = main.getTicketHandler().getTicket(id);
                                    if (t != null) {
                                        if (t.getClaimedBy().equals(player) || (t.getClaimedBy() != null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.others")) || (t.getClaimedBy() == null && main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.close.unclaimed"))) {
                                            main.getTicketHandler().closeTicket(t, main.getCachedPlayerHandler().getCachedPlayer(sender), answer);
                                            main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.success"), t));
                                            ProxiedPlayer creator = t.getPlayer().getBungeePlayer();
                                            if (creator != null)
                                                main.getMessageHandler().sendMessage(creator, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.userinfo"), t).replace("%player%", sender.getName()));
                                            main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.close.teaminfo"), t).replace("%player%", sender.getName()), "proxytickets.tickets.close.receiveteaminfo");
                                        } else {
                                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.close.onlyown"));
                                        }
                                    } else {
                                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                                    }
                                }
                            });
                        }
                    } catch (NumberFormatException ex) {
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                    }
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else if (args[0].equalsIgnoreCase("comment")) {
                if (main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.comment")) {
                    if (sender instanceof ProxiedPlayer) {
                        final ProxiedPlayer p = (ProxiedPlayer) sender;
                        try {
                            final int id = Integer.parseInt(args[1]);
                            StringBuilder commentBuilder = new StringBuilder();
                            for (int i = 2; i < args.length; i++)
                                commentBuilder.append(args[i] + " ");
                            final String comment = commentBuilder.toString().trim();
                            final CachedPlayer player = main.getCachedPlayerHandler().getCachedPlayer(sender);
                            if (main.getTicketHandler().isLoadedTicket(id)) {
                                final Ticket t = main.getTicketHandler().getTicket(id);
                                if (t.getPlayer().equals(player) || main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.comment.others")) {
                                    main.getPositionHandler().requestPosition(p);
                                    main.getPositionHandler().addPositionRunnable(p, new Runnable() {
                                        public void run() {
                                            Comment c = main.getTicketHandler().commentTicket(t, player, comment, main.getPositionHandler().getLocalPositions().remove(p.getUniqueId()));
                                            main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.success"), t, c));
                                            if (!player.equals(t.getPlayer()))
                                                main.getMessageHandler().sendMessage(t.getPlayer().getBungeePlayer(), main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.success"), t, c));
                                            main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.info"), t), "proxytickets.tickets.comment.receiveteaminfo", p);
                                        }
                                    });
                                } else {
                                    main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.onlyown"), t));
                                }
                            } else {
                                main.getProxy().getScheduler().runAsync(main, new Runnable() {
                                    public void run() {
                                        final Ticket t = main.getTicketHandler().getTicket(id);
                                        if (t != null) {
                                            if (t.getPlayer().equals(player) || main.getPermissionHandler().hasPermission(sender, "proxytickets.tickets.comment.others")) {
                                                main.getPositionHandler().requestPosition(p);
                                                main.getPositionHandler().addPositionRunnable(p, new Runnable() {
                                                    public void run() {
                                                        Comment c = main.getTicketHandler().commentTicket(t, player, comment, main.getPositionHandler().getLocalPositions().remove(p.getUniqueId()));
                                                        main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.success"), t, c));
                                                        if (!player.equals(t.getPlayer()))
                                                            main.getMessageHandler().sendMessage(t.getPlayer().getBungeePlayer(), main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.success"), t, c));
                                                        main.getMessageHandler().sendMessageWithPermission(main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.info"), t), "proxytickets.tickets.comment.receiveteaminfo", p);
                                                    }
                                                });
                                            } else {
                                                main.getMessageHandler().sendMessage(sender, main.getTicketHandler().translateTicketVariables(main.getMessageHandler().getMessage("ticket.comment.onlyown"), t));
                                            }
                                        } else {
                                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("ticket.notexists").replace("%id%", String.valueOf(id)));
                                        }
                                    }
                                });
                            }
                        } catch (NumberFormatException ex) {
                            main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.invalidnumber").replace("%number%", args[1]));
                        }
                    } else
                        main.getMessageHandler().sendMessage(sender, main.getMessageHandler().getMessage("command.noplayer"));
                } else
                    main.getPermissionHandler().sendMissingPermissionInfo(sender);
            } else {
                main.getCommandHandler().sendUsage(sender, this);
            }
        } else {
            main.getCommandHandler().sendUsage(sender, this);
        }
    }
}
