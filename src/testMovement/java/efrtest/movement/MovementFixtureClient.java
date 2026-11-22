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
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

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
    private MovementPose loggedPose;
    private boolean sawCrawlRender;
    private boolean captureCrawl;
    private boolean checkedCrawlLight;
    private boolean wrongCrawlLight;
    private boolean swamAfterSprintRelease;
    private int wallTicks, wallPoseDrops;
    private int floorSwimTicks, submergedFrames, surfaceFrames, badFogFrames, badOverlayFrames;
    private int surfaceSwimTicks;
    private boolean stoodAfterWaterExit;
    private float floorBob;
    private double cameraEyeError;
    private boolean sampledFog, expectedWet, fogWet, overlayWet;

    public static void start() {
        MovementFixtureClient fixture = new MovementFixtureClient();
        FMLCommonHandler.instance().bus().register(fixture);
        MinecraftForge.EVENT_BUS.register(fixture);
        if (!fixture.observer) {
            MinecraftForgeClient.registerItemRenderer(Items.stick, new IItemRenderer() {
                public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
                    return type == ItemRenderType.EQUIPPED_FIRST_PERSON;
                }

                public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
                    return false;
                }

                public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
                    fixture.checkCrawlLight();
                }
            });
        }
    }

    private void checkCrawlLight() {
        EntityClientPlayerMP player = mc.thePlayer;
        if (stage != 10 || player.posX < 10.1D || player.posX >= 10.9D
                || PlayerMovement.pose(player) != MovementPose.CRAWL || checkedCrawlLight) return;
        int x = MathHelper.floor_double(player.posX);
        int z = MathHelper.floor_double(player.posZ);
        int y = MathHelper.floor_double(player.posY + player.getEyeHeight());
        int expected = mc.theWorld.getLightBrightnessForSkyBlocks(x, y, z, 0);
        int ceiling = mc.theWorld.getLightBrightnessForSkyBlocks(x, y + 1, z, 0);
        int actual = (int) OpenGlHelper.lastBrightnessY << 16 | (int) OpenGlHelper.lastBrightnessX;
        checkedCrawlLight = true;
        wrongCrawlLight = actual != expected || expected == ceiling;
        System.out.println("MOVEMENT_FIXTURE_CRAWL_LIGHT actual=" + actual + " eye=" + expected + " ceiling=" + ceiling);
    }

    @SubscribeEvent
    public void observeCrawlRender(RenderPlayerEvent.Specials.Pre event) {
        if (!observer || sawCrawlRender || event.entityPlayer == mc.thePlayer
                || PlayerMovement.pose(event.entityPlayer) != MovementPose.CRAWL) return;
        ModelBiped model = event.renderer.modelBipedMain;
        if (model.bipedLeftArm.rotateAngleX < -2.8F && model.bipedRightArm.rotateAngleX < -2.8F) {
            sawCrawlRender = true;
            captureCrawl = true;
            System.out.println("MOVEMENT_FIXTURE_CRAWL_ARMS left=" + model.bipedLeftArm.rotateAngleX
                    + " right=" + model.bipedRightArm.rotateAngleX);
        }
    }

    @SubscribeEvent
    public void captureCrawl(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            sampledFog = false;
            overlayWet = false;
        } else if (sampledFog) {
            if (expectedWet) submergedFrames++;
            else surfaceFrames++;
            if (fogWet != expectedWet) badFogFrames++;
            if (overlayWet != expectedWet) badOverlayFrames++;
        }
        if (event.phase == TickEvent.Phase.END && captureCrawl) {
            captureCrawl = false;
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, "movement-crawl.png", mc.displayWidth, mc.displayHeight,
                    mc.getFramebuffer());
        }
    }

    @SubscribeEvent
    public void swimFog(EntityViewRenderEvent.FogDensity event) {
        EntityClientPlayerMP player = mc.thePlayer;
        if ((stage != 33 && stage != 34) || event.entity != player
                || PlayerMovement.pose(player) != MovementPose.SWIM) return;
        double eye = player.prevPosY + (player.posY - player.prevPosY) * event.renderPartialTicks + player.getEyeHeight();
        double surface = MovementFixture.Y + 2 + 8.0D / 9.0D;
        if (Math.abs(eye - surface) < 0.05D) return;
        Vec3 projected = ActiveRenderInfo.projectViewFromEntity(player, event.renderPartialTicks);
        cameraEyeError = Math.max(cameraEyeError, Math.abs(projected.yCoord - eye));
        sampledFog = true;
        expectedWet = eye < surface;
        fogWet = event.block.getMaterial() == Material.water;
    }

    @SubscribeEvent
    public void swimOverlay(RenderBlockOverlayEvent event) {
        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.WATER) overlayWet = true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            if (new File("FIXTURE_STOP").isFile()) { mc.shutdown(); return; }
            require(++ticks < 1200, "timeout stage " + stage + " seen=" + seen + " screen=" + mc.currentScreen);
            if (++elapsed < 10 && stage != 21 && stage != 15 && stage != 24 && stage != 29
                    && stage != 13 && stage != 33 && stage != 34) return;
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
            MovementMode negotiated = ClientWorldProfile.server().movement;
            require(negotiated == expected, "negotiated " + negotiated + ", expected " + expected);
            MovementPose predicted = PlayerMovement.pose(player);
            if (predicted != loggedPose) {
                loggedPose = predicted;
                System.out.println("MOVEMENT_FIXTURE_CLIENT_POSE mode=" + negotiated + " pose=" + predicted
                        + " x=" + player.posX + " height=" + (player.boundingBox.maxY - player.boundingBox.minY)
                        + " eye=" + (player.posY + player.getEyeHeight() - player.boundingBox.minY)
                        + " pick=" + (player.getPosition(1.0F).yCoord - player.boundingBox.minY));
            }
            switch (stage) {
                case 1:
                    command(player, "bridge");
                    break;
                case 2:
                    if (elapsed < 30 || !player.onGround) return;
                    if (reconnects > 0) {
                        if (expected != MovementMode.DISABLED) {
                            require(checkedCrawlLight && !wrongCrawlLight && !swamAfterSprintRelease && wallPoseDrops == 0,
                                    "crawl light checked=" + checkedCrawlLight + " wrong=" + wrongCrawlLight
                                            + ", swims after release=" + swamAfterSprintRelease + ", wall pose drops=" + wallPoseDrops);
                            require(floorSwimTicks >= 10 && submergedFrames > 0 && surfaceFrames > 0
                                            && surfaceSwimTicks >= 2 && stoodAfterWaterExit
                                            && badFogFrames == 0 && badOverlayFrames == 0
                                            && cameraEyeError < 0.05D && floorBob < 0.005F,
                                    "swim camera regression, see MOVEMENT_FIXTURE_SWIM_CAMERA");
                        }
                        System.out.println("MOVEMENT_FIXTURE_CLIENT PASSED " + fixture + " including reconnect");
                        mc.shutdown();
                        return;
                    }
                    stage = 21;
                    elapsed = 0;
                    break;
                case 21:
                    player.rotationYaw = player.prevRotationYaw = -90F;
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
                    player.rotationYaw = player.prevRotationYaw = -90F;
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
                    command(player, "slab");
                    stage = 6;
                    break;
                case 6:
                    if (elapsed < 25) return;
                    keys(true, true, false);
                    advance();
                    break;
                case 7:
                    if (expected != MovementMode.MODERN) {
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
                    keys(true, false, false);
                    stage = 25;
                    elapsed = 0;
                    break;
                case 9:
                    pose(player, MovementPose.VANILLA);
                    require(mc.theWorld.getBlock(9, 81, 0) == Blocks.trapdoor
                            && (mc.theWorld.getBlockMetadata(9, 81, 0) & 4) != 0, "trapdoor did not start open");
                    MovingObjectPosition trapdoor = player.rayTrace(4.5D, 1.0F);
                    require(trapdoor != null && trapdoor.blockX == 9 && trapdoor.blockY == 81 && trapdoor.blockZ == 0,
                            "standing pick missed open trapdoor: " + trapdoor);
                    require(mc.playerController.onPlayerRightClick(player, mc.theWorld, player.getHeldItem(),
                            trapdoor.blockX, trapdoor.blockY, trapdoor.blockZ, trapdoor.sideHit, trapdoor.hitVec),
                            "trapdoor right click rejected");
                    stage = 26;
                    elapsed = 0;
                    break;
                case 25:
                    player.rotationPitch = player.prevRotationPitch = 0F;
                    if ((player.posX < 8.31D || PlayerMovement.pose(player) != MovementPose.VANILLA)
                            && elapsed < 60) return;
                    require(player.posX >= 8.31D, "failed to walk out from under slab");
                    pose(player, MovementPose.VANILLA);
                    keys(false, false, false);
                    command(player, "crawl");
                    stage = 9;
                    break;
                case 26:
                    require((mc.theWorld.getBlockMetadata(9, 81, 0) & 4) == 0, "trapdoor did not close on client");
                    pose(player, expected == MovementMode.DISABLED ? MovementPose.VANILLA : MovementPose.CRAWL);
                    player.sendChatMessage("fixture-check-trapdoor");
                    if (expected == MovementMode.DISABLED) {
                        command(player, "swim");
                        stage = 11;
                        break;
                    }
                    // Separate the samples even when the freshly built fixture has not relit its chunks yet.
                    mc.theWorld.setLightValue(EnumSkyBlock.Block, 10, 80, 0, 14);
                    mc.theWorld.setLightValue(EnumSkyBlock.Block, 10, 81, 0, 0);
                    player.rotationYaw = player.prevRotationYaw = -90F;
                    keys(true, false, false);
                    stage = 10;
                    elapsed = 0;
                    break;
                case 10:
                    if ((player.posX < 11.31D || PlayerMovement.pose(player) == MovementPose.CRAWL) && elapsed < 100) return;
                    require(player.posX > 11.3D, "crawl did not exit tunnel x=" + player.posX);
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
                    if (expected == MovementMode.DISABLED) {
                        pose(player, MovementPose.VANILLA);
                        require(player.isInWater(), "disabled swim check left the water");
                        reconnect();
                        break;
                    }
                    pose(player, MovementPose.SWIM);
                    require(player.posX > 12.65D, "sprint swim did not move forward");
                    keys(true, false, false);
                    advance();
                    break;
                case 13:
                    if (elapsed < 3) return;
                    require(player.isInWater(), "sprint release sample left the water");
                    swamAfterSprintRelease = PlayerMovement.pose(player) == MovementPose.SWIM;
                    System.out.println("MOVEMENT_FIXTURE_SWIM_KEY_RELEASE pose=" + PlayerMovement.pose(player));
                    keys(false, false, false);
                    command(player, "swim-wall");
                    stage = 28;
                    break;
                case 28:
                    require(player.isInWater(), "wall sample did not start underwater");
                    keys(true, false, true);
                    advance();
                    break;
                case 29:
                    if (player.isCollidedHorizontally && player.isInWater()) {
                        wallTicks++;
                        if (PlayerMovement.pose(player) != MovementPose.SWIM) wallPoseDrops++;
                    }
                    if (elapsed == 30 && wallPoseDrops == 0) player.sendChatMessage("fixture-check-swim-wall");
                    if (elapsed < 35) return;
                    require(wallTicks >= 10, "did not reach underwater wall");
                    System.out.println("MOVEMENT_FIXTURE_SWIM_WALL ticks=" + wallTicks + " poseDrops=" + wallPoseDrops);
                    keys(false, false, false);
                    command(player, "wade");
                    stage = 30;
                    break;
                case 30:
                    require(player.isInWater() && !PlayerMovement.eyesInWater(player, MovementPose.VANILLA.eye),
                            "wade sample did not start in shallow water");
                    keys(true, false, true);
                    advance();
                    break;
                case 31:
                    pose(player, MovementPose.VANILLA);
                    require(player.isInWater() && player.posX > 29.6D, "player did not wade forward");
                    System.out.println("MOVEMENT_FIXTURE_WADE pose=VANILLA");
                    keys(false, false, false);
                    command(player, "swim-camera");
                    break;
                case 32:
                    require(player.isInWater() && player.posX > 36.0D, "camera sample did not start in deep water");
                    keys(true, false, true);
                    advance();
                    break;
                case 33:
                    if (elapsed < 12) return;
                    pose(player, MovementPose.SWIM);
                    if (player.onGround) {
                        floorSwimTicks++;
                        floorBob = Math.max(floorBob, Math.abs(player.cameraYaw) + Math.abs(player.cameraPitch));
                    }
                    if (elapsed < 34) return;
                    require(player.posX > 38.0D, "player did not swim along the floor");
                    player.rotationPitch = player.prevRotationPitch = -40F;
                    advance();
                    break;
                case 34:
                    if (player.isInWater() && PlayerMovement.pose(player) == MovementPose.SWIM
                            && !PlayerMovement.eyesInWater(player, MovementPose.SWIM.eye)) surfaceSwimTicks++;
                    stoodAfterWaterExit = !player.isInWater() && PlayerMovement.pose(player) == MovementPose.VANILLA;
                    if (!stoodAfterWaterExit && elapsed < 45) return;
                    System.out.println("MOVEMENT_FIXTURE_SWIM_CAMERA floorTicks=" + floorSwimTicks + " bob=" + floorBob
                            + " submergedFrames=" + submergedFrames + " surfaceFrames=" + surfaceFrames
                            + " surfaceSwimTicks=" + surfaceSwimTicks + " stoodAfterExit=" + stoodAfterWaterExit
                            + " fogMismatch=" + badFogFrames + " overlayMismatch=" + badOverlayFrames
                            + " eyeError=" + cameraEyeError);
                    keys(false, false, false);
                    command(player, "waterlogged");
                    stage = 14;
                    break;
                case 14:
                    pose(player, MovementPose.CRAWL);
                    require(player.isInWater() && PlayerMovement.eyesInWater(player, MovementPose.CRAWL.eye),
                            "waterlogged floor and ceiling did not supply swim water");
                    player.rotationYaw = -90F;
                    keys(true, false, true);
                    advance();
                    break;
                case 15:
                    if (elapsed < 12 || PlayerMovement.pose(player) != MovementPose.SWIM && elapsed < 25) return;
                    pose(player, MovementPose.SWIM);
                    keys(false, false, false);
                    stage = 27;
                    elapsed = 0;
                    break;
                case 27:
                    pose(player, MovementPose.CRAWL);
                    require(player.isInWater(), "sprint release check left the waterlogged passage");
                    command(player, "elytra");
                    stage = 16;
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
                double height = other.boundingBox.maxY - other.boundingBox.minY;
                if (Math.abs(height - pose.height) < 0.003 && seen.add(pose)) {
                    System.out.println("MOVEMENT_FIXTURE_REMOTE_POSE mode=" + ClientWorldProfile.server().movement
                            + " pose=" + pose + " x=" + other.posX + " height=" + height);
                }
            }
        }
        if (sawCrawlRender && seen.containsAll(EnumSet.allOf(MovementPose.class))) {
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
