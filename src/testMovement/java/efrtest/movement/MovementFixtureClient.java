package efrtest.movement;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import ganymedes01.etfuturum.elytra.IElytraPlayer;
import ganymedes01.etfuturum.movement.MovementMode;
import ganymedes01.etfuturum.movement.MovementPose;
import ganymedes01.etfuturum.movement.PlayerMovement;
import ganymedes01.etfuturum.network.profile.ClientWorldProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.io.File;
import java.util.EnumSet;

import static efrtest.movement.MovementFixture.near;
import static efrtest.movement.MovementFixture.require;

public final class MovementFixtureClient {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final EnumSet<MovementPose> seen = EnumSet.noneOf(MovementPose.class);
    private final String fixture = System.getProperty("efr.movementFixture");
    private final boolean observer = fixture.equals("observer");
    private int ticks, stage, elapsed, reconnects;
    private double edgeX;

    public static void start() {
        FMLCommonHandler.instance().bus().register(new MovementFixtureClient());
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            if (new File("FIXTURE_STOP").isFile()) { mc.shutdown(); return; }
            require(++ticks < 1200, "timeout stage " + stage + " seen=" + seen + " screen=" + mc.currentScreen);
            if (++elapsed < 10 && stage != 21 && stage != 15 && stage != 24) return;
            if (stage == 0) {
                FMLClientHandler.instance().setupServerList();
                FMLClientHandler.instance().connectToServer(new GuiMainMenu(),
                        new ServerData("Movement fixture", "127.0.0.1:" + System.getProperty("efr.movementPort")));
                advance();
                return;
            }
            EntityClientPlayerMP player = mc.thePlayer;
            if (mc.theWorld == null || player == null) return;
            if (mc.currentScreen != null) mc.displayGuiScreen(null);
            if (observer) {
                observe(player);
                return;
            }
            MovementMode expected = fixture.equals("legacy") ? MovementMode.LEGACY_SNEAK
                    : fixture.equals("disabled") ? MovementMode.DISABLED : MovementMode.MODERN;
            require(ClientWorldProfile.server().movement == expected, "wrong negotiated movement mode");
            switch (stage) {
                case 1:
                    command(player, "bridge");
                    break;
                case 2:
                    if (elapsed < 30 || !player.onGround) return;
                    if (reconnects > 0) {
                        System.out.println("MOVEMENT_FIXTURE_CLIENT PASSED " + fixture + " including reconnect");
                        mc.shutdown();
                        return;
                    }
                    stage = 21;
                    elapsed = 0;
                    break;
                case 21:
                    if (elapsed > 1 && expected == MovementMode.MODERN) {
                        pose(player, (elapsed & 1) == 0 ? MovementPose.CROUCH : MovementPose.VANILLA);
                    }
                    keys(elapsed >= 10, elapsed >= 10 || (elapsed & 1) != 0, false);
                    if (elapsed >= 10) {
                        stage = 3;
                        elapsed = 0;
                    }
                    break;
                case 3:
                    if (elapsed < 60) return;
                    require(player.onGround && player.posX > 1 && player.posX < 1.305, "sneak edge failed x=" + player.posX);
                    edgeX = player.posX;
                    keys(false, true, false);
                    advance();
                    break;
                case 4:
                    require(Math.abs(player.posX - edgeX) < 0.012D, "sneak edge drift x=" + player.posX);
                    if (expected == MovementMode.MODERN) pose(player, MovementPose.CROUCH);
                    else near(player.height, 1.8, "legacy sneak hitbox");
                    Vec3 eye = player.getPosition(1.0F);
                    player.rotationYaw = player.prevRotationYaw = 90F;
                    player.rotationPitch = player.prevRotationPitch =
                            (float) Math.toDegrees(Math.atan2(eye.yCoord - 79.65D, eye.xCoord - 1.0D));
                    MovingObjectPosition target = player.rayTrace(4.5D, 1.0F);
                    require(target != null && target.blockX == 0 && target.blockY == 79 && target.sideHit == 5,
                            "bridging pick missed edge: " + target);
                    require(mc.playerController.onPlayerRightClick(player, mc.theWorld, player.getHeldItem(),
                            target.blockX, target.blockY, target.blockZ, target.sideHit, target.hitVec), "bridge placement rejected");
                    System.out.println("MOVEMENT_FIXTURE_EDGE mode=" + expected + " x=" + edgeX + " eye=" + (eye.yCoord - player.boundingBox.minY));
                    advance();
                    break;
                case 5:
                    require(mc.theWorld.getBlock(1, 79, 0) == Blocks.cobblestone, "placed bridge block did not persist");
                    player.sendChatMessage("fixture-check-bridge");
                    command(player, "correct-bridge");
                    stage = 22;
                    break;
                case 22:
                    if (elapsed < 20) return;
                    near(player.posX, 1.19D, "server correction while sneaking");
                    require(player.onGround && player.isSneaking(), "lost footing or sneak after correction");
                    if (expected == MovementMode.MODERN) pose(player, MovementPose.CROUCH);
                    keys(false, false, false);
                    command(player, "diagonal");
                    break;
                case 23:
                    if (elapsed < 20 || !player.onGround) return;
                    player.rotationYaw = player.prevRotationYaw = -45F;
                    keys(true, true, false);
                    advance();
                    break;
                case 24:
                    player.rotationYaw = player.prevRotationYaw = -45F;
                    if (elapsed < 70) return;
                    require(player.onGround && player.posX > 1.0D && player.posZ > 1.0D
                            && player.posX < 1.305D && player.posZ < 1.305D,
                            "sneak diagonal edge failed x=" + player.posX + " z=" + player.posZ);
                    System.out.println("MOVEMENT_FIXTURE_DIAGONAL mode=" + expected + " x=" + player.posX + " z=" + player.posZ);
                    keys(false, false, false);
                    if (expected == MovementMode.DISABLED) {
                        reconnect();
                    } else {
                        command(player, "slab");
                        stage = 6;
                    }
                    break;
                case 6:
                    if (elapsed < 25) return;
                    keys(true, true, false);
                    advance();
                    break;
                case 7:
                    if (expected == MovementMode.LEGACY_SNEAK) {
                        require(player.posX < 6.71D, "legacy sneak should keep its open-ground height at an overhead slab");
                    } else {
                        if (player.posX < 7.07D && elapsed < 60) return;
                        require(player.posX > 7.0D, "modern crouch failed to enter slab clearance x=" + player.posX);
                        keys(false, false, false);
                        advance();
                        return;
                    }
                    keys(false, false, false);
                    command(player, "crawl");
                    stage = 9;
                    break;
                case 8:
                    if (player.posX >= 8.3D) throw new AssertionError("fixture overshot slab x=" + player.posX);
                    pose(player, MovementPose.CROUCH);
                    require(!player.isSneaking(), "ceiling altered sneak input");
                    player.rotationPitch = player.prevRotationPitch = -90F;
                    MovingObjectPosition underside = player.rayTrace(4.5D, 1.0F);
                    require(underside != null && underside.blockX == 7 && underside.blockY == 81 && underside.sideHit == 0,
                            "stationary slab underside pick missed: " + underside);
                    System.out.println("MOVEMENT_FIXTURE_SLAB_UNDERSIDE " + underside);
                    command(player, "crawl");
                    break;
                case 9:
                    pose(player, MovementPose.CRAWL);
                    keys(true, false, false);
                    advance();
                    break;
                case 10:
                    if ((player.posX < 10.31D || PlayerMovement.pose(player) == MovementPose.CRAWL) && elapsed < 100) return;
                    require(player.posX > 10.3D, "crawl did not exit tunnel x=" + player.posX);
                    pose(player, MovementPose.VANILLA);
                    keys(false, false, false);
                    command(player, "swim");
                    break;
                case 11:
                    if (!player.isInWater()) return;
                    keys(true, false, true);
                    advance();
                    break;
                case 12:
                    pose(player, MovementPose.SWIM);
                    require(player.posX > 12.65D, "sprint swim did not move forward");
                    keys(false, false, false);
                    advance();
                    break;
                case 13:
                    require(PlayerMovement.pose(player) != MovementPose.SWIM, "swim stuck after sprint release");
                    command(player, "waterlogged");
                    break;
                case 14:
                    pose(player, MovementPose.CRAWL);
                    require(player.isInWater() && PlayerMovement.eyesInWater(player, MovementPose.CRAWL.eye),
                            "waterlogged floor and ceiling did not supply swim water");
                    player.rotationYaw = -90F;
                    // Hold sprint forward against the next tall block in an isolated contained-water cell.
                    keys(true, false, true);
                    advance();
                    break;
                case 15:
                    if (PlayerMovement.pose(player) != MovementPose.SWIM && elapsed < 25) return;
                    pose(player, MovementPose.SWIM);
                    keys(false, false, false);
                    command(player, "elytra");
                    break;
                case 16:
                    if (player.onGround || player.motionY >= 0) return;
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                    advance();
                    break;
                case 17:
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                    if (!((IElytraPlayer) player).etfu$isElytraFlying() && elapsed < 30) return;
                    require(((IElytraPlayer) player).etfu$isElytraFlying(), "Backlytra did not take off");
                    require(PlayerMovement.pose(player) == MovementPose.VANILLA, "movement pose overrode Backlytra");
                    near(player.height, 0.6D, "Backlytra hitbox");
                    near(player.getEyeHeight(), 0.4D, "Backlytra eye");
                    advance();
                    break;
                case 18:
                    if (elapsed < 160 && (!player.onGround || ((IElytraPlayer) player).etfu$isElytraFlying())) return;
                    require(player.onGround && !((IElytraPlayer) player).etfu$isElytraFlying(), "Backlytra did not land");
                    pose(player, MovementPose.VANILLA);
                    reconnect();
                    break;
                default: throw new AssertionError("unknown fixture stage");
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.out.println("MOVEMENT_FIXTURE_CLIENT FAILED " + fixture);
            mc.shutdown();
        }
    }

    private void observe(EntityClientPlayerMP player) {
        if (stage == 1) {
            command(player, "observe");
            return;
        }
        for (Object candidate : mc.theWorld.playerEntities) {
            EntityPlayer other = (EntityPlayer) candidate;
            if (other != player) {
                MovementPose pose = PlayerMovement.pose(other);
                if (Math.abs(other.height - pose.height) < 0.003) seen.add(pose);
            }
        }
        if (seen.containsAll(EnumSet.allOf(MovementPose.class))) {
            System.out.println("MOVEMENT_FIXTURE_REMOTE PASSED " + seen);
            mc.shutdown();
        }
    }

    private void reconnect() {
        keys(false, false, false);
        mc.theWorld.sendQuittingDisconnectingPacket();
        mc.loadWorld(null);
        require(ClientWorldProfile.server().movement == MovementMode.DISABLED, "pose mode leaked after disconnect");
        mc.displayGuiScreen(new GuiMainMenu());
        reconnects++;
        stage = 0;
        elapsed = 0;
    }

    private void command(EntityClientPlayerMP player, String phase) {
        player.sendChatMessage("fixture-" + phase);
        advance();
    }

    private void advance() {
        stage++;
        elapsed = 0;
    }

    private void pose(EntityClientPlayerMP player, MovementPose expected) {
        require(PlayerMovement.pose(player) == expected, "stage " + stage + " expected " + expected + " got " + PlayerMovement.pose(player));
        near(player.height, expected.height, "client hitbox");
        if (expected != MovementPose.VANILLA) {
            near(player.posY + player.getEyeHeight() - player.boundingBox.minY, expected.eye, "logical eye");
            near(player.getPosition(1.0F).yCoord - player.boundingBox.minY, expected.eye, "pick origin");
        }
    }

    private void keys(boolean forward, boolean sneak, boolean sprint) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), forward);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), sneak);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), sprint);
    }
}
