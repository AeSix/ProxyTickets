# ProxyTickets

I have forked this on Github and am updating it with changes to be suitable for my use.  However, as it currently stands, it appears to be working! 

These changes:
Updated to Java 10
Properly added UTF-8
Set to compile ProxyTickets for Bungeecord 1.13
Set to compile ProxyTicketsBukkit for Spigot 1.13 SNAPSHOT
Changed version from 1.0.4 to 1.1.0 (Will be incrementing as I work)
Replaced /ticket with /modreq (No change to /tickets command)
Removed the database "table prefix" nonsense. (fite me!) (please don't)

I currently have ProxyTickets running on a Minecraft 1.13 compatible Bungeecord with a 1.12.2 Spigot server with ViaVersion.
I am testing with 1.12.2 client. 

Cheatsheet:
* apt-get install maven
* apt-get install git
* mkdir git
* cd git
* git clone https://github.com/AeSix/ProxyTickets
* cd ProxyTickets
* mvn clean pacakge
* *** wait for it to build ***
* cd ../
* git clone https://github.com/AeSix/ProxyTicketsBukkit
* cd ProxyTicketsBukkit
*  mvn clean package
* *** wait for it to build ***
* cd ../

The jar files will be in git/ProxyTickets/target and git/ProxyTicketsBukkit/target 

Copy the ProxyTickets jar to your Bungeecord plugins/ folder and restart Bungee (use a test server for configuring it!)

Copy the ProxyTicketsBukkit jar to EACH of your Spigot servers' plugins folders.  You don't need to configure anything. 

Be sure to have a Vault compatible Permissions system installed and working on EACH of your Spigot servers (along with Vault) as permissions are set on a per-server bases via the servers themselves.  This is why there is a ProxyTicketsBukkit plugin - it relays each servers' permissions to ProxyTickets for Bungee in order to know the permissions each user has.
(I'll admit this is a bit wonky, especially in light of LuckPerms' server contexts where LuckPermsBungee could easily be used to specify per-server permissions without the need of a Spigot plugin. 
