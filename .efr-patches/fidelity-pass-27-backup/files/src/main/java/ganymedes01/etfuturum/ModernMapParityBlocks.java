package ganymedes01.etfuturum;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.network.WoodSignOpenMessage;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Static visual compatibility shells for modern vanilla block families that are not otherwise
 * represented by Et Futurum Requiem. These blocks exist so imported modern maps can preserve
 * their block identity and appearance before the corresponding gameplay/update family is fully
 * backported. They intentionally do not claim broad modern mechanics, block entities, redstone,
 * loot, growth, waterlogging, or world generation; narrowly scoped placement/state interactions
 * may be implemented where they are required to expose the visual variants faithfully.
 *
 * <p>Existing 1.7/Et Futurum blocks remain authoritative. Registration skips an entry whenever
 * the same exact registry path is already supplied by vanilla or Et Futurum, so later real
 * implementations can replace a shell simply by registering before this compatibility layer.</p>
 */
public enum ModernMapParityBlocks {
    REPEATING_COMMAND_BLOCK("1.9", Style.CUBE, Material.rock, "repeating_command_block", "textures/block/repeating_command_block.png", 0),
    CHAIN_COMMAND_BLOCK("1.9", Style.CUBE, Material.rock, "chain_command_block", "textures/block/chain_command_block.png", 0),
    STRUCTURE_BLOCK("1.10", Style.CUBE, Material.rock, "structure_block", "textures/block/structure_block.png", 0),
    STRUCTURE_VOID("1.10", Style.CUBE, Material.rock, "structure_void", "textures/block/structure_void.png", 0),
    DRIED_KELP_BLOCK("1.13", Style.CUBE, Material.plants, "dried_kelp_block", "textures/block/dried_kelp_block.png", 0),
    KELP("1.13", Style.PLANT, Material.plants, "kelp", "textures/block/kelp.png", 0),
    KELP_PLANT("1.13", Style.PLANT, Material.plants, "kelp_plant", "textures/block/kelp_plant.png", 0),
    SEAGRASS("1.13", Style.PLANT, Material.plants, "seagrass", "textures/block/seagrass.png", 0),
    TALL_SEAGRASS("1.13", Style.PLANT, Material.plants, "seagrass", "textures/block/seagrass.png", 0),
    SEA_PICKLE("1.13", Style.LAYER, Material.plants, "sea_pickle", "textures/block/sea_pickle.png", 6),
    CONDUIT("1.13", Style.SMALL, Material.rock, "conduit_base", "textures/entity/conduit/base.png", 15),
    TURTLE_EGG("1.13", Style.SMALL, Material.rock, "turtle_egg", "textures/block/turtle_egg.png", 0),
    TUBE_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "tube_coral_block", "textures/block/tube_coral_block.png", 0),
    DEAD_TUBE_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "dead_tube_coral_block", "textures/block/dead_tube_coral_block.png", 0),
    TUBE_CORAL("1.13", Style.PLANT, Material.plants, "tube_coral", "textures/block/tube_coral.png", 0),
    DEAD_TUBE_CORAL("1.13", Style.PLANT, Material.plants, "dead_tube_coral", "textures/block/dead_tube_coral.png", 0),
    TUBE_CORAL_FAN("1.13", Style.PLANT, Material.plants, "tube_coral_fan", "textures/block/tube_coral_fan.png", 0),
    DEAD_TUBE_CORAL_FAN("1.13", Style.PLANT, Material.plants, "dead_tube_coral_fan", "textures/block/dead_tube_coral_fan.png", 0),
    TUBE_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "tube_coral_fan", "textures/block/tube_coral_fan.png", 0),
    DEAD_TUBE_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "dead_tube_coral_fan", "textures/block/dead_tube_coral_fan.png", 0),
    BRAIN_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "brain_coral_block", "textures/block/brain_coral_block.png", 0),
    DEAD_BRAIN_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "dead_brain_coral_block", "textures/block/dead_brain_coral_block.png", 0),
    BRAIN_CORAL("1.13", Style.PLANT, Material.plants, "brain_coral", "textures/block/brain_coral.png", 0),
    DEAD_BRAIN_CORAL("1.13", Style.PLANT, Material.plants, "dead_brain_coral", "textures/block/dead_brain_coral.png", 0),
    BRAIN_CORAL_FAN("1.13", Style.PLANT, Material.plants, "brain_coral_fan", "textures/block/brain_coral_fan.png", 0),
    DEAD_BRAIN_CORAL_FAN("1.13", Style.PLANT, Material.plants, "dead_brain_coral_fan", "textures/block/dead_brain_coral_fan.png", 0),
    BRAIN_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "brain_coral_fan", "textures/block/brain_coral_fan.png", 0),
    DEAD_BRAIN_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "dead_brain_coral_fan", "textures/block/dead_brain_coral_fan.png", 0),
    BUBBLE_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "bubble_coral_block", "textures/block/bubble_coral_block.png", 0),
    DEAD_BUBBLE_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "dead_bubble_coral_block", "textures/block/dead_bubble_coral_block.png", 0),
    BUBBLE_CORAL("1.13", Style.PLANT, Material.plants, "bubble_coral", "textures/block/bubble_coral.png", 0),
    DEAD_BUBBLE_CORAL("1.13", Style.PLANT, Material.plants, "dead_bubble_coral", "textures/block/dead_bubble_coral.png", 0),
    BUBBLE_CORAL_FAN("1.13", Style.PLANT, Material.plants, "bubble_coral_fan", "textures/block/bubble_coral_fan.png", 0),
    DEAD_BUBBLE_CORAL_FAN("1.13", Style.PLANT, Material.plants, "dead_bubble_coral_fan", "textures/block/dead_bubble_coral_fan.png", 0),
    BUBBLE_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "bubble_coral_fan", "textures/block/bubble_coral_fan.png", 0),
    DEAD_BUBBLE_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "dead_bubble_coral_fan", "textures/block/dead_bubble_coral_fan.png", 0),
    FIRE_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "fire_coral_block", "textures/block/fire_coral_block.png", 0),
    DEAD_FIRE_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "dead_fire_coral_block", "textures/block/dead_fire_coral_block.png", 0),
    FIRE_CORAL("1.13", Style.PLANT, Material.plants, "fire_coral", "textures/block/fire_coral.png", 0),
    DEAD_FIRE_CORAL("1.13", Style.PLANT, Material.plants, "dead_fire_coral", "textures/block/dead_fire_coral.png", 0),
    FIRE_CORAL_FAN("1.13", Style.PLANT, Material.plants, "fire_coral_fan", "textures/block/fire_coral_fan.png", 0),
    DEAD_FIRE_CORAL_FAN("1.13", Style.PLANT, Material.plants, "dead_fire_coral_fan", "textures/block/dead_fire_coral_fan.png", 0),
    FIRE_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "fire_coral_fan", "textures/block/fire_coral_fan.png", 0),
    DEAD_FIRE_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "dead_fire_coral_fan", "textures/block/dead_fire_coral_fan.png", 0),
    HORN_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "horn_coral_block", "textures/block/horn_coral_block.png", 0),
    DEAD_HORN_CORAL_BLOCK("1.13", Style.CUBE, Material.rock, "dead_horn_coral_block", "textures/block/dead_horn_coral_block.png", 0),
    HORN_CORAL("1.13", Style.PLANT, Material.plants, "horn_coral", "textures/block/horn_coral.png", 0),
    DEAD_HORN_CORAL("1.13", Style.PLANT, Material.plants, "dead_horn_coral", "textures/block/dead_horn_coral.png", 0),
    HORN_CORAL_FAN("1.13", Style.PLANT, Material.plants, "horn_coral_fan", "textures/block/horn_coral_fan.png", 0),
    DEAD_HORN_CORAL_FAN("1.13", Style.PLANT, Material.plants, "dead_horn_coral_fan", "textures/block/dead_horn_coral_fan.png", 0),
    HORN_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "horn_coral_fan", "textures/block/horn_coral_fan.png", 0),
    DEAD_HORN_CORAL_WALL_FAN("1.13", Style.WALL_PLANT, Material.plants, "dead_horn_coral_fan", "textures/block/dead_horn_coral_fan.png", 0),
    SCAFFOLDING("1.14", Style.CUTOUT, Material.wood, "scaffolding_side", "textures/block/scaffolding_side.png", 0),
    BELL("1.14", Style.SMALL, Material.iron, "bell_body", "textures/entity/bell/bell_body.png", 0),
    LECTERN("1.14", Style.CUTOUT, Material.wood, "lectern_sides", "textures/block/lectern_sides.png", 0),
    GRINDSTONE("1.14", Style.CUTOUT, Material.rock, "grindstone_side", "textures/block/grindstone_side.png", 0),
    CAMPFIRE("1.14", Style.LAYER, Material.wood, "campfire_log", "textures/block/campfire_log.png", 15),
    JIGSAW("1.14", Style.CUBE, Material.rock, "jigsaw_top", "textures/block/jigsaw_top.png", 0),
    SOUL_CAMPFIRE("1.16", Style.LAYER, Material.wood, "soul_campfire_log", "textures/block/soul_campfire_log.png", 10),
    RESPAWN_ANCHOR("1.16", Style.CUBE, Material.rock, "respawn_anchor_side0", "textures/block/respawn_anchor_side0.png", 3),
    LODESTONE("1.16", Style.CUBE, Material.rock, "lodestone_side", "textures/block/lodestone_side.png", 0),
    CANDLE("1.17", Style.CANDLE, Material.plants, "candle", "textures/block/candle.png", 3),
    WHITE_CANDLE("1.17", Style.CANDLE, Material.plants, "white_candle", "textures/block/white_candle.png", 3),
    ORANGE_CANDLE("1.17", Style.CANDLE, Material.plants, "orange_candle", "textures/block/orange_candle.png", 3),
    MAGENTA_CANDLE("1.17", Style.CANDLE, Material.plants, "magenta_candle", "textures/block/magenta_candle.png", 3),
    LIGHT_BLUE_CANDLE("1.17", Style.CANDLE, Material.plants, "light_blue_candle", "textures/block/light_blue_candle.png", 3),
    YELLOW_CANDLE("1.17", Style.CANDLE, Material.plants, "yellow_candle", "textures/block/yellow_candle.png", 3),
    LIME_CANDLE("1.17", Style.CANDLE, Material.plants, "lime_candle", "textures/block/lime_candle.png", 3),
    PINK_CANDLE("1.17", Style.CANDLE, Material.plants, "pink_candle", "textures/block/pink_candle.png", 3),
    GRAY_CANDLE("1.17", Style.CANDLE, Material.plants, "gray_candle", "textures/block/gray_candle.png", 3),
    LIGHT_GRAY_CANDLE("1.17", Style.CANDLE, Material.plants, "light_gray_candle", "textures/block/light_gray_candle.png", 3),
    CYAN_CANDLE("1.17", Style.CANDLE, Material.plants, "cyan_candle", "textures/block/cyan_candle.png", 3),
    PURPLE_CANDLE("1.17", Style.CANDLE, Material.plants, "purple_candle", "textures/block/purple_candle.png", 3),
    BLUE_CANDLE("1.17", Style.CANDLE, Material.plants, "blue_candle", "textures/block/blue_candle.png", 3),
    BROWN_CANDLE("1.17", Style.CANDLE, Material.plants, "brown_candle", "textures/block/brown_candle.png", 3),
    GREEN_CANDLE("1.17", Style.CANDLE, Material.plants, "green_candle", "textures/block/green_candle.png", 3),
    RED_CANDLE("1.17", Style.CANDLE, Material.plants, "red_candle", "textures/block/red_candle.png", 3),
    BLACK_CANDLE("1.17", Style.CANDLE, Material.plants, "black_candle", "textures/block/black_candle.png", 3),
    CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "candle", "textures/block/candle.png", 3),
    WHITE_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "white_candle", "textures/block/white_candle.png", 3),
    ORANGE_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "orange_candle", "textures/block/orange_candle.png", 3),
    MAGENTA_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "magenta_candle", "textures/block/magenta_candle.png", 3),
    LIGHT_BLUE_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "light_blue_candle", "textures/block/light_blue_candle.png", 3),
    YELLOW_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "yellow_candle", "textures/block/yellow_candle.png", 3),
    LIME_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "lime_candle", "textures/block/lime_candle.png", 3),
    PINK_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "pink_candle", "textures/block/pink_candle.png", 3),
    GRAY_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "gray_candle", "textures/block/gray_candle.png", 3),
    LIGHT_GRAY_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "light_gray_candle", "textures/block/light_gray_candle.png", 3),
    CYAN_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "cyan_candle", "textures/block/cyan_candle.png", 3),
    PURPLE_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "purple_candle", "textures/block/purple_candle.png", 3),
    BLUE_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "blue_candle", "textures/block/blue_candle.png", 3),
    BROWN_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "brown_candle", "textures/block/brown_candle.png", 3),
    GREEN_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "green_candle", "textures/block/green_candle.png", 3),
    RED_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "red_candle", "textures/block/red_candle.png", 3),
    BLACK_CANDLE_CAKE("1.17", Style.CANDLE_CAKE, Material.plants, "black_candle", "textures/block/black_candle.png", 3),
    POWDER_SNOW("1.17", Style.CUBE, Material.snow, "powder_snow", "textures/block/powder_snow.png", 0),
    SCULK_VEIN("1.19", Style.VINE, Material.plants, "sculk_vein", "textures/block/sculk_vein.png", 1),
    SCULK_SENSOR("1.19", Style.SMALL, Material.rock, "sculk_sensor_top", "textures/block/sculk_sensor_top.png", 1),
    SCULK_SHRIEKER("1.19", Style.CUBE, Material.rock, "sculk_shrieker_top", "textures/block/sculk_shrieker_top.png", 0),
    REINFORCED_DEEPSLATE("1.19", Style.CUBE, Material.rock, "reinforced_deepslate", "textures/block/reinforced_deepslate.png", 0),
    FROGSPAWN("1.19", Style.LAYER, Material.plants, "frogspawn", "textures/block/frogspawn.png", 0),
    OCHRE_FROGLIGHT("1.19", Style.CUBE, Material.rock, "ochre_froglight", "textures/block/ochre_froglight.png", 15),
    VERDANT_FROGLIGHT("1.19", Style.CUBE, Material.rock, "verdant_froglight", "textures/block/verdant_froglight.png", 15),
    PEARLESCENT_FROGLIGHT("1.19", Style.CUBE, Material.rock, "pearlescent_froglight", "textures/block/pearlescent_froglight.png", 15),
    MANGROVE_LEAVES("1.19", Style.LEAVES, Material.leaves, "mangrove_leaves", "textures/block/mangrove_leaves.png", 0),
    MANGROVE_PROPAGULE("1.19", Style.PLANT, Material.plants, "mangrove_propagule", "textures/block/mangrove_propagule.png", 0),
    CHISELED_BOOKSHELF("1.20", Style.CUBE, Material.wood, "chiseled_bookshelf_empty", "textures/block/chiseled_bookshelf_empty.png", 0),
    SUSPICIOUS_SAND("1.20", Style.CUBE, Material.sand, "suspicious_sand", "textures/block/suspicious_sand.png", 0),
    SUSPICIOUS_GRAVEL("1.20", Style.CUBE, Material.sand, "suspicious_gravel", "textures/block/suspicious_gravel.png", 0),
    DECORATED_POT("1.20", Style.SMALL, Material.rock, "terracotta", "textures/block/terracotta.png", 0),
    TORCHFLOWER("1.20", Style.PLANT, Material.plants, "torchflower", "textures/block/torchflower.png", 0),
    TORCHFLOWER_CROP("1.20", Style.PLANT, Material.plants, "torchflower_crop_stage1", "textures/block/torchflower_crop_stage1.png", 0),
    POTTED_TORCHFLOWER("1.20", Style.SMALL, Material.rock, "flower_pot", "textures/block/flower_pot.png", 0),
    PITCHER_PLANT("1.20", Style.PLANT, Material.plants, "pitcher_plant_top", "textures/block/pitcher_plant_top.png", 0),
    PITCHER_CROP("1.20", Style.PLANT, Material.plants, "pitcher_crop_top_stage_4", "textures/block/pitcher_crop_top_stage_4.png", 0),
    SNIFFER_EGG("1.20", Style.SMALL, Material.rock, "sniffer_egg", "textures/block/sniffer_egg.png", 0),
    CALIBRATED_SCULK_SENSOR("1.20", Style.SMALL, Material.rock, "calibrated_sculk_sensor_top", "textures/block/calibrated_sculk_sensor_top.png", 1),
    OAK_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "oak_planks", "textures/block/oak_planks.png", 0),
    OAK_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "oak_planks", "textures/block/oak_planks.png", 0),
    SPRUCE_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "spruce_planks", "textures/block/spruce_planks.png", 0),
    SPRUCE_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "spruce_planks", "textures/block/spruce_planks.png", 0),
    BIRCH_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "birch_planks", "textures/block/birch_planks.png", 0),
    BIRCH_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "birch_planks", "textures/block/birch_planks.png", 0),
    JUNGLE_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "jungle_planks", "textures/block/jungle_planks.png", 0),
    JUNGLE_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "jungle_planks", "textures/block/jungle_planks.png", 0),
    ACACIA_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "acacia_planks", "textures/block/acacia_planks.png", 0),
    ACACIA_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "acacia_planks", "textures/block/acacia_planks.png", 0),
    DARK_OAK_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "dark_oak_planks", "textures/block/dark_oak_planks.png", 0),
    DARK_OAK_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "dark_oak_planks", "textures/block/dark_oak_planks.png", 0),
    MANGROVE_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "mangrove_planks", "textures/block/mangrove_planks.png", 0),
    MANGROVE_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "mangrove_planks", "textures/block/mangrove_planks.png", 0),
    CHERRY_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "cherry_planks", "textures/block/cherry_planks.png", 0),
    CHERRY_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "cherry_planks", "textures/block/cherry_planks.png", 0),
    BAMBOO_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "bamboo_planks", "textures/block/bamboo_planks.png", 0),
    BAMBOO_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "bamboo_planks", "textures/block/bamboo_planks.png", 0),
    CRIMSON_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "crimson_planks", "textures/block/crimson_planks.png", 0),
    CRIMSON_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "crimson_planks", "textures/block/crimson_planks.png", 0),
    WARPED_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "warped_planks", "textures/block/warped_planks.png", 0),
    WARPED_WALL_HANGING_SIGN("1.20", Style.HANGING_SIGN, Material.wood, "warped_planks", "textures/block/warped_planks.png", 0),
    TRIAL_SPAWNER("1.21", Style.CUBE, Material.iron, "trial_spawner_side_inactive", "textures/block/trial_spawner_side_inactive.png", 4),
    VAULT("1.21", Style.CUBE, Material.iron, "vault_top", "textures/block/vault_top.png", 6),
    CRAFTER("1.21", Style.CUBE, Material.wood, "crafter_top", "textures/block/crafter_top.png", 0),
    HEAVY_CORE("1.21", Style.SMALL, Material.iron, "heavy_core", "textures/block/heavy_core.png", 0),
    PALE_OAK_PLANKS("1.21.4", Style.CUBE, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_LOG("1.21.4", Style.LOG, Material.wood, "pale_oak_log", "textures/block/pale_oak_log.png", 0),
    PALE_OAK_WOOD("1.21.4", Style.CUBE, Material.wood, "pale_oak_log", "textures/block/pale_oak_log.png", 0),
    STRIPPED_PALE_OAK_LOG("1.21.4", Style.LOG, Material.wood, "stripped_pale_oak_log", "textures/block/stripped_pale_oak_log.png", 0),
    STRIPPED_PALE_OAK_WOOD("1.21.4", Style.CUBE, Material.wood, "stripped_pale_oak_log", "textures/block/stripped_pale_oak_log.png", 0),
    PALE_OAK_LEAVES("1.21.4", Style.LEAVES, Material.leaves, "pale_oak_leaves", "textures/block/pale_oak_leaves.png", 0),
    PALE_OAK_SAPLING("1.21.4", Style.PLANT, Material.plants, "pale_oak_sapling", "textures/block/pale_oak_sapling.png", 0),
    PALE_OAK_STAIRS("1.21.4", Style.STAIRS, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_SLAB("1.21.4", Style.SLAB, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_FENCE("1.21.4", Style.FENCE, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_FENCE_GATE("1.21.4", Style.FENCE_GATE, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_DOOR("1.21.4", Style.DOOR, Material.wood, "pale_oak_door", "textures/block/pale_oak_door.png", 0),
    PALE_OAK_TRAPDOOR("1.21.4", Style.TRAPDOOR, Material.wood, "pale_oak_trapdoor", "textures/block/pale_oak_trapdoor.png", 0),
    PALE_OAK_PRESSURE_PLATE("1.21.4", Style.LAYER, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_BUTTON("1.21.4", Style.BUTTON, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_SIGN("1.21.4", Style.SIGN, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_WALL_SIGN("1.21.4", Style.SIGN, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_HANGING_SIGN("1.21.4", Style.HANGING_SIGN, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_OAK_WALL_HANGING_SIGN("1.21.4", Style.HANGING_SIGN, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
    PALE_MOSS_BLOCK("1.21.4", Style.CUBE, Material.plants, "pale_moss_block", "textures/block/pale_moss_block.png", 0),
    PALE_MOSS_CARPET("1.21.4", Style.LAYER, Material.plants, "pale_moss_carpet", "textures/block/pale_moss_carpet.png", 0),
    PALE_HANGING_MOSS("1.21.4", Style.VINE, Material.plants, "pale_hanging_moss", "textures/block/pale_hanging_moss.png", 0),
    CREAKING_HEART("1.21.4", Style.CUBE, Material.wood, "creaking_heart", "textures/block/creaking_heart.png", 0),
    CLOSED_EYEBLOSSOM("1.21.4", Style.PLANT, Material.plants, "closed_eyeblossom", "textures/block/closed_eyeblossom.png", 0),
    OPEN_EYEBLOSSOM("1.21.4", Style.PLANT, Material.plants, "open_eyeblossom", "textures/block/open_eyeblossom.png", 3),
    RESIN_CLUMP("1.21.4", Style.VINE, Material.plants, "resin_clump", "textures/block/resin_clump.png", 0),
    RESIN_BLOCK("1.21.4", Style.CUBE, Material.rock, "resin_block", "textures/block/resin_block.png", 0),
    RESIN_BRICKS("1.21.4", Style.CUBE, Material.rock, "resin_bricks", "textures/block/resin_bricks.png", 0),
    RESIN_BRICK_SLAB("1.21.4", Style.SLAB, Material.rock, "resin_bricks", "textures/block/resin_bricks.png", 0),
    RESIN_BRICK_STAIRS("1.21.4", Style.STAIRS, Material.rock, "resin_bricks", "textures/block/resin_bricks.png", 0),
    RESIN_BRICK_WALL("1.21.4", Style.WALL, Material.rock, "resin_bricks", "textures/block/resin_bricks.png", 0),
    CHISELED_RESIN_BRICKS("1.21.4", Style.CUBE, Material.rock, "chiseled_resin_bricks", "textures/block/chiseled_resin_bricks.png", 0),
    FIREFLY_BUSH("1.21.5", Style.PLANT, Material.plants, "firefly_bush", "textures/block/firefly_bush.png", 2),
    BUSH("1.21.5", Style.PLANT, Material.plants, "bush", "textures/block/bush.png", 0),
    SHORT_DRY_GRASS("1.21.5", Style.PLANT, Material.plants, "short_dry_grass", "textures/block/short_dry_grass.png", 0),
    TALL_DRY_GRASS("1.21.5", Style.PLANT, Material.plants, "tall_dry_grass", "textures/block/tall_dry_grass.png", 0),
    CACTUS_FLOWER("1.21.5", Style.PLANT, Material.plants, "cactus_flower", "textures/block/cactus_flower.png", 0),
    LEAF_LITTER("1.21.5", Style.LAYER, Material.plants, "leaf_litter", "textures/block/leaf_litter.png", 0),
    WILDFLOWERS("1.21.5", Style.LAYER, Material.plants, "wildflowers", "textures/block/wildflowers.png", 0),
    DRIED_GHAST("1.21.6", Style.SMALL, Material.rock, "dried_ghast", "textures/block/dried_ghast.png", 0),
    COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "copper_block", "textures/block/copper_block.png", 0),
    COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "copper_block", "textures/block/copper_block.png", 0),
    COPPER_BARS("1.21.9", Style.PANE, Material.iron, "copper_bars", "textures/block/copper_bars.png", 0),
    COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "copper_chain", "textures/block/copper_chain.png", 0),
    COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "copper_lantern", "textures/block/copper_lantern.png", 15),
    WAXED_COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "copper_block", "textures/block/copper_block.png", 0),
    WAXED_COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "copper_block", "textures/block/copper_block.png", 0),
    WAXED_COPPER_BARS("1.21.9", Style.PANE, Material.iron, "copper_bars", "textures/block/copper_bars.png", 0),
    WAXED_COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "copper_chain", "textures/block/copper_chain.png", 0),
    WAXED_COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "copper_lantern", "textures/block/copper_lantern.png", 15),
    WAXED_LIGHTNING_ROD("1.21.9", Style.SMALL, Material.iron, "copper_block", "textures/block/copper_block.png", 0),
    EXPOSED_COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "exposed_copper", "textures/block/exposed_copper.png", 0),
    EXPOSED_COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "exposed_copper", "textures/block/exposed_copper.png", 0),
    EXPOSED_COPPER_BARS("1.21.9", Style.PANE, Material.iron, "exposed_copper_bars", "textures/block/exposed_copper_bars.png", 0),
    EXPOSED_COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "exposed_copper_chain", "textures/block/exposed_copper_chain.png", 0),
    EXPOSED_COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "exposed_copper_lantern", "textures/block/exposed_copper_lantern.png", 15),
    EXPOSED_LIGHTNING_ROD("1.21.9", Style.SMALL, Material.iron, "exposed_copper", "textures/block/exposed_copper.png", 0),
    WAXED_EXPOSED_COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "exposed_copper", "textures/block/exposed_copper.png", 0),
    WAXED_EXPOSED_COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "exposed_copper", "textures/block/exposed_copper.png", 0),
    WAXED_EXPOSED_COPPER_BARS("1.21.9", Style.PANE, Material.iron, "exposed_copper_bars", "textures/block/exposed_copper_bars.png", 0),
    WAXED_EXPOSED_COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "exposed_copper_chain", "textures/block/exposed_copper_chain.png", 0),
    WAXED_EXPOSED_COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "exposed_copper_lantern", "textures/block/exposed_copper_lantern.png", 15),
    WAXED_EXPOSED_LIGHTNING_ROD("1.21.9", Style.SMALL, Material.iron, "exposed_copper", "textures/block/exposed_copper.png", 0),
    WEATHERED_COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "weathered_copper", "textures/block/weathered_copper.png", 0),
    WEATHERED_COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "weathered_copper", "textures/block/weathered_copper.png", 0),
    WEATHERED_COPPER_BARS("1.21.9", Style.PANE, Material.iron, "weathered_copper_bars", "textures/block/weathered_copper_bars.png", 0),
    WEATHERED_COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "weathered_copper_chain", "textures/block/weathered_copper_chain.png", 0),
    WEATHERED_COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "weathered_copper_lantern", "textures/block/weathered_copper_lantern.png", 15),
    WEATHERED_LIGHTNING_ROD("1.21.9", Style.SMALL, Material.iron, "weathered_copper", "textures/block/weathered_copper.png", 0),
    WAXED_WEATHERED_COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "weathered_copper", "textures/block/weathered_copper.png", 0),
    WAXED_WEATHERED_COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "weathered_copper", "textures/block/weathered_copper.png", 0),
    WAXED_WEATHERED_COPPER_BARS("1.21.9", Style.PANE, Material.iron, "weathered_copper_bars", "textures/block/weathered_copper_bars.png", 0),
    WAXED_WEATHERED_COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "weathered_copper_chain", "textures/block/weathered_copper_chain.png", 0),
    WAXED_WEATHERED_COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "weathered_copper_lantern", "textures/block/weathered_copper_lantern.png", 15),
    WAXED_WEATHERED_LIGHTNING_ROD("1.21.9", Style.SMALL, Material.iron, "weathered_copper", "textures/block/weathered_copper.png", 0),
    OXIDIZED_COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "oxidized_copper", "textures/block/oxidized_copper.png", 0),
    OXIDIZED_COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "oxidized_copper", "textures/block/oxidized_copper.png", 0),
    OXIDIZED_COPPER_BARS("1.21.9", Style.PANE, Material.iron, "oxidized_copper_bars", "textures/block/oxidized_copper_bars.png", 0),
    OXIDIZED_COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "oxidized_copper_chain", "textures/block/oxidized_copper_chain.png", 0),
    OXIDIZED_COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "oxidized_copper_lantern", "textures/block/oxidized_copper_lantern.png", 15),
    OXIDIZED_LIGHTNING_ROD("1.21.9", Style.SMALL, Material.iron, "oxidized_copper", "textures/block/oxidized_copper.png", 0),
    WAXED_OXIDIZED_COPPER_CHEST("1.21.9", Style.CUBE, Material.iron, "oxidized_copper", "textures/block/oxidized_copper.png", 0),
    WAXED_OXIDIZED_COPPER_GOLEM_STATUE("1.21.9", Style.SMALL, Material.iron, "oxidized_copper", "textures/block/oxidized_copper.png", 0),
    WAXED_OXIDIZED_COPPER_BARS("1.21.9", Style.PANE, Material.iron, "oxidized_copper_bars", "textures/block/oxidized_copper_bars.png", 0),
    WAXED_OXIDIZED_COPPER_CHAIN("1.21.9", Style.CUTOUT, Material.iron, "oxidized_copper_chain", "textures/block/oxidized_copper_chain.png", 0),
    WAXED_OXIDIZED_COPPER_LANTERN("1.21.9", Style.SMALL, Material.iron, "oxidized_copper_lantern", "textures/block/oxidized_copper_lantern.png", 15),
    WAXED_OXIDIZED_LIGHTNING_ROD("1.21.9", Style.SMALL, Material.iron, "oxidized_copper", "textures/block/oxidized_copper.png", 0),
    COPPER_TORCH("1.21.9", Style.TORCH, Material.plants, "copper_torch", "textures/block/copper_torch.png", 14),
    OAK_SHELF("1.21.9", Style.SHELF, Material.wood, "oak_shelf", "textures/block/oak_shelf.png", 0),
    SPRUCE_SHELF("1.21.9", Style.SHELF, Material.wood, "spruce_shelf", "textures/block/spruce_shelf.png", 0),
    BIRCH_SHELF("1.21.9", Style.SHELF, Material.wood, "birch_shelf", "textures/block/birch_shelf.png", 0),
    JUNGLE_SHELF("1.21.9", Style.SHELF, Material.wood, "jungle_shelf", "textures/block/jungle_shelf.png", 0),
    ACACIA_SHELF("1.21.9", Style.SHELF, Material.wood, "acacia_shelf", "textures/block/acacia_shelf.png", 0),
    DARK_OAK_SHELF("1.21.9", Style.SHELF, Material.wood, "dark_oak_shelf", "textures/block/dark_oak_shelf.png", 0),
    MANGROVE_SHELF("1.21.9", Style.SHELF, Material.wood, "mangrove_shelf", "textures/block/mangrove_shelf.png", 0),
    CHERRY_SHELF("1.21.9", Style.SHELF, Material.wood, "cherry_shelf", "textures/block/cherry_shelf.png", 0),
    PALE_OAK_SHELF("1.21.9", Style.SHELF, Material.wood, "pale_oak_shelf", "textures/block/pale_oak_shelf.png", 0),
    BAMBOO_SHELF("1.21.9", Style.SHELF, Material.wood, "bamboo_shelf", "textures/block/bamboo_shelf.png", 0),
    CRIMSON_SHELF("1.21.9", Style.SHELF, Material.wood, "crimson_shelf", "textures/block/crimson_shelf.png", 0),
    WARPED_SHELF("1.21.9", Style.SHELF, Material.wood, "warped_shelf", "textures/block/warped_shelf.png", 0);

    public enum Style {
        CUBE, CUTOUT, PLANT, WALL_PLANT, VINE, LAYER, SMALL, CANDLE, CANDLE_CAKE,
        LEAVES, LOG, SLAB, STAIRS, WALL, FENCE, FENCE_GATE, DOOR, TRAPDOOR, BUTTON,
        SIGN, HANGING_SIGN, PANE, TORCH, SHELF
    }

    private final String vanillaVersion;
    private final Style style;
    private final Material material;
    private final String textureKey;
    private final String modernAssetPath;
    private final int lightLevel;
    private Block block;
    private static boolean parityChestTileRegistered;
    private static boolean paritySignTileRegistered;
    private static boolean parityCampfireTileRegistered;
    private static boolean parityChiseledBookshelfTileRegistered;

    ModernMapParityBlocks(String vanillaVersion, Style style, Material material,
            String textureKey, String modernAssetPath, int lightLevel) {
        this.vanillaVersion = vanillaVersion;
        this.style = style;
        this.material = material;
        this.textureKey = textureKey;
        this.modernAssetPath = modernAssetPath;
        this.lightLevel = lightLevel;
    }

    public String getRegistryName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getVanillaVersion() {
        return vanillaVersion;
    }

    public Style getStyle() {
        return style;
    }

    public Block get() {
        return block;
    }

    public static void init() {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        if (!parityChestTileRegistered) {
            GameRegistry.registerTileEntity(ParityCopperChestTileEntity.class, Tags.MOD_ID + ":modern_parity_copper_chest");
            parityChestTileRegistered = true;
        }
        if (!paritySignTileRegistered) {
            GameRegistry.registerTileEntity(ParitySignTileEntity.class, Tags.MOD_ID + ":modern_parity_sign");
            paritySignTileRegistered = true;
        }
        if (!parityCampfireTileRegistered) {
            GameRegistry.registerTileEntity(ParityCampfireTileEntity.class, Tags.MOD_ID + ":modern_parity_campfire");
            parityCampfireTileRegistered = true;
        }
        if (!parityChiseledBookshelfTileRegistered) {
            GameRegistry.registerTileEntity(ParityChiseledBookshelfTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_chiseled_bookshelf");
            parityChiseledBookshelfTileRegistered = true;
        }
        for (ModernMapParityBlocks entry : values()) {
            String name = entry.getRegistryName();
            if (GameRegistry.findBlock("minecraft", name) != null || GameRegistry.findBlock(Tags.MOD_ID, name) != null) {
                continue;
            }
            entry.block = entry.createBlock();
            entry.block.setBlockName(Tags.MOD_ID + "." + name);
            // Wall sign and coral-wall-fan identities are placement/state-only blocks in modern
            // Minecraft. Keep their block registry identities for imported maps and old worlds,
            // but do not manufacture separate 1.7 ItemBlocks for content that has no obtainable
            // standalone item in 1.21.11. The existing missing-mapping handler deliberately
            // tolerates removed technical ItemBlocks for older EFR saves.
            boolean technicalPlacementBlock = name.endsWith("_wall_sign")
                    || name.endsWith("_wall_hanging_sign") || name.endsWith("_coral_wall_fan")
                    || "potted_torchflower".equals(name);
            if (!technicalPlacementBlock) entry.block.setCreativeTab(EtFuturum.creativeTabBlocks);
            if (entry.lightLevel > 0 && !entry.usesDynamicLight()) entry.block.setLightLevel(entry.lightLevel / 15.0F);
            if (name.endsWith("_coral_wall_fan")) GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name);
            else if ("potted_torchflower".equals(name)) {
                GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name);
            } else if ("torchflower".equals(name)) {
                GameRegistry.registerBlock(entry.block, ParityTorchflowerItemBlock.class, name);
            } else {
                GameRegistry.registerBlock(entry.block, name);
            }
        }
    }

    private boolean usesDynamicLight() {
        String name = getRegistryName();
        return style == Style.CANDLE || style == Style.CANDLE_CAKE
                || "campfire".equals(name) || "soul_campfire".equals(name);
    }

    private Block createBlock() {
        if (getRegistryName().endsWith("copper_chest")) return new ParityCopperChestBlock(this);
        return new ParityModelBlock(this);
    }

    public static ModernMapParityBlocks fromBlock(Block block) {
        if (block == null) return null;
        for (ModernMapParityBlocks entry : values()) {
            if (entry.block == block) return entry;
        }
        return null;
    }

    /**
     * Kept only for compatibility with older validators/callers. Modern parity rendering no
     * longer guesses a single texture per block; its exact model texture dependencies are
     * discovered dynamically from the AssetDirector-provided 1.21.11 JSON model graph.
     */
    public static Map<String, String> getAssetAliases() {
        return Collections.emptyMap();
    }

    /**
     * Copper chest compatibility blocks deliberately reuse vanilla chest behavior/inventory logic.
     * A dedicated TileEntity subclass lets the client bind copper textures without disturbing
     * normal/trapped chest rendering.
     */
    public static final class ParityCopperChestTileEntity extends TileEntityChest {
    }

    /** Text storage for the modern sign/hanging-sign compatibility families. */
    public static final class ParitySignTileEntity extends TileEntityWoodSign {
        // The shared TileEntityWoodSign compatibility layer keeps signs re-editable until waxed.
        @Override
        public boolean func_145914_a() {
            return super.func_145914_a();
        }
    }

    /**
     * Six real inventory slots back the six visual slot properties used by the modern JSON model.
     * Facing remains in the block's low two metadata bits; occupancy is deliberately kept in NBT
     * because the 64 possible slot masks cannot fit into 1.7 metadata without breaking the registry.
     */
    public static final class ParityChiseledBookshelfTileEntity extends TileEntity implements IInventory {
        private final ItemStack[] books = new ItemStack[6];

        public int getOccupancyMask() {
            int mask = 0;
            for (int slot = 0; slot < books.length; slot++) {
                if (books[slot] != null) mask |= 1 << slot;
            }
            return mask;
        }

        private void markDirtyAndSync() {
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        @Override public int getSizeInventory() { return books.length; }
        @Override public ItemStack getStackInSlot(int slot) { return slot >= 0 && slot < books.length ? books[slot] : null; }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            ItemStack stack = getStackInSlot(slot);
            if (stack == null) return null;
            ItemStack removed;
            if (stack.stackSize <= amount) {
                removed = stack;
                books[slot] = null;
            } else {
                removed = stack.splitStack(amount);
                if (stack.stackSize <= 0) books[slot] = null;
            }
            markDirtyAndSync();
            return removed;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            ItemStack stack = getStackInSlot(slot);
            if (stack != null) books[slot] = null;
            return stack;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (slot < 0 || slot >= books.length) return;
            books[slot] = stack;
            if (books[slot] != null && books[slot].stackSize > 1) books[slot].stackSize = 1;
            markDirtyAndSync();
        }

        @Override public String getInventoryName() { return "container.etfuturum.chiseled_bookshelf"; }
        @Override public boolean hasCustomInventoryName() { return false; }
        @Override public int getInventoryStackLimit() { return 1; }
        @Override public boolean isUseableByPlayer(EntityPlayer player) {
            return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                    && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
        }
        @Override public void openInventory() { }
        @Override public void closeInventory() { }
        @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return isBook(stack); }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            NBTTagList list = new NBTTagList();
            for (int slot = 0; slot < books.length; slot++) {
                if (books[slot] == null) continue;
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) slot);
                books[slot].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
            tag.setTag("Items", list);
            tag.setInteger("OccupancyMask", getOccupancyMask());
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            for (int slot = 0; slot < books.length; slot++) books[slot] = null;
            NBTTagList list = tag.getTagList("Items", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound itemTag = list.getCompoundTagAt(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < books.length) books[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
            // Some map conversion paths can preserve only the modern occupied-slot state. Retain
            // that visual information with placeholder books until real item NBT is available.
            int mask = tag.getInteger("OccupancyMask") & 63;
            for (int slot = 0; slot < books.length; slot++) {
                if (books[slot] == null && (mask & (1 << slot)) != 0) books[slot] = new ItemStack(Items.book);
            }
        }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 2, tag);
        }

        @Override
        public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
        }
    }

    /** Makes the existing Torchflower item pot-aware without creating an item for the technical pot block. */
    public static final class ParityTorchflowerItemBlock extends ItemBlock {
        public ParityTorchflowerItemBlock(Block block) {
            super(block);
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                int side, float hitX, float hitY, float hitZ) {
            if (!player.isSneaking() && world.getBlock(x, y, z) == Blocks.flower_pot
                    && POTTED_TORCHFLOWER.get() != null) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof net.minecraft.tileentity.TileEntityFlowerPot
                        && ((net.minecraft.tileentity.TileEntityFlowerPot) tile).getFlowerPotItem() == null) {
                    if (!world.isRemote) {
                        world.setBlock(x, y, z, POTTED_TORCHFLOWER.get(), 0, 3);
                        if (!player.capabilities.isCreativeMode && --stack.stackSize <= 0) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                        }
                    }
                    return true;
                }
            }
            return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        }
    }

    private static boolean isBook(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        return item == Items.book || item == Items.writable_book || item == Items.written_book
                || item == Items.enchanted_book;
    }

    /**
     * Four-slot campfire cooking state. This deliberately implements vanilla-style campfire
     * cooking without importing Campfire Backport source: one item per slot, 30 seconds of
     * accumulated lit time, persistent progress, and item ejection on completion.
     */
    public static final class ParityCampfireTileEntity extends TileEntity {
        private static final int COOK_TIME = 600;
        private final ItemStack[] cookingItems = new ItemStack[4];
        private final int[] cookingTimes = new int[4];

        public ItemStack getCookingItem(int slot) {
            return slot >= 0 && slot < cookingItems.length ? cookingItems[slot] : null;
        }

        public boolean addCookingItem(ItemStack input) {
            if (input == null || input.getItem() == null || !(input.getItem() instanceof ItemFood)) return false;
            ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(input);
            if (result == null) return false;
            for (int i = 0; i < cookingItems.length; i++) {
                if (cookingItems[i] == null) {
                    cookingItems[i] = input.copy();
                    cookingItems[i].stackSize = 1;
                    cookingTimes[i] = 0;
                    markDirtyAndSync();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void updateEntity() {
            if (worldObj == null || worldObj.isRemote) return;
            Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(block);
            if (parity == null || !("campfire".equals(parity.getRegistryName()) || "soul_campfire".equals(parity.getRegistryName()))) return;
            if ((worldObj.getBlockMetadata(xCoord, yCoord, zCoord) & 4) == 0) return;

            boolean changed = false;
            for (int i = 0; i < cookingItems.length; i++) {
                ItemStack input = cookingItems[i];
                if (input == null) continue;
                cookingTimes[i]++;
                if (cookingTimes[i] < COOK_TIME) continue;

                ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(input);
                if (result == null) result = input.copy();
                else result = result.copy();
                EntityItem entity = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.55D, zCoord + 0.5D, result);
                double angle = (Math.PI * 2.0D * i / 4.0D) + Math.PI / 4.0D;
                entity.motionX = Math.cos(angle) * 0.08D;
                entity.motionY = 0.12D;
                entity.motionZ = Math.sin(angle) * 0.08D;
                worldObj.spawnEntityInWorld(entity);
                cookingItems[i] = null;
                cookingTimes[i] = 0;
                changed = true;
            }
            if (changed) markDirtyAndSync();
        }

        public void ejectAll() {
            if (worldObj == null || worldObj.isRemote) return;
            for (int i = 0; i < cookingItems.length; i++) {
                ItemStack stack = cookingItems[i];
                if (stack == null) continue;
                EntityItem entity = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, stack.copy());
                worldObj.spawnEntityInWorld(entity);
                cookingItems[i] = null;
                cookingTimes[i] = 0;
            }
        }

        private void markDirtyAndSync() {
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            NBTTagList list = new NBTTagList();
            for (int i = 0; i < cookingItems.length; i++) {
                if (cookingItems[i] == null) continue;
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);
                itemTag.setInteger("CookTime", cookingTimes[i]);
                cookingItems[i].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
            tag.setTag("CookingItems", list);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            for (int i = 0; i < cookingItems.length; i++) {
                cookingItems[i] = null;
                cookingTimes[i] = 0;
            }
            NBTTagList list = tag.getTagList("CookingItems", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound itemTag = list.getCompoundTagAt(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < 0 || slot >= cookingItems.length) continue;
                cookingItems[slot] = ItemStack.loadItemStackFromNBT(itemTag);
                cookingTimes[slot] = itemTag.getInteger("CookTime");
            }
        }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
        }

        @Override
        public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
        }
    }

    private static final class ParityCopperChestBlock extends BlockChest {
        private final ModernMapParityBlocks entry;

        ParityCopperChestBlock(ModernMapParityBlocks entry) {
            super(0);
            this.entry = entry;
            setHardness(2.5F);
            setResistance(6.0F);
            setStepSound(soundTypeMetal);
            setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        }

        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new ParityCopperChestTileEntity();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            // Use stone only during the bootstrap stitch. Once AssetDirector has resolved
            // the 1.21.11 model, publish one of that model's own atlas icons as the 1.7
            // fallback/particle icon instead of leaving a misleading placeholder behind.
            blockIcon = reg.registerIcon("minecraft:stone");
            ModernJsonModelBridge.PreparedModels prepared = ModernJsonModelBridge.prepare(entry, reg);
            IIcon fallback = ModernJsonModelBridge.getFallbackIcon(prepared);
            if (fallback != null) blockIcon = fallback;
        }
    }

    private static final class ParityModelBlock extends Block {
        private final ModernMapParityBlocks entry;

        ParityModelBlock(ModernMapParityBlocks entry) {
            super(entry.material);
            this.entry = entry;
            // Block's constructor invokes isOpaqueCube() virtually before this field can be assigned.
            // Recompute the cached vanilla opacity values now that the parity entry is available.
            this.opaque = isOpaqueCube();
            this.lightOpacity = this.opaque ? 255 : 0;
            setHardness(isScaffolding() ? 0.0F : entry.material == Material.wood ? 2.0F : entry.material == Material.plants ? 0.0F : 1.5F);
            setResistance(entry.material == Material.plants || isScaffolding() ? 0.0F : 5.0F);
            if (isScaffolding()) {
                // Modern scaffolding breaks essentially instantly even by hand and has its own
                // bamboo/scaffolding acoustic family rather than generic 1.7 wood sounds.
                setStepSound(new SoundType("scaffolding", 1.0F, 1.0F) {
                    @Override public String getBreakSound() { return Tags.MC_ASSET_VER + ":block.scaffolding.break"; }
                    @Override public String getStepResourcePath() { return Tags.MC_ASSET_VER + ":block.scaffolding.step"; }
                    @Override public String func_150496_b() { return Tags.MC_ASSET_VER + ":block.scaffolding.place"; }
                });
            } else if (isHangingSign()) {
                final String family = hangingSignSoundFamily();
                setStepSound(new SoundType("hanging_sign", 1.0F, 1.0F) {
                    @Override public String getBreakSound() { return Tags.MC_ASSET_VER + ":block." + family + ".break"; }
                    @Override public String getStepResourcePath() { return Tags.MC_ASSET_VER + ":block." + family + ".step"; }
                    @Override public String func_150496_b() { return Tags.MC_ASSET_VER + ":block." + family + ".place"; }
                });
            } else {
                setStepSound(entry.material == Material.wood ? soundTypeWood : entry.material == Material.iron ? soundTypeMetal
                        : entry.material == Material.plants || entry.material == Material.leaves ? soundTypeGrass : soundTypeStone);
            }
            if (isCampfire() || isScaffolding()) setTickRandomly(true);
            applyBounds(entry.style);
        }

        private boolean isLightningRod() {
            return entry.getRegistryName().endsWith("lightning_rod");
        }

        private boolean isFenceGate() {
            return entry.style == Style.FENCE_GATE;
        }

        private boolean isGrindstone() {
            return "grindstone".equals(entry.getRegistryName());
        }

        private boolean isSign() {
            String name = entry.getRegistryName();
            return entry.style == Style.SIGN && (name.endsWith("_sign") || name.endsWith("_wall_sign"));
        }

        private boolean isHangingSign() {
            return entry.style == Style.HANGING_SIGN;
        }

        private String hangingSignSoundFamily() {
            String name = entry.getRegistryName();
            if (name.startsWith("bamboo_")) return "bamboo_wood_hanging_sign";
            if (name.startsWith("cherry_")) return "cherry_wood_hanging_sign";
            if (name.startsWith("crimson_") || name.startsWith("warped_")) return "nether_wood_hanging_sign";
            return "hanging_sign";
        }

        private boolean isShelf() {
            return entry.style == Style.SHELF;
        }

        private boolean isChiseledBookshelf() {
            return entry == CHISELED_BOOKSHELF;
        }

        private boolean isFroglight() {
            String name = entry.getRegistryName();
            return name.endsWith("_froglight");
        }

        private boolean isCopperChain() {
            return entry.getRegistryName().endsWith("copper_chain");
        }

        private boolean isCopperLantern() {
            return entry.getRegistryName().endsWith("copper_lantern");
        }

        private boolean canCopperLanternStand(IBlockAccess world, int x, int y, int z) {
            Block below = world.getBlock(x, y - 1, z);
            if (below == null) return false;
            if (world instanceof World && below.canPlaceTorchOnTop((World) world, x, y - 1, z)) return true;
            return World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
        }

        private boolean canCopperLanternHang(IBlockAccess world, int x, int y, int z) {
            Block above = world.getBlock(x, y + 1, z);
            if (above == null) return false;
            ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(above);
            boolean verticalCopperChain = parity != null && parity.getRegistryName().endsWith("copper_chain")
                    && (world.getBlockMetadata(x, y + 1, z) & 3) == 0;
            return world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN, false) || verticalCopperChain
                    || above instanceof BlockFence || above instanceof BlockWall;
        }

        private boolean isCandle() {
            return entry.style == Style.CANDLE;
        }

        private boolean isCandleCake() {
            return entry.style == Style.CANDLE_CAKE;
        }

        private boolean isTurtleEgg() {
            return "turtle_egg".equals(entry.getRegistryName());
        }

        private boolean isCampfire() {
            String name = entry.getRegistryName();
            return "campfire".equals(name) || "soul_campfire".equals(name);
        }

        private boolean isSoulCampfire() {
            return "soul_campfire".equals(entry.getRegistryName());
        }

        private boolean isScaffolding() {
            return "scaffolding".equals(entry.getRegistryName());
        }

        private boolean isCoralFan() {
            String name = entry.getRegistryName();
            return name.endsWith("_coral_fan") && !name.endsWith("_coral_wall_fan");
        }

        private boolean isCoralWallFan() {
            return entry.getRegistryName().endsWith("_coral_wall_fan");
        }

        private ModernMapParityBlocks coralFanCompanion(boolean wall) {
            String name = entry.getRegistryName();
            String target;
            if (wall && isCoralFan()) target = name.substring(0, name.length() - "_coral_fan".length()) + "_coral_wall_fan";
            else if (!wall && isCoralWallFan()) target = name.substring(0, name.length() - "_coral_wall_fan".length()) + "_coral_fan";
            else return entry;
            try {
                return ModernMapParityBlocks.valueOf(target.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private boolean isSegmentedGroundDecal() {
            String name = entry.getRegistryName();
            return "leaf_litter".equals(name) || "wildflowers".equals(name);
        }

        private boolean isWallSignIdentity() {
            String name = entry.getRegistryName();
            return name.endsWith("_wall_sign") || name.endsWith("_wall_hanging_sign");
        }

        private boolean isFullOpaqueModel() {
            if (entry == null) return false;
            if (entry.style != Style.CUBE && entry.style != Style.LOG) return false;
            String name = entry.getRegistryName();
            // These are cube-sized registry blocks whose actual modern visuals contain holes/inset geometry.
            return !"trial_spawner".equals(name) && !"vault".equals(name);
        }

        private void applyBounds(Style style) {
            if (isLightningRod()) {
                setBlockBounds(0.375F, 0.0F, 0.375F, 0.625F, 1.0F, 0.625F);
                return;
            }
            if (isGrindstone()) {
                setBlockBounds(0.125F, 0.0F, 0.0F, 0.875F, 0.75F, 1.0F);
                return;
            }
            String name = entry.getRegistryName();
            if ("conduit".equals(name)) {
                // Modern ConduitBlock uses Block.createCubeShape(6): a centered 6x6x6 shell.
                setBlockBounds(0.3125F, 0.3125F, 0.3125F, 0.6875F, 0.6875F, 0.6875F);
                return;
            }
            if ("decorated_pot".equals(name)) {
                // Modern DecoratedPotBlock is a 14-wide full-height column.
                setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 1.0F, 0.9375F);
                return;
            }
            if ("potted_torchflower".equals(name)) {
                setBlockBounds(5.0F / 16.0F, 0.0F, 5.0F / 16.0F,
                        11.0F / 16.0F, 6.0F / 16.0F, 11.0F / 16.0F);
                return;
            }
            if (isCopperChain()) {
                setBlockBounds(6.5F / 16.0F, 0.0F, 6.5F / 16.0F,
                        9.5F / 16.0F, 1.0F, 9.5F / 16.0F);
                return;
            }
            if (isCopperLantern()) {
                setBlockBounds(5.0F / 16.0F, 0.0F, 5.0F / 16.0F,
                        11.0F / 16.0F, 9.0F / 16.0F, 11.0F / 16.0F);
                return;
            }
            if ("heavy_core".equals(name)) {
                setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
                return;
            }
            if ("sea_pickle".equals(name)) {
                setBlockBounds(0.375F, 0.0F, 0.375F, 0.625F, 0.375F, 0.625F);
                return;
            }
            if ("turtle_egg".equals(name)) {
                setBlockBounds(0.1875F, 0.0F, 0.1875F, 0.75F, 0.4375F, 0.75F);
                return;
            }
            if ("campfire".equals(name) || "soul_campfire".equals(name)) {
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 7.0F / 16.0F, 1.0F);
                return;
            }
            if ("scaffolding".equals(name)) {
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                return;
            }
            if ("leaf_litter".equals(name)) {
                // Modern LeafLitterBlock outline is one model pixel high.
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F / 16.0F, 1.0F);
                return;
            }
            if ("wildflowers".equals(name)) {
                // FlowerBedBlock outline is three model pixels high.
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 3.0F / 16.0F, 1.0F);
                return;
            }
            if (isCoralFan()) {
                // DeadCoralFanBlock/CoralFanBlock use a compact 12x4x12 outline rather than the
                // generic full-height plant selection box. This applies equally to live/dead fans.
                setBlockBounds(2.0F / 16.0F, 0.0F, 2.0F / 16.0F, 14.0F / 16.0F, 4.0F / 16.0F, 14.0F / 16.0F);
                return;
            }
            if (isCoralWallFan()) {
                // Default north-facing wall fan. setBlockBoundsBasedOnState supplies the other
                // three directional variants once metadata is available.
                setBlockBounds(0.0F, 4.0F / 16.0F, 5.0F / 16.0F, 1.0F, 12.0F / 16.0F, 1.0F);
                return;
            }
            if (isShelf()) {
                // Default north-facing Shelf union: 3px rear body plus the 2px front lips.
                setBlockBounds(0.0F, 0.0F, 11.0F / 16.0F, 1.0F, 1.0F, 1.0F);
                return;
            }
            switch (style) {
                case SLAB: setBlockBounds(0,0,0,1,0.5F,1); break;
                case LAYER: setBlockBounds(0,0,0,1,0.125F,1); break;
                case SMALL: setBlockBounds(0.1875F,0,0.1875F,0.8125F,0.625F,0.8125F); break;
                case CANDLE: setBlockBounds(0.4375F,0,0.4375F,0.5625F,0.625F,0.5625F); break;
                case CANDLE_CAKE: setBlockBounds(0.125F,0,0.125F,0.875F,0.625F,0.875F); break;
                case BUTTON: setBlockBounds(0.3125F,0.375F,0.4375F,0.6875F,0.625F,0.5625F); break;
                case SIGN: case HANGING_SIGN: setBlockBounds(0.0625F,0.1875F,0.375F,0.9375F,0.875F,0.625F); break;
                case PLANT: case WALL_PLANT: case VINE: setBlockBounds(0.125F,0,0.125F,0.875F,1,0.875F); break;
                case PANE: setBlockBounds(0.4375F,0,0.4375F,0.5625F,1,0.5625F); break;
                case FENCE: setBlockBounds(0.375F,0,0.375F,0.625F,1,0.625F); break;
                case FENCE_GATE: setBlockBounds(0.0F,0,0.375F,1.0F,1.0F,0.625F); break;
                case WALL: setBlockBounds(0.25F,0,0.25F,0.75F,1,0.75F); break;
                default: setBlockBounds(0,0,0,1,1,1); break;
            }
        }

        private boolean connectsTo(IBlockAccess world, int x, int y, int z, Style style) {
            Block other = world.getBlock(x, y, z);
            if (other == this) return true;
            ModernMapParityBlocks otherEntry = ModernMapParityBlocks.fromBlock(other);
            if (otherEntry != null) {
                if (otherEntry.style == style) return true;
                if (style == Style.FENCE && otherEntry.style == Style.FENCE_GATE) return true;
            }
            return other != null && other.isOpaqueCube();
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
            if (isCopperChain()) {
                int axis = world.getBlockMetadata(x, y, z) & 3;
                if (axis == 1) setBlockBounds(0.0F, 6.5F/16.0F, 6.5F/16.0F, 1.0F, 9.5F/16.0F, 9.5F/16.0F);
                else if (axis == 2) setBlockBounds(6.5F/16.0F, 6.5F/16.0F, 0.0F, 9.5F/16.0F, 9.5F/16.0F, 1.0F);
                else setBlockBounds(6.5F/16.0F, 0.0F, 6.5F/16.0F, 9.5F/16.0F, 1.0F, 9.5F/16.0F);
                return;
            }
            if (isCopperLantern()) {
                boolean hanging = (world.getBlockMetadata(x, y, z) & 1) != 0;
                float minY = hanging ? 1.0F / 16.0F : 0.0F;
                float maxY = hanging ? 1.0F : 9.0F / 16.0F;
                setBlockBounds(5.0F/16.0F, minY, 5.0F/16.0F, 11.0F/16.0F, maxY, 11.0F/16.0F);
                return;
            }
            if (isSign()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                boolean wall = isWallSignIdentity() || (meta >= 2 && meta <= 5);
                if (!wall) {
                    setBlockBounds(0.0F, 0.0F, 0.43F, 1.0F, 0.875F, 0.57F);
                } else {
                    setWallSignBounds(meta, 0.25F, 0.875F, 0.125F);
                }
                return;
            }
            if (isHangingSign()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                boolean wall = isWallSignIdentity() || (!isWallSignIdentity() && meta >= 8);
                if (wall) {
                    int facing = isWallSignIdentity() ? normaliseHorizontalSide(meta) : 2 + (meta - 8);
                    // Modern wall hanging signs project sideways from their support. Their outline
                    // is the board plus the small top mounting plank, centred in the block rather
                    // than flattened against the clicked wall.
                    if (facing == 4 || facing == 5) setBlockBounds(6.0F/16.0F, 0.0F, 0.0F, 10.0F/16.0F, 1.0F, 1.0F);
                    else setBlockBounds(0.0F, 0.0F, 6.0F/16.0F, 1.0F, 1.0F, 10.0F/16.0F);
                } else {
                    int facing = (meta >> 1) & 3;
                    // Cardinal ceiling signs use Mojang's 14x10x2 board outline and rotate with
                    // the rendered model. This fixes the old N/S-only selection box on E/W signs.
                    if ((facing & 1) != 0) setBlockBounds(7.0F/16.0F, 0.0F, 1.0F/16.0F, 9.0F/16.0F, 10.0F/16.0F, 15.0F/16.0F);
                    else setBlockBounds(1.0F/16.0F, 0.0F, 7.0F/16.0F, 15.0F/16.0F, 10.0F/16.0F, 9.0F/16.0F);
                }
                return;
            }
            if (isCoralFan()) {
                setBlockBounds(2.0F / 16.0F, 0.0F, 2.0F / 16.0F, 14.0F / 16.0F, 4.0F / 16.0F, 14.0F / 16.0F);
                return;
            }
            if (isCoralWallFan()) {
                // Metadata uses Forge's horizontal side convention: 2=N, 3=S, 4=W, 5=E.
                // Keep the wall outline directional and local to one block instead of inheriting
                // the generic 12x16x12 PLANT selection box.
                switch (normaliseHorizontalSide(world.getBlockMetadata(x, y, z) & 7)) {
                    // Modern BaseCoralWallFanBlock outline: 16 wide, 8 high, 11 deep,
                    // rotated into the four horizontal facings.
                    case 2: setBlockBounds(0.0F, 4.0F/16.0F, 5.0F/16.0F, 1.0F, 12.0F/16.0F, 1.0F); break;
                    case 3: setBlockBounds(0.0F, 4.0F/16.0F, 0.0F, 1.0F, 12.0F/16.0F, 11.0F/16.0F); break;
                    case 4: setBlockBounds(5.0F/16.0F, 4.0F/16.0F, 0.0F, 1.0F, 12.0F/16.0F, 1.0F); break;
                    case 5: setBlockBounds(0.0F, 4.0F/16.0F, 0.0F, 11.0F/16.0F, 12.0F/16.0F, 1.0F); break;
                    default: setBlockBounds(0.0F, 4.0F/16.0F, 5.0F/16.0F, 1.0F, 12.0F/16.0F, 1.0F); break;
                }
                return;
            }
            if (isGrindstone()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                int face = meta / 4;
                int facing = meta & 3;
                if (face == 0) { // floor
                    if (facing == 1 || facing == 3) setBlockBounds(0.0F,0.0F,0.125F,1.0F,0.75F,0.875F);
                    else setBlockBounds(0.125F,0.0F,0.0F,0.875F,0.75F,1.0F);
                } else if (face == 2) { // ceiling
                    if (facing == 1 || facing == 3) setBlockBounds(0.0F,0.25F,0.125F,1.0F,1.0F,0.875F);
                    else setBlockBounds(0.125F,0.25F,0.0F,0.875F,1.0F,1.0F);
                } else { // wall
                    if (facing == 1 || facing == 3) setBlockBounds(0.0F,0.125F,0.125F,0.75F,0.875F,0.875F);
                    else setBlockBounds(0.125F,0.125F,0.0F,0.875F,0.875F,0.75F);
                }
                return;
            }
            if (isLightningRod()) {
                int facing = world.getBlockMetadata(x, y, z) & 7;
                if (facing == 2 || facing == 3) setBlockBounds(0.375F,0.375F,0.0F,0.625F,0.625F,1.0F);
                else if (facing == 4 || facing == 5) setBlockBounds(0.0F,0.375F,0.375F,1.0F,0.625F,0.625F);
                else setBlockBounds(0.375F,0.0F,0.375F,0.625F,1.0F,0.625F);
                return;
            }
            if (isFenceGate()) {
                int direction = world.getBlockMetadata(x, y, z) & 3;
                if (direction == 1 || direction == 3) {
                    setBlockBounds(0.375F, 0.0F, 0.0F, 0.625F, 1.0F, 1.0F);
                } else {
                    setBlockBounds(0.0F, 0.0F, 0.375F, 1.0F, 1.0F, 0.625F);
                }
                return;
            }
            if (isShelf()) {
                int facing = world.getBlockMetadata(x, y, z) & 3;
                switch (facing) {
                    case 1: setBlockBounds(0.0F, 0.0F, 0.0F, 5.0F / 16.0F, 1.0F, 1.0F); break;
                    case 2: setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 5.0F / 16.0F); break;
                    case 3: setBlockBounds(11.0F / 16.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F); break;
                    default: setBlockBounds(0.0F, 0.0F, 11.0F / 16.0F, 1.0F, 1.0F, 1.0F); break;
                }
                return;
            }
            if (isCandle()) {
                int candles = (world.getBlockMetadata(x, y, z) & 3) + 1;
                if (candles == 1) setBlockBounds(7.0F/16.0F, 0.0F, 7.0F/16.0F, 9.0F/16.0F, 7.0F/16.0F, 9.0F/16.0F);
                else if (candles == 2) setBlockBounds(5.0F/16.0F, 0.0F, 5.0F/16.0F, 11.0F/16.0F, 7.0F/16.0F, 11.0F/16.0F);
                else setBlockBounds(3.0F/16.0F, 0.0F, 3.0F/16.0F, 13.0F/16.0F, 7.0F/16.0F, 13.0F/16.0F);
                return;
            }
            if (isTurtleEgg()) {
                int eggs = (world.getBlockMetadata(x, y, z) & 3) + 1;
                if (eggs == 1) setBlockBounds(3.0F/16.0F,0.0F,3.0F/16.0F,12.0F/16.0F,7.0F/16.0F,12.0F/16.0F);
                else setBlockBounds(1.0F/16.0F,0.0F,1.0F/16.0F,15.0F/16.0F,7.0F/16.0F,15.0F/16.0F);
                return;
            }
            if (isCampfire()) {
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 7.0F / 16.0F, 1.0F);
                return;
            }
            if (isScaffolding()) {
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                return;
            }
            if (isSegmentedGroundDecal()) {
                setSegmentedGroundDecalBounds(world.getBlockMetadata(x, y, z) & 15);
                return;
            }
            if (entry.style == Style.PANE || entry.style == Style.FENCE || entry.style == Style.WALL) {
                boolean n = connectsTo(world, x, y, z - 1, entry.style);
                boolean s = connectsTo(world, x, y, z + 1, entry.style);
                boolean w = connectsTo(world, x - 1, y, z, entry.style);
                boolean e = connectsTo(world, x + 1, y, z, entry.style);
                if (entry.style == Style.PANE) {
                    setBlockBounds(w ? 0.0F : 0.4375F, 0.0F, n ? 0.0F : 0.4375F,
                            e ? 1.0F : 0.5625F, 1.0F, s ? 1.0F : 0.5625F);
                } else if (entry.style == Style.FENCE) {
                    setBlockBounds(w ? 0.0F : 0.375F, 0.0F, n ? 0.0F : 0.375F,
                            e ? 1.0F : 0.625F, 1.0F, s ? 1.0F : 0.625F);
                } else {
                    setBlockBounds(w ? 0.0F : 0.25F, 0.0F, n ? 0.0F : 0.25F,
                            e ? 1.0F : 0.75F, 1.0F, s ? 1.0F : 0.75F);
                }
                return;
            }
            applyBounds(entry.style);
        }

        /**
         * Legacy selection boxes cannot represent modern VoxelShape unions, but amounts 1 and 2
         * have exact single-AABB quarter/half footprints. Amount 3 is an L-shape in modern
         * Minecraft, so its narrowest truthful legacy envelope is the full block; amount 4 is full.
         * Facing follows the same N/E/S/W rotation used by the JSON models in metadata bits 0..1.
         */
        private void setSegmentedGroundDecalBounds(int meta) {
            int facing = meta & 3;
            int amount = ((meta >> 2) & 3) + 1;
            float height = "leaf_litter".equals(entry.getRegistryName()) ? 1.0F / 16.0F : 3.0F / 16.0F;

            if (amount >= 3) {
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, height, 1.0F);
                return;
            }

            if (amount == 1) {
                switch (facing) {
                    case 1: setBlockBounds(0.5F, 0.0F, 0.0F, 1.0F, height, 0.5F); break; // east
                    case 2: setBlockBounds(0.5F, 0.0F, 0.5F, 1.0F, height, 1.0F); break; // south
                    case 3: setBlockBounds(0.0F, 0.0F, 0.5F, 0.5F, height, 1.0F); break; // west
                    default: setBlockBounds(0.0F, 0.0F, 0.0F, 0.5F, height, 0.5F); break; // north
                }
                return;
            }

            // Amount 2 is the first two 8x8 segments: west half when north-facing, then rotate.
            switch (facing) {
                case 1: setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, height, 0.5F); break; // north half
                case 2: setBlockBounds(0.5F, 0.0F, 0.0F, 1.0F, height, 1.0F); break; // east half
                case 3: setBlockBounds(0.0F, 0.0F, 0.5F, 1.0F, height, 1.0F); break; // south half
                default: setBlockBounds(0.0F, 0.0F, 0.0F, 0.5F, height, 1.0F); break; // west half
            }
        }

        private void setWallSignBounds(int side, float minY, float maxY, float depth) {
            switch (side) {
                case 2: setBlockBounds(0.0F, minY, 1.0F - depth, 1.0F, maxY, 1.0F); break;
                case 3: setBlockBounds(0.0F, minY, 0.0F, 1.0F, maxY, depth); break;
                case 4: setBlockBounds(1.0F - depth, minY, 0.0F, 1.0F, maxY, 1.0F); break;
                case 5: setBlockBounds(0.0F, minY, 0.0F, depth, maxY, 1.0F); break;
                default: setBlockBounds(0.0F, minY, 0.375F, 1.0F, maxY, 0.625F); break;
            }
        }

        private static int normaliseHorizontalSide(int side) {
            return side >= 2 && side <= 5 ? side : 3;
        }

        @Override
        public Item getItemDropped(int meta, Random random, int fortune) {
            if (isCoralWallFan()) {
                ModernMapParityBlocks floor = coralFanCompanion(false);
                if (floor != null && floor.get() != null) return Item.getItemFromBlock(floor.get());
            }
            return super.getItemDropped(meta, random, fortune);
        }

        @Override
        public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
            if (entry == POTTED_TORCHFLOWER && TORCHFLOWER.get() != null) {
                return new ItemStack(TORCHFLOWER.get());
            }
            if (isCoralWallFan()) {
                ModernMapParityBlocks floor = coralFanCompanion(false);
                if (floor != null && floor.get() != null) return new ItemStack(floor.get());
            }
            return super.getPickBlock(target, world, x, y, z, player);
        }

        @Override
        public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
            super.onBlockClicked(world, x, y, z, player);
            if (isHangingSign() && world != null && !world.isRemote) {
                playModernSound(world, x, y, z, "block." + hangingSignSoundFamily() + ".hit", 0.75F, 1.0F);
            }
        }

        @Override
        public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
            super.onFallenUpon(world, x, y, z, entity, fallDistance);
            if (isHangingSign() && fallDistance > 0.0F && world != null && !world.isRemote) {
                playModernSound(world, x, y, z, "block." + hangingSignSoundFamily() + ".fall", 0.75F, 1.0F);
            }
        }

        @Override
        public int onBlockPlaced(World world, int x, int y, int z, int side,
                float hitX, float hitY, float hitZ, int meta) {
            if (isFroglight() || isCopperChain()) {
                if (side == 4 || side == 5) return 1; // X
                if (side == 2 || side == 3) return 2; // Z
                return 0; // Y
            }
            if (isCopperLantern()) {
                if (side == 0) return 1;
                if (side == 1) return 0;
                return canCopperLanternStand(world, x, y, z) ? 0 : 1;
            }
            if (isCandle() || isCandleCake() || isTurtleEgg()) return 0;
            if (isCampfire()) return 4; // facing is filled by onBlockPlacedBy; campfires start lit.
            if (isScaffolding()) return computeScaffoldingMeta(world, x, y, z);
            if (isSign()) {
                if (isWallSignIdentity()) return side >= 2 && side <= 5 ? side : 3;
                // One obtainable sign item may visually resolve standing or wall placement.
                return side >= 2 && side <= 5 ? side : 0;
            }
            if (isHangingSign()) {
                if (isWallSignIdentity()) return side >= 2 && side <= 5 ? side : 3;
                // Keep wall placement distinct from ceiling rotation metadata. 8..11 encode
                // north/south/east/west wall placement for the obtainable hanging-sign identity.
                if (side >= 2 && side <= 5) return 8 + (side - 2);
                // Ceiling placement uses bit 0 for attachment type and bits 1..2 for four-way
                // player-facing rotation, filled in by onBlockPlacedBy.
                Block support = world.getBlock(x, y + 1, z);
                return support != null && support.isOpaqueCube() ? 0 : 1;
            }
            if (isCoralFan()) {
                // The one obtainable coral-fan item chooses its technical wall block when used
                // against a horizontal face. Temporarily preserve the clicked side in metadata;
                // onBlockPlacedBy swaps the registry identity after ItemBlock has placed it.
                return side >= 2 && side <= 5 ? side : 0;
            }
            if (isGrindstone()) {
                // 0..3=floor N/E/S/W, 4..7=wall N/E/S/W, 8..11=ceiling N/E/S/W.
                if (side == 0) return 8;
                if (side == 1) return 0;
                if (side == 2) return 6; // clicked north face -> grindstone faces south
                if (side == 3) return 4; // south -> north
                if (side == 4) return 5; // west -> east
                if (side == 5) return 7; // east -> west
                return 0;
            }
            if (!isLightningRod()) return meta;
            // Metadata convention: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east.
            switch (side) {
                case 0: return 0;
                case 1: return 1;
                case 2: return 2;
                case 3: return 3;
                case 4: return 4;
                case 5: return 5;
                default: return 1;
            }
        }

        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
            int quadrant = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            if (isCoralFan()) {
                int side = world.getBlockMetadata(x, y, z) & 7;
                if (side >= 2 && side <= 5) {
                    ModernMapParityBlocks wallEntry = coralFanCompanion(true);
                    if (wallEntry != null && wallEntry.get() != null) {
                        world.setBlock(x, y, z, wallEntry.get(), side, 3);
                    } else {
                        world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                    }
                } else if (side != 0) {
                    world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                }
                return;
            }
            if (isHangingSign()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                if (!isWallSignIdentity() && meta < 8) {
                    int attachment = placer.isSneaking() ? 1 : (meta & 1);
                    int facing = (quadrant + 2) & 3;
                    world.setBlockMetadataWithNotify(x, y, z, attachment | (facing << 1), 2);
                } else {
                    int clickedSide = isWallSignIdentity() ? normaliseHorizontalSide(meta) : 2 + (meta - 8);
                    int facingSide = perpendicularWallHangingFacing(clickedSide);
                    int placedMeta = isWallSignIdentity() ? facingSide : 8 + (facingSide - 2);
                    world.setBlockMetadataWithNotify(x, y, z, placedMeta, 2);
                }
                if (!world.isRemote && placer instanceof EntityPlayerMP) {
                    // Wall hanging signs have two writable faces.  Open whichever face the placer
                    // is actually standing on instead of always forcing the stored "front" face;
                    // otherwise one of the perpendicular wall orientations writes away from them.
                    int placedMeta = world.getBlockMetadata(x, y, z) & 15;
                    boolean wallForm = isWallSignIdentity() || (!isWallSignIdentity() && placedMeta >= 8);
                    boolean back = wallForm && signBackSide((EntityPlayer) placer, x, y, z, placedMeta);
                    openParitySignEditor(world, x, y, z, (EntityPlayerMP) placer, back);
                }
                return;
            }
            if (isSign()) {
                if (!world.isRemote && placer instanceof EntityPlayerMP) openParitySignEditor(world, x, y, z, (EntityPlayerMP) placer, false);
                return;
            }
            if (isCampfire()) {
                int lit = world.getBlockMetadata(x, y, z) & 4;
                world.setBlockMetadataWithNotify(x, y, z, lit | ((quadrant + 2) & 3), 2);
                return;
            }
            if (isScaffolding()) {
                world.setBlockMetadataWithNotify(x, y, z, computeScaffoldingMeta(world, x, y, z), 2);
                world.scheduleBlockUpdate(x, y, z, this, 1);
                return;
            }
            if (isGrindstone()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                int faceBase = (meta / 4) * 4;
                if (faceBase == 4) return; // wall direction already follows the clicked face
                int facing = (quadrant + 2) & 3; // N/E/S/W encoding used by the JSON bridge
                world.setBlockMetadataWithNotify(x, y, z, faceBase + facing, 2);
                return;
            }
            if (isFenceGate()) {
                // Match the mature 1.7 fence-gate metadata contract: bits 0..1 are the
                // horizontal player-facing quadrant and bit 2 is OPEN.
                world.setBlockMetadataWithNotify(x, y, z, quadrant, 2);
                return;
            }
            if (isChiseledBookshelf()) {
                world.setBlockMetadataWithNotify(x, y, z, quadrant, 2);
                return;
            }
            if (isShelf()) {
                // Low two bits are N/E/S/W and bit 2 is the modern POWERED state.
                int powered = world.isBlockIndirectlyGettingPowered(x, y, z) ? 4 : 0;
                world.setBlockMetadataWithNotify(x, y, z, quadrant | powered, 2);
                return;
            }
            if (isSegmentedGroundDecal()) {
                // Keep amount in bits 2..3 and recover the modern horizontal facing in bits 0..1.
                int amountBits = world.getBlockMetadata(x, y, z) & 12;
                world.setBlockMetadataWithNotify(x, y, z, amountBits | quadrant, 2);
            }
        }

        private void openParitySignEditor(World world, int x, int y, int z, EntityPlayerMP player, boolean back) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof ParitySignTileEntity)) return;
            TileEntityWoodSign sign = (TileEntityWoodSign) tile;
            if (sign.isWaxed()) {
                playModernSound(world, x, y, z, isHangingSign() ? "block.hanging_sign.waxed_interact_fail" : "block.sign.waxed_interact_fail", 1.0F, 1.0F);
                return;
            }
            sign.func_145912_a(player);
            EtFuturum.networkWrapper.sendTo(new WoodSignOpenMessage(sign, Block.getIdFromBlock(this), back), player);
        }

        private static int perpendicularWallHangingFacing(int clickedSide) {
            switch (clickedSide) {
                case 2: return 5; // north wall -> board projects east/west
                case 3: return 4; // south wall -> west/east
                case 4: return 2; // west wall -> north/south
                case 5: return 3; // east wall -> south/north
                default: return 5;
            }
        }

        private static int chiseledBookshelfSlot(int facing, float hitX, float hitY, float hitZ) {
            float horizontal;
            switch (facing & 3) {
                case 1: horizontal = hitZ; break;
                case 2: horizontal = 1.0F - hitX; break;
                case 3: horizontal = 1.0F - hitZ; break;
                default: horizontal = hitX; break;
            }
            int column = Math.max(0, Math.min(2, (int) (horizontal * 3.0F)));
            int row = hitY >= 0.5F ? 0 : 1;
            return row * 3 + column;
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
                float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem();

            if (entry == POTTED_TORCHFLOWER) {
                if (held != null) return false;
                if (!world.isRemote) {
                    world.setBlock(x, y, z, Blocks.flower_pot, 0, 3);
                    ItemStack flower = new ItemStack(Item.getItemFromBlock(TORCHFLOWER.get()));
                    if (!player.inventory.addItemStackToInventory(flower)) {
                        world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, flower));
                    }
                }
                return true;
            }

            if (isChiseledBookshelf()) {
                int facing = world.getBlockMetadata(x, y, z) & 3;
                int expectedSide = facing == 0 ? 2 : facing == 1 ? 5 : facing == 2 ? 3 : 4;
                if (side != expectedSide) return false;
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof ParityChiseledBookshelfTileEntity)) return false;
                ParityChiseledBookshelfTileEntity shelf = (ParityChiseledBookshelfTileEntity) tile;
                int slot = chiseledBookshelfSlot(facing, hitX, hitY, hitZ);
                ItemStack stored = shelf.getStackInSlot(slot);
                if (stored == null && isBook(held)) {
                    if (!world.isRemote) {
                        ItemStack inserted = held.copy();
                        inserted.stackSize = 1;
                        shelf.setInventorySlotContents(slot, inserted);
                        if (!player.capabilities.isCreativeMode && --held.stackSize <= 0) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                        }
                        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 0.5F, 1.0F);
                    }
                    return true;
                }
                if (stored != null && held == null) {
                    if (!world.isRemote) {
                        ItemStack removed = shelf.getStackInSlotOnClosing(slot);
                        shelf.markDirtyAndSync();
                        if (!player.inventory.addItemStackToInventory(removed)) {
                            world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, removed));
                        }
                        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 0.5F, 0.85F);
                    }
                    return true;
                }
                return false;
            }

            if (isSign() || isHangingSign()) {
                // Sneak-use with a hanging-sign item must fall through to ItemBlock placement so
                // another hanging sign can be chained from the clicked support/sign.
                if (isHangingSign() && player.isSneaking() && held != null && held.getItem() instanceof net.minecraft.item.ItemBlock) {
                    Block heldBlock = Block.getBlockFromItem(held.getItem());
                    ModernMapParityBlocks heldEntry = ModernMapParityBlocks.fromBlock(heldBlock);
                    if (heldEntry != null && heldEntry.getStyle() == Style.HANGING_SIGN) return false;
                }
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof TileEntityWoodSign)) return true;
                TileEntityWoodSign sign = (TileEntityWoodSign) tile;
                boolean back = signBackSide(player, x, y, z, world.getBlockMetadata(x, y, z) & 15);
                if (tryApplySignModifier(world, x, y, z, player, held, sign, back)) return true;
                if (!world.isRemote && player instanceof EntityPlayerMP) openParitySignEditor(world, x, y, z, (EntityPlayerMP) player, back);
                return true;
            }

            if (isFenceGate()) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                if ((meta & 4) != 0) {
                    world.setBlockMetadataWithNotify(x, y, z, meta & ~4, 2);
                } else {
                    int playerDirection = MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
                    int gateDirection = meta & 3;
                    if (gateDirection == ((playerDirection + 2) & 3)) meta = playerDirection;
                    world.setBlockMetadataWithNotify(x, y, z, meta | 4, 2);
                }
                world.playAuxSFXAtEntity(player, 1003, x, y, z, 0);
                return true;
            }

            if (isCandle()) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                int count = (meta & 3) + 1;
                boolean lit = (meta & 4) != 0;
                if (held != null && held.getItem() == Item.getItemFromBlock(this) && count < 4) {
                    if (!world.isRemote) {
                        setMetadataAndRelight(world, x, y, z, (meta & 4) | count);
                        if (!player.capabilities.isCreativeMode) held.stackSize--;
                        playModernSound(world, x, y, z, "block.candle.place", 1.0F, 1.0F);
                    }
                    return true;
                }
                if (!lit && isIgnitionItem(held)) {
                    if (!world.isRemote) {
                        setMetadataAndRelight(world, x, y, z, meta | 4);
                        consumeIgnitionItem(player, held);
                        playModernSound(world, x, y, z, ignitionSound(held), 1.0F, 1.0F);
                    }
                    return true;
                }
                if (lit && held == null) {
                    if (!world.isRemote) {
                        setMetadataAndRelight(world, x, y, z, meta & 3);
                        playModernSound(world, x, y, z, "block.candle.extinguish", 1.0F, 1.0F);
                    }
                    return true;
                }
                return false;
            }

            if (isCandleCake()) {
                int meta = world.getBlockMetadata(x, y, z) & 1;
                boolean lit = meta != 0;
                if (!lit && isIgnitionItem(held)) {
                    if (!world.isRemote) {
                        setMetadataAndRelight(world, x, y, z, 1);
                        consumeIgnitionItem(player, held);
                        playModernSound(world, x, y, z, ignitionSound(held), 1.0F, 1.0F);
                    }
                    return true;
                }
                if (lit && held == null) {
                    if (!world.isRemote) {
                        setMetadataAndRelight(world, x, y, z, 0);
                        playModernSound(world, x, y, z, "block.candle.extinguish", 1.0F, 1.0F);
                    }
                    return true;
                }
                return false;
            }

            if (isTurtleEgg()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                int eggs = (meta & 3) + 1;
                if (held == null || held.getItem() != Item.getItemFromBlock(this) || eggs >= 4) return false;
                if (!world.isRemote) {
                    // Preserve hatch stage in bits 2..3 and increment the egg count in bits 0..1.
                    world.setBlockMetadataWithNotify(x, y, z, (meta & 12) | eggs, 3);
                    if (!player.capabilities.isCreativeMode) held.stackSize--;
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, stepSound.func_150496_b(), 1.0F, 1.0F);
                }
                return true;
            }

            if (isCampfire()) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                boolean lit = (meta & 4) != 0;
                if (!lit && isIgnitionItem(held)) {
                    if (!world.isRemote) {
                        setMetadataAndRelight(world, x, y, z, meta | 4);
                        consumeIgnitionItem(player, held);
                        playModernSound(world, x, y, z, ignitionSound(held), 1.0F, 1.0F);
                    }
                    return true;
                }
                if (lit && held != null && (held.getItem() instanceof ItemSpade || held.getItem() == Items.water_bucket)) {
                    if (!world.isRemote) {
                        setMetadataAndRelight(world, x, y, z, meta & 3);
                        if (held.getItem() == Items.water_bucket && !player.capabilities.isCreativeMode) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.bucket));
                        }
                        playModernSound(world, x, y, z, "block.fire.extinguish", 1.0F, 1.0F);
                    }
                    return true;
                }
                if (lit && held != null && held.getItem() instanceof ItemFood) {
                    TileEntity tile = world.getTileEntity(x, y, z);
                    if (!(tile instanceof ParityCampfireTileEntity)) return false;
                    if (world.isRemote) return true;
                    if (((ParityCampfireTileEntity) tile).addCookingItem(held)) {
                        if (!player.capabilities.isCreativeMode) held.stackSize--;
                        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 0.4F, 1.2F);
                        return true;
                    }
                }
                return false;
            }

            if (isScaffolding()) {
                if (held == null || held.getItem() != Item.getItemFromBlock(this)) return false;

                // Modern scaffolding placement deliberately differs from ordinary ItemBlock placement:
                // - top-face use grows outward in the player's horizontal facing direction;
                // - ordinary side/bottom use grows upward through the scaffold column;
                // - sneaking follows the actually clicked face for deliberate extensions.
                ForgeDirection direction;
                if (player.isSneaking()) {
                    direction = ForgeDirection.getOrientation(side);
                } else if (side == 1) {
                    direction = horizontalFacing(player);
                } else {
                    direction = ForgeDirection.UP;
                }

                int tx = x + direction.offsetX;
                int ty = y + direction.offsetY;
                int tz = z + direction.offsetZ;
                int scanned = 0;
                while (ty >= 0 && ty <= 255 && world.getBlock(tx, ty, tz) == this && scanned++ < 7) {
                    tx += direction.offsetX;
                    ty += direction.offsetY;
                    tz += direction.offsetZ;
                }
                if (ty < 0 || ty > 255 || scanned > 7 || !world.isAirBlock(tx, ty, tz) || !canPlaceBlockAt(world, tx, ty, tz)) return false;
                if (!world.isRemote) {
                    int scaffoldMeta = computeScaffoldingMeta(world, tx, ty, tz);
                    if (!world.setBlock(tx, ty, tz, this, scaffoldMeta, 3)) return false;
                    if (!player.capabilities.isCreativeMode) held.stackSize--;
                    world.playSoundEffect(tx + 0.5D, ty + 0.5D, tz + 0.5D, stepSound.func_150496_b(), 1.0F, 1.0F);
                }
                return true;
            }

            if (!isSegmentedGroundDecal()) return false;
            if (held == null || held.getItem() != Item.getItemFromBlock(this)) return false;

            int meta = world.getBlockMetadata(x, y, z) & 15;
            int amount = ((meta >> 2) & 3) + 1;
            if (amount >= 4) return false;

            world.playSound((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F,
                    stepSound.func_150496_b(), (stepSound.getVolume() + 1.0F) / 2.0F,
                    stepSound.getPitch() * 0.8F, false);
            world.setBlockMetadataWithNotify(x, y, z, (meta & 3) | (amount << 2), 3);
            if (!player.capabilities.isCreativeMode) held.stackSize--;
            return true;
        }

        private boolean signBackSide(EntityPlayer player, int x, int y, int z, int meta) {
            double fx = 0.0D;
            double fz = 1.0D;
            if (isHangingSign()) {
                boolean wall = isWallSignIdentity() || (!isWallSignIdentity() && meta >= 8);
                if (!wall) {
                    switch ((meta >> 1) & 3) {
                        case 1: fx = -1.0D; fz = 0.0D; break;
                        case 2: fx = 0.0D; fz = -1.0D; break;
                        case 3: fx = 1.0D; fz = 0.0D; break;
                        default: fx = 0.0D; fz = 1.0D; break;
                    }
                } else {
                    int facing = isWallSignIdentity() ? normaliseHorizontalSide(meta) : 2 + (meta - 8);
                    switch (facing) {
                        case 2: fx = 0.0D; fz = -1.0D; break;
                        case 4: fx = 1.0D; fz = 0.0D; break;
                        case 5: fx = -1.0D; fz = 0.0D; break;
                        default: fx = 0.0D; fz = 1.0D; break;
                    }
                }
            } else {
                // Ordinary parity signs render their established front plane on local -Z.
                switch (meta) {
                    case 2: fx = 0.0D; fz = 1.0D; break;
                    case 4: fx = -1.0D; fz = 0.0D; break;
                    case 5: fx = 1.0D; fz = 0.0D; break;
                    default: fx = 0.0D; fz = -1.0D; break;
                }
            }
            double dx = player.posX - (x + 0.5D);
            double dz = player.posZ - (z + 0.5D);
            return dx * fx + dz * fz < 0.0D;
        }

        private boolean tryApplySignModifier(World world, int x, int y, int z, EntityPlayer player,
                ItemStack held, TileEntityWoodSign sign, boolean back) {
            if (sign.isWaxed()) {
                if (!world.isRemote) playModernSound(world, x, y, z,
                        isHangingSign() ? "block.hanging_sign.waxed_interact_fail" : "block.sign.waxed_interact_fail", 1.0F, 1.0F);
                return true;
            }
            if (held == null) return false;

            if (held.getItem() == ModItems.HONEYCOMB.get()) {
                if (world.isRemote) spawnSignWaxOnParticles(world, x, y, z);
                else {
                    sign.setWaxed(true);
                    consumeOne(player, held);
                    playModernSound(world, x, y, z, "item.honeycomb.wax_on", 1.0F, 1.0F);
                }
                return true;
            }
            if (held.getItem() == ModItems.GLOW_INK_SAC.get()) {
                if (sign.isGlowing(back)) return true;
                if (!world.isRemote) {
                    sign.setGlowing(back, true);
                    consumeOne(player, held);
                    playModernSound(world, x, y, z, "item.glow_ink_sac.use", 1.0F, 1.0F);
                }
                return true;
            }
            if (held.getItem() == Items.dye && held.getItemDamage() == 0 && sign.isGlowing(back)) {
                if (!world.isRemote) {
                    sign.setGlowing(back, false);
                    consumeOne(player, held);
                    playModernSound(world, x, y, z, "item.ink_sac.use", 1.0F, 1.0F);
                }
                return true;
            }

            Integer dye = signDyeColour(held);
            if (dye != null) {
                if (!world.isRemote) {
                    sign.setTextColour(back, dye);
                    consumeOne(player, held);
                    playModernSound(world, x, y, z, "item.dye.use", 1.0F, 1.0F);
                }
                return true;
            }
            return false;
        }

        private static Integer signDyeColour(ItemStack held) {
            if (held == null) return null;
            if (held.getItem() == Items.dye) {
                int meta = MathHelper.clamp_int(held.getItemDamage(), 0, 15);
                if (meta == 0) return null; // 1.7 ink sac is reserved for removing glow.
                return modernSignTextColourForLegacyDyeMeta(meta);
            }
            if (held.getItem() == ModItems.DYE.get()) {
                int[] legacyMeta = {15, 4, 3, 0}; // white, blue, brown, black
                int meta = MathHelper.clamp_int(held.getItemDamage(), 0, legacyMeta.length - 1);
                return modernSignTextColourForLegacyDyeMeta(legacyMeta[meta]);
            }
            return null;
        }

        private static int modernSignTextColourForLegacyDyeMeta(int meta) {
            // 1.21 DyeColor#getTextColor values arranged in the 1.7 dye metadata order:
            // black, red, green, brown, blue, purple, cyan, light gray, gray, pink,
            // lime, yellow, light blue, magenta, orange, white.
            final int[] colours = {
                    0x000000, 0xFF0000, 0x00FF00, 0x8B4513,
                    0x0000FF, 0xA020F0, 0x00FFFF, 0xD3D3D3,
                    0x808080, 0xFF69B4, 0xBFFF00, 0xFFFF00,
                    0x9AC0CD, 0xFF00FF, 0xFF681F, 0xFFFFFF
            };
            return colours[MathHelper.clamp_int(meta, 0, colours.length - 1)];
        }

        @SideOnly(Side.CLIENT)
        private static void spawnSignWaxOnParticles(World world, int x, int y, int z) {
            Random random = world.rand;
            for (int i = 0; i < 10; i++) {
                double px = x + 0.15D + random.nextDouble() * 0.70D;
                double py = y + 0.20D + random.nextDouble() * 0.65D;
                double pz = z + 0.15D + random.nextDouble() * 0.70D;
                CustomParticles.spawnCopperWaxOnParticle(world, px, py, pz);
            }
        }

        private static void consumeOne(EntityPlayer player, ItemStack held) {
            if (player.capabilities.isCreativeMode || held == null) return;
            if (--held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }

        private boolean isIgnitionItem(ItemStack held) {
            return held != null && (held.getItem() == Items.flint_and_steel || held.getItem() == Items.fire_charge);
        }

        private String ignitionSound(ItemStack held) {
            return held != null && held.getItem() == Items.fire_charge ? "item.firecharge.use" : "item.flintandsteel.use";
        }

        private void consumeIgnitionItem(EntityPlayer player, ItemStack held) {
            if (player.capabilities.isCreativeMode || held == null) return;
            if (held.getItem() == Items.flint_and_steel) held.damageItem(1, player);
            else if (--held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }

        private void playModernSound(World world, int x, int y, int z, String sound, float volume, float pitch) {
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Tags.MC_ASSET_VER + ":" + sound, volume, pitch);
        }

        private void setMetadataAndRelight(World world, int x, int y, int z, int meta) {
            world.setBlockMetadataWithNotify(x, y, z, meta, 3);
            world.updateLightByType(EnumSkyBlock.Block, x, y, z);
        }

        @Override
        public int quantityDropped(int meta, int fortune, Random random) {
            if (isSegmentedGroundDecal()) return ((meta >> 2) & 3) + 1;
            if (isCandle() || isTurtleEgg()) return (meta & 3) + 1;
            return super.quantityDropped(meta, fortune, random);
        }

        @Override
        public int damageDropped(int meta) {
            return (isSegmentedGroundDecal() || isCandle() || isTurtleEgg() || isCampfire() || isScaffolding()
                    || isFroglight() || isCopperChain() || isCopperLantern() || isShelf() || isChiseledBookshelf())
                    ? 0 : super.damageDropped(meta);
        }

        @Override
        public int getDamageValue(World world, int x, int y, int z) {
            return (isSegmentedGroundDecal() || isCandle() || isTurtleEgg() || isCampfire() || isScaffolding()
                    || isFroglight() || isCopperChain() || isCopperLantern() || isShelf() || isChiseledBookshelf())
                    ? 0 : super.getDamageValue(world, x, y, z);
        }

        @Override
        public boolean hasTileEntity(int metadata) {
            return isSign() || isHangingSign() || isCampfire() || isChiseledBookshelf();
        }

        @Override
        public TileEntity createTileEntity(World world, int metadata) {
            if (isSign() || isHangingSign()) return new ParitySignTileEntity();
            if (isCampfire()) return new ParityCampfireTileEntity();
            if (isChiseledBookshelf()) return new ParityChiseledBookshelfTileEntity();
            return null;
        }

        @Override
        public int getLightValue(IBlockAccess world, int x, int y, int z) {
            int meta = world.getBlockMetadata(x, y, z) & 15;
            if (isCandle()) return (meta & 4) != 0 ? ((meta & 3) + 1) * 3 : 0;
            if (isCandleCake()) return (meta & 1) != 0 ? 3 : 0;
            if (isCampfire()) return (meta & 4) != 0 ? (isSoulCampfire() ? 10 : 15) : 0;
            return super.getLightValue(world, x, y, z);
        }

        private int computeScaffoldingMeta(IBlockAccess world, int x, int y, int z) {
            int distance = 7;
            Block below = y > 0 ? world.getBlock(x, y - 1, z) : null;
            if (below == this) {
                distance = world.getBlockMetadata(x, y - 1, z) & 7;
            } else if (below != null && below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP)) {
                distance = 0;
            }
            if (distance != 0) {
                final int[][] offsets = {{-1,0},{1,0},{0,-1},{0,1}};
                for (int[] offset : offsets) {
                    if (world.getBlock(x + offset[0], y, z + offset[1]) != this) continue;
                    distance = Math.min(distance, (world.getBlockMetadata(x + offset[0], y, z + offset[1]) & 7) + 1);
                    if (distance == 1) break;
                }
            }
            distance = Math.min(distance, 7);
            // Modern BOTTOM is not "there is no scaffold below". It marks the lower frame used
            // by a horizontally-supported/floating scaffold. A scaffold directly on solid ground
            // has distance 0 and MUST use the normal stable model.
            boolean bottom = distance > 0 && below != this;
            return distance | (bottom ? 8 : 0);
        }

        private static ForgeDirection horizontalFacing(EntityPlayer player) {
            int facing = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
            return switch (facing) {
                case 0 -> ForgeDirection.SOUTH;
                case 1 -> ForgeDirection.WEST;
                case 2 -> ForgeDirection.NORTH;
                default -> ForgeDirection.EAST;
            };
        }

        @Override
        public boolean canPlaceBlockAt(World world, int x, int y, int z) {
            if (isCopperLantern()) {
                return canCopperLanternStand(world, x, y, z) || canCopperLanternHang(world, x, y, z);
            }
            if (isFenceGate()) {
                return world.getBlock(x, y - 1, z).getMaterial().isSolid() && super.canPlaceBlockAt(world, x, y, z);
            }
            if (!isScaffolding()) return true;
            return (computeScaffoldingMeta(world, x, y, z) & 7) < 7;
        }

        @Override
        public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
            if (isFenceGate()) return (world.getBlockMetadata(x, y, z) & 4) != 0;
            return super.getBlocksMovement(world, x, y, z);
        }

        @Override
        public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity) {
            // Scaffolding cannot use 1.7's ladder flag: vanilla ladder motion only climbs while
            // horizontally colliding with a solid face and also prevents sneaking descent. Modern
            // scaffolding has open sides, so its vertical motion is handled explicitly below.
            return !isScaffolding() && super.isLadder(world, x, y, z, entity);
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
            if (isScaffolding()) {
                world.scheduleBlockUpdate(x, y, z, this, 1);
            } else if (isCopperLantern()) {
                boolean hanging = (world.getBlockMetadata(x, y, z) & 1) != 0;
                if ((hanging && !canCopperLanternHang(world, x, y, z))
                        || (!hanging && !canCopperLanternStand(world, x, y, z))) {
                    if (!world.isRemote) dropBlockAsItem(world, x, y, z, 0, 0);
                    world.setBlockToAir(x, y, z);
                    return;
                }
            } else if (isShelf() && !world.isRemote) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                int next = (meta & 3) | (world.isBlockIndirectlyGettingPowered(x, y, z) ? 4 : 0);
                if (meta != next) world.setBlockMetadataWithNotify(x, y, z, next, 3);
                world.markBlockForUpdate(x, y, z);
            } else if (isFenceGate() && !world.isRemote) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                boolean powered = world.isBlockIndirectlyGettingPowered(x, y, z);
                if (powered && (meta & 4) == 0) {
                    world.setBlockMetadataWithNotify(x, y, z, meta | 4, 2);
                    world.playAuxSFXAtEntity(null, 1003, x, y, z, 0);
                } else if (!powered && (meta & 4) != 0 && neighbor != null && neighbor.canProvidePower()) {
                    world.setBlockMetadataWithNotify(x, y, z, meta & ~4, 2);
                    world.playAuxSFXAtEntity(null, 1003, x, y, z, 0);
                }
            }
            super.onNeighborBlockChange(world, x, y, z, neighbor);
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            if (isScaffolding()) {
                int next = computeScaffoldingMeta(world, x, y, z);
                if ((next & 7) >= 7) {
                    dropBlockAsItem(world, x, y, z, 0, 0);
                    world.setBlockToAir(x, y, z);
                } else if ((world.getBlockMetadata(x, y, z) & 15) != next) {
                    world.setBlockMetadataWithNotify(x, y, z, next, 3);
                }
                return;
            }
            if (isCampfire() && (world.getBlockMetadata(x, y, z) & 4) != 0
                    && world.canLightningStrikeAt(x, y + 1, z)) {
                setMetadataAndRelight(world, x, y, z, world.getBlockMetadata(x, y, z) & 3);
                playModernSound(world, x, y, z, "block.fire.extinguish", 1.0F, 1.0F);
                return;
            }
            super.updateTick(world, x, y, z, random);
        }

        @Override
        public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
            if (isScaffolding()) {
                entity.fallDistance = 0.0F;
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (player.isSneaking()) {
                        // The top collision is removed for sneaking players, so a small controlled
                        // downward velocity gives the modern "hold sneak to descend" behaviour.
                        entity.motionY = -0.15D;
                    } else if (EtFuturum.proxy.isScaffoldingJumpHeld(player)) {
                        // Sustain ascent only while the local jump input is actually held. Using
                        // positive motionY as a proxy made one jump tap self-sustain indefinitely.
                        entity.motionY = 0.18D;
                    } else if (entity.motionY < -0.15D) {
                        entity.motionY = -0.15D;
                    }
                } else if (entity.motionY < -0.15D) {
                    entity.motionY = -0.15D;
                }
            }
            if (isCampfire() && (world.getBlockMetadata(x, y, z) & 4) != 0 && !entity.isImmuneToFire()) {
                entity.attackEntityFrom(DamageSource.inFire, isSoulCampfire() ? 2.0F : 1.0F);
            }
            super.onEntityCollidedWithBlock(world, x, y, z, entity);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(World world, int x, int y, int z, Random random) {
            int meta = world.getBlockMetadata(x, y, z) & 15;
            if (isCandle() && (meta & 4) != 0) {
                spawnCandleFlames(world, x, y, z, (meta & 3) + 1, random);
            } else if (isCandleCake() && (meta & 1) != 0) {
                // The cake candle is centered on the cake; its wick sits just above the frosting.
                spawnSmallFlame(world, x + 0.5D, y + 0.94D, z + 0.5D, random);
            }
            if (isCampfire() && (meta & 4) != 0) {
                boolean signal = world.getBlock(x, y - 1, z) == Blocks.hay_block;
                int count = signal ? 3 : 1;
                for (int i = 0; i < count; i++) {
                    double px = x + 0.5D + (random.nextDouble() - 0.5D) * 0.3D;
                    double py = y + (signal ? 1.25D : 0.55D) + random.nextDouble() * 0.15D;
                    double pz = z + 0.5D + (random.nextDouble() - 0.5D) * 0.3D;
                    world.spawnParticle("largesmoke", px, py, pz, 0.0D, signal ? 0.08D : 0.03D, 0.0D);
                }
                if (random.nextInt(10) == 0) {
                    world.playSound(x + 0.5F, y + 0.5F, z + 0.5F,
                            Tags.MC_ASSET_VER + ":block.campfire.crackle", 0.5F + random.nextFloat() * 0.5F,
                            0.6F + random.nextFloat() * 0.7F, false);
                }
            }
            super.randomDisplayTick(world, x, y, z, random);
        }

        @SideOnly(Side.CLIENT)
        private void spawnCandleFlames(World world, int x, int y, int z, int count, Random random) {
            // Exact wick centers/heights from Mojang's template_{one,two,three,four}_candles models.
            final double[][] one = {{8, 7, 8}};
            final double[][] two = {{6, 6, 8}, {10, 7, 7}};
            final double[][] three = {{8, 4, 10}, {6, 6, 8}, {9, 7, 7}};
            final double[][] four = {{7, 4, 9}, {10, 6, 9}, {6, 6, 6}, {9, 7, 6}};
            double[][] positions = count <= 1 ? one : count == 2 ? two : count == 3 ? three : four;
            for (double[] wick : positions) {
                spawnSmallFlame(world, x + wick[0] / 16.0D, y + wick[1] / 16.0D + 0.025D,
                        z + wick[2] / 16.0D, random);
            }
        }

        @SideOnly(Side.CLIENT)
        private void spawnSmallFlame(World world, double x, double y, double z, Random random) {
            // The stock 1.7 flame particle is much larger than a modern candle flame. Use the
            // same flame atlas cell/animation at exactly half the legacy base scale.
            Minecraft.getMinecraft().effectRenderer.addEffect(new CandleFlameFX(world, x, y, z));
            if (random.nextInt(24) == 0) world.spawnParticle("smoke", x, y + 0.02D, z, 0.0D, 0.01D, 0.0D);
        }

        @SideOnly(Side.CLIENT)
        private static final class CandleFlameFX extends EntityFX {
            private final float baseScale;

            CandleFlameFX(World world, double x, double y, double z) {
                super(world, x, y, z, 0.0D, 0.0D, 0.0D);
                motionX *= 0.01D;
                motionY *= 0.01D;
                motionZ *= 0.01D;
                baseScale = particleScale * 0.50F;
                particleRed = particleGreen = particleBlue = 1.0F;
                particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
                noClip = true;
                setParticleTextureIndex(48);
            }

            @Override
            public void renderParticle(Tessellator tessellator, float partialTicks, float rotationX,
                    float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
                float age = ((float) particleAge + partialTicks) / (float) particleMaxAge;
                particleScale = baseScale * (1.0F - age * age * 0.5F);
                super.renderParticle(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            }

            @Override
            public int getBrightnessForRender(float partialTicks) {
                float age = ((float) particleAge + partialTicks) / (float) particleMaxAge;
                age = Math.max(0.0F, Math.min(1.0F, age));
                int packed = super.getBrightnessForRender(partialTicks);
                int low = packed & 255;
                int high = packed >> 16 & 255;
                low = Math.min(240, low + (int) (age * 15.0F * 16.0F));
                return low | high << 16;
            }

            @Override
            public float getBrightness(float partialTicks) {
                float age = ((float) particleAge + partialTicks) / (float) particleMaxAge;
                age = Math.max(0.0F, Math.min(1.0F, age));
                float normal = super.getBrightness(partialTicks);
                return normal * age + (1.0F - age);
            }

            @Override
            public void onUpdate() {
                prevPosX = posX;
                prevPosY = posY;
                prevPosZ = posZ;
                if (particleAge++ >= particleMaxAge) setDead();
                moveEntity(motionX, motionY, motionZ);
                motionX *= 0.96D;
                motionY *= 0.96D;
                motionZ *= 0.96D;
                if (onGround) {
                    motionX *= 0.70D;
                    motionZ *= 0.70D;
                }
            }
        }

        @Override
        public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
            if (isCampfire()) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityCampfireTileEntity) ((ParityCampfireTileEntity) tile).ejectAll();
            }
            if (isChiseledBookshelf() && !world.isRemote) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityChiseledBookshelfTileEntity) {
                    ParityChiseledBookshelfTileEntity shelf = (ParityChiseledBookshelfTileEntity) tile;
                    for (int slot = 0; slot < shelf.getSizeInventory(); slot++) {
                        ItemStack stored = shelf.getStackInSlot(slot);
                        if (stored != null) {
                            world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stored.copy()));
                        }
                    }
                }
            }
            super.breakBlock(world, x, y, z, block, meta);
        }

        @Override
        public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
            if (entry == POTTED_TORCHFLOWER) {
                ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                drops.add(new ItemStack(Item.getItemFromBlock(Blocks.flower_pot)));
                if (TORCHFLOWER.get() != null) drops.add(new ItemStack(Item.getItemFromBlock(TORCHFLOWER.get())));
                return drops;
            }
            return super.getDrops(world, x, y, z, metadata, fortune);
        }

        @Override public int getRenderType() { return RenderIDs.MODERN_MAP_PARITY; }
        @Override @SideOnly(Side.CLIENT)
        public int getRenderBlockPass() {
            // Leaf Litter/Wildflowers use binary cutout transparency (0 or 255 alpha), not true
            // translucency. Keep them in legacy pass 0 so the normal alpha-test path discards the
            // transparent texels after the resource bridge has repaired their colour-key alpha.
            return 0;
        }
        @Override public boolean renderAsNormalBlock() { return false; }
        @Override public boolean isOpaqueCube() {
            // Vanilla Block() calls this before the subclass constructor assigns entry.
            return entry != null && isFullOpaqueModel();
        }
        @Override
        public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask,
                List<AxisAlignedBB> list, Entity collider) {
            if (entry.style == Style.PLANT || entry.style == Style.WALL_PLANT || entry.style == Style.VINE
                    || isSegmentedGroundDecal()) return;
            if (isScaffolding()) {
                boolean descending = collider instanceof EntityPlayer && ((EntityPlayer) collider).isSneaking();
                if (descending || collider == null || collider.boundingBox == null) return;

                int scaffoldMeta = world.getBlockMetadata(x, y, z) & 15;
                int distance = scaffoldMeta & 7;
                boolean bottom = (scaffoldMeta & 8) != 0;
                double feet = collider.boundingBox.minY;

                // Modern scaffolding only supplies its upper collision once the entity is already
                // above the full block. Pass 11 used y+14/16 as the context threshold, which made
                // a climber collide with the top while still inside it and caused the visible
                // last-moment upward pop. Keep the physical plate at 14/16, but do not enable it
                // until the entity's feet have actually cleared y+1.
                if (feet >= y + 1.0D - 1.0E-5D) {
                    addCollisionBox(mask, list, x, y, z, 0.0F, 14.0F/16.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                }

                // A horizontally-supported BOTTOM scaffold has the extra 2px lower frame. Modern
                // collision uses that frame to stop entities trying to jump into an overhang from
                // underneath, while remaining non-solid when approached from inside/sideways.
                if (distance > 0 && bottom && feet >= y + 2.0D / 16.0D - 1.0E-5D) {
                    // One-way lower frame: stand on it from above, but never collide with it while
                    // rising through the scaffold from underneath. This removes the underside jitter.
                    addCollisionBox(mask, list, x, y, z, 0.0F, 0.0F, 0.0F, 1.0F, 2.0F/16.0F, 1.0F);
                }
                return;
            }
            if (isFenceGate()) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                if ((meta & 4) != 0) return;
                int direction = meta & 3;
                if (direction == 1 || direction == 3) {
                    addCollisionBox(mask, list, x, y, z, 0.375F, 0.0F, 0.0F, 0.625F, 1.5F, 1.0F);
                } else {
                    addCollisionBox(mask, list, x, y, z, 0.0F, 0.0F, 0.375F, 1.0F, 1.5F, 0.625F);
                }
                return;
            }
            if (isShelf()) {
                addShelfCollisionBoxes(world.getBlockMetadata(x, y, z) & 3, mask, list, x, y, z);
                return;
            }
            if (entry.style != Style.PANE && entry.style != Style.FENCE && entry.style != Style.WALL) {
                setBlockBoundsBasedOnState(world, x, y, z);
                super.addCollisionBoxesToList(world, x, y, z, mask, list, collider);
                return;
            }

            boolean n = connectsTo(world, x, y, z - 1, entry.style);
            boolean s = connectsTo(world, x, y, z + 1, entry.style);
            boolean w = connectsTo(world, x - 1, y, z, entry.style);
            boolean e = connectsTo(world, x + 1, y, z, entry.style);
            float min, max, collisionHeight;
            if (entry.style == Style.PANE) {
                min = 0.4375F; max = 0.5625F; collisionHeight = 1.0F;
            } else if (entry.style == Style.FENCE) {
                min = 0.375F; max = 0.625F; collisionHeight = 1.5F;
            } else {
                min = 0.25F; max = 0.75F; collisionHeight = 1.5F;
            }

            // Use independent center/arm collision prisms rather than one union AABB. This keeps
            // the empty corners of L/T/cross connections traversable, matching pane/fence/wall
            // collision semantics and fixing Copper Bars occupying invisible space.
            addCollisionBox(mask, list, x, y, z, min, 0.0F, min, max, collisionHeight, max);
            if (n) addCollisionBox(mask, list, x, y, z, min, 0.0F, 0.0F, max, collisionHeight, min);
            if (s) addCollisionBox(mask, list, x, y, z, min, 0.0F, max, max, collisionHeight, 1.0F);
            if (w) addCollisionBox(mask, list, x, y, z, 0.0F, 0.0F, min, min, collisionHeight, max);
            if (e) addCollisionBox(mask, list, x, y, z, max, 0.0F, min, 1.0F, collisionHeight, max);
        }

        private static void addShelfCollisionBoxes(int facing, AxisAlignedBB mask, List<AxisAlignedBB> list,
                int x, int y, int z) {
            final float three = 3.0F / 16.0F;
            final float five = 5.0F / 16.0F;
            final float eleven = 11.0F / 16.0F;
            final float thirteen = 13.0F / 16.0F;
            final float four = 4.0F / 16.0F;
            final float twelve = 12.0F / 16.0F;
            switch (facing) {
                case 1: // east: rear body on west, lips extending eastward
                    addCollisionBox(mask, list, x, y, z, 0, 0, 0, three, 1, 1);
                    addCollisionBox(mask, list, x, y, z, three, 0, 0, five, four, 1);
                    addCollisionBox(mask, list, x, y, z, three, twelve, 0, five, 1, 1);
                    break;
                case 2: // south
                    addCollisionBox(mask, list, x, y, z, 0, 0, 0, 1, 1, three);
                    addCollisionBox(mask, list, x, y, z, 0, 0, three, 1, four, five);
                    addCollisionBox(mask, list, x, y, z, 0, twelve, three, 1, 1, five);
                    break;
                case 3: // west
                    addCollisionBox(mask, list, x, y, z, thirteen, 0, 0, 1, 1, 1);
                    addCollisionBox(mask, list, x, y, z, eleven, 0, 0, thirteen, four, 1);
                    addCollisionBox(mask, list, x, y, z, eleven, twelve, 0, thirteen, 1, 1);
                    break;
                default: // north
                    addCollisionBox(mask, list, x, y, z, 0, 0, thirteen, 1, 1, 1);
                    addCollisionBox(mask, list, x, y, z, 0, 0, eleven, 1, four, thirteen);
                    addCollisionBox(mask, list, x, y, z, 0, twelve, eleven, 1, 1, thirteen);
                    break;
            }
        }

        private static void addCollisionBox(AxisAlignedBB mask, List<AxisAlignedBB> list, int x, int y, int z,
                float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                    x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
            if (mask == null || box.intersectsWith(mask)) list.add(box);
        }

        @Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
            if (isSegmentedGroundDecal() || isScaffolding()) return null;
            if (isFenceGate()) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                if ((meta & 4) != 0) return null;
                int direction = meta & 3;
                if (direction == 1 || direction == 3) {
                    return AxisAlignedBB.getBoundingBox(x + 0.375D, y, z, x + 0.625D, y + 1.5D, z + 1.0D);
                }
                return AxisAlignedBB.getBoundingBox(x, y, z + 0.375D, x + 1.0D, y + 1.5D, z + 0.625D);
            }
            if (isHangingSign()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                boolean wall = isWallSignIdentity() || (!isWallSignIdentity() && meta >= 8);
                if (wall) {
                    // Modern wall hanging signs collide only with their upper mounting plank; the
                    // wooden board is selectable but walk-through. Rotate that plank with FACING.
                    int facing = isWallSignIdentity() ? normaliseHorizontalSide(meta) : 2 + (meta - 8);
                    if (facing == 4 || facing == 5) {
                        return AxisAlignedBB.getBoundingBox(x + 6.0D / 16.0D, y + 14.0D / 16.0D, z,
                                x + 10.0D / 16.0D, y + 1.0D, z + 1.0D);
                    }
                    return AxisAlignedBB.getBoundingBox(x, y + 14.0D / 16.0D, z + 6.0D / 16.0D,
                            x + 1.0D, y + 1.0D, z + 10.0D / 16.0D);
                }
            }
            switch (entry.style) {
                case PLANT: case WALL_PLANT: case VINE: return null;
                default:
                    setBlockBoundsBasedOnState(world, x, y, z);
                    return super.getCollisionBoundingBoxFromPool(world,x,y,z);
            }
        }
        @Override public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
            setBlockBoundsBasedOnState(world, x, y, z);
            return super.getSelectedBoundingBoxFromPool(world, x, y, z);
        }
        @Override @SideOnly(Side.CLIENT)
        public IIcon getIcon(int side, int meta) {
            if ((entry == PALE_OAK_FENCE || entry == PALE_OAK_FENCE_GATE) && PALE_OAK_PLANKS.get() != null) {
                IIcon planks = PALE_OAK_PLANKS.get().getIcon(side, 0);
                if (planks != null) return planks;
            }
            return super.getIcon(side, meta);
        }
        @Override @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            // Use stone only during the bootstrap stitch. Once AssetDirector has resolved
            // the 1.21.11 model, publish one of that model's own atlas icons as the 1.7
            // fallback/particle icon instead of leaving a misleading placeholder behind.
            blockIcon = reg.registerIcon("minecraft:stone");
            ModernJsonModelBridge.PreparedModels prepared = ModernJsonModelBridge.prepare(entry, reg);
            IIcon fallback = ModernJsonModelBridge.getFallbackIcon(prepared);
            if (fallback != null) blockIcon = fallback;
            // Pale Oak fences/gates should expose the same atlas fallback as Pale Oak Planks,
            // matching the mature EFR/vanilla fence families. JSON model quads remain authoritative.
            if ((entry == PALE_OAK_FENCE || entry == PALE_OAK_FENCE_GATE) && PALE_OAK_PLANKS.get() != null) {
                IIcon planks = PALE_OAK_PLANKS.get().getIcon(0, 0);
                if (planks != null) blockIcon = planks;
            }
        }
    }

}
