package ganymedes01.etfuturum.command;

import ganymedes01.etfuturum.world.imported.ImportCapabilities;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import java.io.IOException;

/** One non-destructive command for the user's eventual exact-modpack smoke test. */
public final class CommandImportReport extends CommandBase {
    @Override public String getCommandName() { return "efrimportreport"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/efrimportreport"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 0) throw new CommandException(getCommandUsage(sender));
        try {
            sender.addChatMessage(new ChatComponentText("EFR runtime import report: " + ImportCapabilities.export(
                    MinecraftServer.getServer().worldServerForDimension(0)).getAbsolutePath()));
        } catch (IOException e) { throw new CommandException("Could not export EFR import report: " + e.getMessage()); }
    }
}
