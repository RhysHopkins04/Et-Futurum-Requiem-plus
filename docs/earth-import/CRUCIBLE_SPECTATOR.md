# Crucible spectator command support — 2026-10-18

The supplied server is Crucible dev-231, staging-9985c735, SHA-256
0e88d7474854d395e2e0775473b2f095c28faa8501d079fd80e7a85a40477f0f.
Its actual org.bukkit.GameMode class lacks SPECTATOR and getByValue(3) returns
null. Its default Bukkit gamemode command also lacks spectator/sp parsing.
EFR already registers a real Minecraft GameType ID 3 and provides spectator
behavior when enableSpectatorMode=true. Altering the Forge command parser alone
would not repair the older Bukkit API or plugin path.

Official Crucible dev-280, staging-667a28e2b5c57f6f1fa21d8fdfd1722e174581ac,
includes the upstream fix. Its actual server jar has Bukkit GameMode.SPECTATOR(3),
numeric/name parsing and CraftPlayer.isGameModeAvailable, which checks whether
an installed Forge mod registered that real Minecraft game type.

Official release: https://github.com/CrucibleMC/Crucible/releases/tag/staging-667a28e
Jar SHA-256:
bb44fe4ef0898716a614ce42be58debc4c9d93b04b674933572a6db86e3f5fd4.
The jar and matching java9args.txt were downloaded from that release and checked
against GitHub's published asset size/digest. The actual old/new Bukkit APIs were
exercised in a Java probe: the old one has no mode 3; the new one resolves both
ID 3 and SPECTATOR while retaining modes 0–2.

Relevant upstream code:
- https://github.com/CrucibleMC/Crucible/blob/667a28e2b5c57f6f1fa21d8fdfd1722e174581ac/patches/org/bukkit/GameMode.java.patch
- https://github.com/CrucibleMC/Crucible/blob/667a28e2b5c57f6f1fa21d8fdfd1722e174581ac/patches/org/bukkit/command/defaults/GameModeCommand.java.patch
- https://github.com/CrucibleMC/Crucible/blob/667a28e2b5c57f6f1fa21d8fdfd1722e174581ac/src/main/java/org/bukkit/craftbukkit/v1_7_R4/entity/CraftPlayer.java

No optional Bukkit enum injection or command takeover was added to EFR. Update
the server using its normal stopped-server backup/deployment procedure, retain
the pack's mods/configs/plugins and enable EFR spectator on server and client.
As an operator test /gamemode spectator, /gamemode 3 and /gamemode 1. If a plugin
owns that alias, test /minecraft:gamemode spectator and /minecraft:gamemode 1.
Check no-clip, exit, relog and visibility to another player. Bukkit/plugin
permissions and a complete hybrid-server login session still require that real
server check; the isolated API probe does not establish full plugin parity.
