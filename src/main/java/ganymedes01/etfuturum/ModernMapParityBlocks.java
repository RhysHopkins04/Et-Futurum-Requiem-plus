package ganymedes01.etfuturum;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.blocks.BaseDoor;
import ganymedes01.etfuturum.blocks.BaseSlab;
import ganymedes01.etfuturum.blocks.BaseStairs;
import ganymedes01.etfuturum.blocks.BaseTrapdoor;
import ganymedes01.etfuturum.blocks.IDegradable;
import ganymedes01.etfuturum.blocks.ModernWallState;
import ganymedes01.etfuturum.blocks.itemblocks.BaseSlabItemBlock;
import ganymedes01.etfuturum.blocks.itemblocks.ItemBlockNewDoor;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.core.utils.IChestPairingState;
import ganymedes01.etfuturum.core.utils.ModernChestPairing;
import ganymedes01.etfuturum.network.WoodSignOpenMessage;
import ganymedes01.etfuturum.recipes.crafting.RecipeDecoratedPot;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockVine;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.IconFlipped;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.Container;
import net.minecraft.item.crafting.CraftingManager;
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
import net.minecraft.stats.StatList;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.oredict.RecipeSorter;

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
    PALE_OAK_PRESSURE_PLATE("1.21.4", Style.PRESSURE_PLATE, Material.wood, "pale_oak_planks", "textures/block/pale_oak_planks.png", 0),
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
    COPPER_WALL_TORCH("1.21.9", Style.TORCH, Material.plants, "copper_torch", "textures/block/copper_torch.png", 14),
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
        LEAVES, LOG, SLAB, STAIRS, WALL, FENCE, FENCE_GATE, DOOR, TRAPDOOR, BUTTON, PRESSURE_PLATE,
        SIGN, HANGING_SIGN, PANE, TORCH, SHELF
    }

    private final String vanillaVersion;
    private final Style style;
    private final Material material;
    private final String textureKey;
    private final String modernAssetPath;
    private final int lightLevel;
    private Block block;
    private Block doubleSlabBlock;
    private static boolean parityChestTileRegistered;
    private static boolean paritySignTileRegistered;
    private static boolean parityCampfireTileRegistered;
    private static boolean parityChiseledBookshelfTileRegistered;
    private static boolean parityDecoratedPotTileRegistered;
    private static boolean parityShelfTileRegistered;
    private static boolean parityBrushableTileRegistered;
    private static boolean parityMultifaceTileRegistered;
    private static boolean parityButtonTileRegistered;
    private static boolean parityPaleMossCarpetTileRegistered;
    private static boolean parityVaultStateTileRegistered;
    private static boolean parityCrafterStateTileRegistered;
    private static boolean decoratedPotRecipeRegistered;

    /** Pass 30 bit order: DOWN, UP, NORTH, SOUTH, WEST, EAST (ForgeDirection ordinals 0..5). */
    public static final int MULTIFACE_ALL_FACES = 0x3F;
    public static final String MULTIFACE_FACE_MASK_TAG = "FaceMask";
    private static final String[] MULTIFACE_FACE_NAMES = {"down", "up", "north", "south", "west", "east"};

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

    /** Pass 30 deliberately enables shared multiface state for these two identities only. */
    public boolean usesMultifaceState() {
        return this == SCULK_VEIN || this == RESIN_CLUMP;
    }

    /** Converts the six modern boolean properties into the stable Pass 30 FaceMask format. */
    public static int encodeMultifaceProperties(Map<String, String> properties) {
        int mask = 0;
        if (properties == null) return mask;
        for (int face = 0; face < MULTIFACE_FACE_NAMES.length; face++) {
            if (Boolean.parseBoolean(properties.get(MULTIFACE_FACE_NAMES[face]))) mask |= 1 << face;
        }
        return mask & MULTIFACE_ALL_FACES;
    }

    /** Reads synchronized multiface state, with a safe floor-face fallback during TE bootstrap. */
    public static int getMultifaceFaceMask(IBlockAccess world, int x, int y, int z) {
        if (world != null) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof ParityMultifaceTileEntity) {
                return ((ParityMultifaceTileEntity) tile).getFaceMask();
            }
        }
        return 1 << ForgeDirection.DOWN.ordinal();
    }

    /** Stable Pass 34 Pale Moss Carpet code: ((((bottom * 3 + north) * 3 + east) * 3 + south) * 3 + west). */
    public static int paleMossStateIndex(boolean bottom, int north, int east, int south, int west) {
        north = clampPaleMossSide(north);
        east = clampPaleMossSide(east);
        south = clampPaleMossSide(south);
        west = clampPaleMossSide(west);
        return ((((bottom ? 1 : 0) * 3 + north) * 3 + east) * 3 + south) * 3 + west;
    }

    private static int clampPaleMossSide(int value) {
        return value < 0 ? 0 : value > 2 ? 2 : value;
    }

    public static int getPaleMossStateIndex(IBlockAccess world, int x, int y, int z) {
        if (world != null) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof ParityPaleMossCarpetTileEntity) {
                return ((ParityPaleMossCarpetTileEntity) tile).getStateIndex();
            }
        }
        return paleMossStateIndex(true, 0, 0, 0, 0);
    }

    /** Pass 35 Vault visible-state index stored in a tiny synchronized TE. */
    public static int getVaultState(IBlockAccess world, int x, int y, int z) {
        if (world != null) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof ParityVaultStateTileEntity) return ((ParityVaultStateTileEntity) tile).getVaultState();
        }
        return 0;
    }

    /** Pass 35 Crafter visual booleans: bit 0=Triggered, bit 1=Crafting. */
    public static int getCrafterVisualFlags(IBlockAccess world, int x, int y, int z) {
        if (world != null) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof ParityCrafterStateTileEntity) return ((ParityCrafterStateTileEntity) tile).getVisualFlags();
        }
        return 0;
    }

    public boolean isCopperGolemStatueIdentity() {
        return getRegistryName().endsWith("copper_golem_statue");
    }

    /** Pass 36 families participating in the shared IDegradable copper lifecycle. */
    public boolean isPass36CopperLifecycleIdentity() {
        String family = copperLifecycleFamily(getRegistryName());
        return "copper_chest".equals(family) || "copper_golem_statue".equals(family)
                || "copper_bars".equals(family) || "copper_chain".equals(family)
                || "copper_lantern".equals(family) || "lightning_rod".equals(family);
    }

    public boolean isWaxedCopperLifecycleIdentity() {
        return isPass36CopperLifecycleIdentity() && getRegistryName().startsWith("waxed_");
    }

    public int getCopperLifecycleStage() {
        String name = getRegistryName();
        if (name.startsWith("waxed_")) name = name.substring(6);
        if (name.startsWith("exposed_")) return 1;
        if (name.startsWith("weathered_")) return 2;
        if (name.startsWith("oxidized_")) return 3;
        return 0;
    }

    private static String copperLifecycleFamily(String name) {
        if (name == null) return "";
        if (name.startsWith("waxed_")) name = name.substring(6);
        if (name.startsWith("exposed_")) name = name.substring(8);
        else if (name.startsWith("weathered_")) name = name.substring(10);
        else if (name.startsWith("oxidized_")) name = name.substring(9);
        return name;
    }

    /** Synthetic IDegradable metadata for Pass-36 registry-identity copper families. */
    public static int getPass36CopperMeta(Block block) {
        if (block == null) return -1;
        if (ModBlocks.LIGHTNING_ROD.isEnabled() && block == ModBlocks.LIGHTNING_ROD.get()) return 0;
        ModernMapParityBlocks entry = fromBlock(block);
        if (entry == null || !entry.isPass36CopperLifecycleIdentity()) return -1;
        return entry.getCopperLifecycleStage() + (entry.isWaxedCopperLifecycleIdentity() ? 8 : 0);
    }

    /** Resolves an oxidation/wax transition while keeping every family's existing metadata untouched. */
    public static Block getPass36CopperBlock(Block source, int copperMeta) {
        if (source == null) return null;
        String family;
        if (ModBlocks.LIGHTNING_ROD.isEnabled() && source == ModBlocks.LIGHTNING_ROD.get()) {
            family = "lightning_rod";
        } else {
            ModernMapParityBlocks sourceEntry = fromBlock(source);
            if (sourceEntry == null || !sourceEntry.isPass36CopperLifecycleIdentity()) return null;
            family = copperLifecycleFamily(sourceEntry.getRegistryName());
        }
        int stage = copperMeta & 3;
        boolean waxed = (copperMeta & 8) != 0;
        if ("lightning_rod".equals(family) && stage == 0 && !waxed) {
            return ModBlocks.LIGHTNING_ROD.isEnabled() ? ModBlocks.LIGHTNING_ROD.get() : null;
        }
        String stagePrefix = stage == 1 ? "exposed_" : stage == 2 ? "weathered_" : stage == 3 ? "oxidized_" : "";
        String targetName = (waxed ? "waxed_" : "") + stagePrefix + family;
        for (ModernMapParityBlocks candidate : values()) {
            if (candidate.getRegistryName().equals(targetName)) return candidate.get();
        }
        return null;
    }

    /** Pass 36b: cycles Statue pose while preserving the lower two facing bits exactly. */
    public static int cycleCopperGolemStatuePoseMeta(int metadata) {
        int state = metadata & 15;
        int facing = state & 3;
        int pose = (state >> 2) & 3;
        return (((pose + 1) & 3) << 2) | facing;
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
        if (!parityDecoratedPotTileRegistered) {
            GameRegistry.registerTileEntity(ParityDecoratedPotTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_decorated_pot");
            parityDecoratedPotTileRegistered = true;
        }
        if (!parityShelfTileRegistered) {
            GameRegistry.registerTileEntity(ParityShelfTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_shelf");
            parityShelfTileRegistered = true;
        }
        if (!parityBrushableTileRegistered) {
            GameRegistry.registerTileEntity(ParityBrushableTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_brushable");
            parityBrushableTileRegistered = true;
        }
        if (!parityMultifaceTileRegistered) {
            GameRegistry.registerTileEntity(ParityMultifaceTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_multiface");
            parityMultifaceTileRegistered = true;
        }
        if (!parityButtonTileRegistered) {
            GameRegistry.registerTileEntity(ParityButtonTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_button");
            parityButtonTileRegistered = true;
        }
        if (!parityPaleMossCarpetTileRegistered) {
            GameRegistry.registerTileEntity(ParityPaleMossCarpetTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_pale_moss_carpet");
            parityPaleMossCarpetTileRegistered = true;
        }
        if (!parityVaultStateTileRegistered) {
            GameRegistry.registerTileEntity(ParityVaultStateTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_vault_state");
            parityVaultStateTileRegistered = true;
        }
        if (!parityCrafterStateTileRegistered) {
            GameRegistry.registerTileEntity(ParityCrafterStateTileEntity.class,
                    Tags.MOD_ID + ":modern_parity_crafter_state");
            parityCrafterStateTileRegistered = true;
        }
        ModernPotterySherds.init();
        ModernArchaeology.init();
        for (ModernMapParityBlocks entry : values()) {
            String name = entry.getRegistryName();
            // The mature Mangrove Propagule lives in BlockModernSapling metadata 0, but Pass 33
            // already exposed etfuturum:mangrove_propagule as a registry identity. Keep that old
            // identity registered as a hidden compatibility alias so dedicated-server handshakes
            // and worlds created before Pass 34 never become fatally missing. New Backporter data
            // still targets etfuturum:sapling metadata 0 + its synchronized visual-state TE.
            boolean legacyMangroveAlias = entry == MANGROVE_PROPAGULE
                    && ConfigBlocksItems.enableMangroveWoodFamily && ModBlocks.SAPLING.isEnabled();
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
                    || "potted_torchflower".equals(name) || "copper_wall_torch".equals(name);
            if (!technicalPlacementBlock && !legacyMangroveAlias) entry.block.setCreativeTab(EtFuturum.creativeTabBlocks);
            if (entry.lightLevel > 0 && !entry.usesDynamicLight()) entry.block.setLightLevel(entry.lightLevel / 15.0F);
            if (entry.block instanceof BaseSlab) {
                GameRegistry.registerBlock(entry.block, BaseSlabItemBlock.class, name);
                GameRegistry.registerBlock(entry.doubleSlabBlock, BaseSlabItemBlock.class, "double_" + name);
            } else if (entry.block instanceof BaseDoor) {
                GameRegistry.registerBlock(entry.block,
                        entry == PALE_OAK_DOOR ? ParityDoorItemBlock.class : ItemBlockNewDoor.class, name);
            } else if (name.endsWith("_coral_wall_fan") || "copper_wall_torch".equals(name)) GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name);
            else if ("potted_torchflower".equals(name)) {
                GameRegistry.registerBlock(entry.block, (Class<? extends ItemBlock>) null, name);
            } else if ("torchflower".equals(name)) {
                GameRegistry.registerBlock(entry.block, ParityTorchflowerItemBlock.class, name);
            } else if (entry == PALE_HANGING_MOSS) {
                GameRegistry.registerBlock(entry.block, ParityPaleHangingMossItemBlock.class, name);
            } else if (entry.isCopperGolemStatueIdentity()) {
                GameRegistry.registerBlock(entry.block, ParityStatefulItemBlock.class, name);
            } else if (entry.usesMultifaceState()) {
                GameRegistry.registerBlock(entry.block, ParityMultifaceItemBlock.class, name);
            } else {
                GameRegistry.registerBlock(entry.block, name);
            }
        }
        if (!decoratedPotRecipeRegistered && DECORATED_POT.get() != null) {
            RecipeSorter.register(Tags.MOD_ID + ".RecipeDecoratedPot", RecipeDecoratedPot.class,
                    RecipeSorter.Category.SHAPED, "after:minecraft:shaped");
            CraftingManager.getInstance().getRecipeList().add(new RecipeDecoratedPot());
            decoratedPotRecipeRegistered = true;
        }
    }

    private boolean usesDynamicLight() {
        String name = getRegistryName();
        return style == Style.CANDLE || style == Style.CANDLE_CAKE
                || "campfire".equals(name) || "soul_campfire".equals(name) || "sea_pickle".equals(name)
                || "respawn_anchor".equals(name) || "trial_spawner".equals(name) || "vault".equals(name);
        // Sculk Sensor / Calibrated Sculk Sensor intentionally stay static at light level 1:
        // Minecraft Java 1.21.11 only makes their emissive-rendering predicate ACTIVE-phase-specific.
    }

    private Block createBlock() {
        if (getRegistryName().endsWith("copper_chest")) return new ParityCopperChestBlock(this);
        if (isPass36CopperLifecycleIdentity()) return new ParityCopperLifecycleModelBlock(this);
        if (style == Style.STAIRS) {
            Block base = this == PALE_OAK_STAIRS ? PALE_OAK_PLANKS.get() : RESIN_BRICKS.get();
            return new ParityStairBlock(this, base);
        }
        if (style == Style.SLAB) {
            Block base = this == PALE_OAK_SLAB ? PALE_OAK_PLANKS.get() : RESIN_BRICKS.get();
            ParitySlabBlock single = new ParitySlabBlock(false, this, base);
            doubleSlabBlock = new ParitySlabBlock(true, this, base);
            return single;
        }
        if (this == PALE_OAK_DOOR) return new ParityDoorBlock(this);
        if (this == PALE_OAK_TRAPDOOR) return new ParityTrapdoorBlock(this);
        if (this == PALE_OAK_PRESSURE_PLATE) return new ParityPressurePlateBlock(this);
        return new ParityModelBlock(this);
    }

    public Block getDoubleSlab() {
        return doubleSlabBlock;
    }

    public static ModernMapParityBlocks fromBlock(Block block) {
        if (block == null) return null;
        for (ModernMapParityBlocks entry : values()) {
            if (entry.block == block) return entry;
            if (entry.doubleSlabBlock == block) return entry;
        }
        return null;
    }

    public static boolean isSuspiciousBlock(Block block) {
        return block != null && (block == SUSPICIOUS_SAND.get() || block == SUSPICIOUS_GRAVEL.get());
    }

    /** True only for EFR's eight dedicated Copper Chest block identities. */
    public static boolean isCopperChestBlock(Block block) {
        return block instanceof ParityCopperChestBlock;
    }

    /**
     * 1.21.11 Copper Chest placement/updateShape converges a paired chest to the least oxidized
     * identity; mixed wax state is first unwaxed. Keep the Pass-32 explicit pair direction while
     * applying that same family-level identity rule.
     */
    public static void normalizeCopperChestPairIdentity(World world, int x, int y, int z) {
        if (world == null || world.isRemote) return;
        Block firstBlock = world.getBlock(x, y, z);
        if (!(firstBlock instanceof ParityCopperChestBlock)) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IChestPairingState)) return;
        IChestPairingState pairing = (IChestPairingState) tile;
        pairing.etfu$resolvePairing();
        byte direction = pairing.etfu$getPairDirection();
        if (!ModernChestPairing.isPair(direction)) return;
        int px = x + ModernChestPairing.offsetX(direction);
        int pz = z + ModernChestPairing.offsetZ(direction);
        Block partnerBlock = world.getBlock(px, y, pz);
        if (!ModernChestPairing.areCompatibleChestBlocks(firstBlock, partnerBlock)
                || !(partnerBlock instanceof ParityCopperChestBlock)) return;

        int firstCopper = getPass36CopperMeta(firstBlock);
        int partnerCopper = getPass36CopperMeta(partnerBlock);
        if (firstCopper < 0 || partnerCopper < 0) return;
        boolean bothWaxed = firstCopper >= 8 && partnerCopper >= 8;
        boolean waxMismatch = (firstCopper >= 8) != (partnerCopper >= 8);
        int stage = Math.min(firstCopper & 3, partnerCopper & 3);
        int targetCopper = stage + (bothWaxed && !waxMismatch ? 8 : 0);
        Block target = getPass36CopperBlock(firstBlock, targetCopper);
        if (!(target instanceof ParityCopperChestBlock)) return;
        if (firstBlock == target && partnerBlock == target) return;
        ParityCopperChestBlock.transitionCopperChestPair(world, x, y, z,
                (ParityCopperChestBlock) target, world.getBlockMetadata(x, y, z));
    }

    /**
     * Kept only for compatibility with older validators/callers. Modern parity rendering no
     * longer guesses a single texture per block; its exact model texture dependencies are
     * discovered dynamically from the AssetDirector-provided 1.21.11 JSON model graph.
     */
    public static Map<String, String> getAssetAliases() {
        return Collections.emptyMap();
    }

    /** Standard slab placement/merging with the modern backing block's atlas texture. */
    private static final class ParityStairBlock extends BaseStairs {
        private final ModernMapParityBlocks entry;

        ParityStairBlock(ModernMapParityBlocks entry, Block backing) {
            super(backing, 0);
            this.entry = entry;
            setBlockName(Tags.MOD_ID + "." + entry.getRegistryName());
            setHardness(entry.material == Material.wood ? 2.0F : 1.5F);
            setResistance(5.0F);
            setStepSound(entry.material == Material.wood ? soundTypeWood : soundTypeStone);
        }

        @Override @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            super.registerBlockIcons(reg);
            ModernJsonModelBridge.prepare(entry, reg);
        }
    }

    /** Standard slab placement/merging with the modern backing block's atlas texture. */
    private static final class ParitySlabBlock extends BaseSlab {
        private final ModernMapParityBlocks entry;
        private final Block backing;

        ParitySlabBlock(boolean isDouble, ModernMapParityBlocks entry, Block backing) {
            super(isDouble, entry.material, entry.textureKey);
            this.entry = entry;
            this.backing = backing;
            setBlockName(Tags.MOD_ID + "." + entry.getRegistryName());
            setHardness(entry.material == Material.wood ? 2.0F : 1.5F);
            setResistance(5.0F);
            setStepSound(entry.material == Material.wood ? soundTypeWood : soundTypeStone);
        }

        @Override @SideOnly(Side.CLIENT)
        public IIcon getIcon(int side, int meta) {
            IIcon icon = backing == null ? null : backing.getIcon(side, 0);
            return icon == null ? super.getIcon(side, meta) : icon;
        }

        @Override @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            if (backing != null) backing.registerBlockIcons(reg);
            IIcon icon = backing == null ? null : backing.getIcon(0, 0);
            if (icon == null) icon = registerLegacyModernTexture(
                    "minecraft:block/" + entry.textureKey, reg);
            setIcons(new IIcon[]{icon});
            blockIcon = icon;
            ModernJsonModelBridge.prepare(entry, reg);
        }

        @Override
        public String func_150002_b(int meta) {
            return entry.getRegistryName();
        }
    }

    private static final class ParityDoorBlock extends BaseDoor {
        private final ModernMapParityBlocks entry;

        ParityDoorBlock(ModernMapParityBlocks entry) {
            super(Material.wood, "pale_oak");
            this.entry = entry;
            setBlockName(Tags.MOD_ID + "." + entry.getRegistryName());
        }

        @Override @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            field_150017_a = new IIcon[2];
            field_150016_b = new IIcon[2];
            field_150017_a[0] = registerLegacyModernTexture(
                    "minecraft:block/pale_oak_door_top", reg);
            field_150016_b[0] = registerLegacyModernTexture(
                    "minecraft:block/pale_oak_door_bottom", reg);
            field_150017_a[1] = new IconFlipped(field_150017_a[0], true, false);
            field_150016_b[1] = new IconFlipped(field_150016_b[0], true, false);
            blockIcon = field_150016_b[0];
            ModernJsonModelBridge.prepare(entry, reg);
        }
    }

    /** Supplies the modern door's generated inventory icon without probing a nonexistent 1.7 path. */
    public static final class ParityDoorItemBlock extends ItemBlockNewDoor {
        public ParityDoorItemBlock(Block block) { super(block); }

        @Override @SideOnly(Side.CLIENT)
        public void registerIcons(IIconRegister reg) {
            ModernAssetResourcePack.registerDynamicAlias(
                    "textures/items/modern_item/pale_oak_door.png",
                    "textures/item/pale_oak_door.png");
            itemIcon = reg.registerIcon("minecraft:modern_item/pale_oak_door");
        }
    }

    private static final class ParityTrapdoorBlock extends BaseTrapdoor {
        private final ModernMapParityBlocks entry;

        ParityTrapdoorBlock(ModernMapParityBlocks entry) {
            super(Material.wood, "pale_oak");
            this.entry = entry;
            setBlockName(Tags.MOD_ID + "." + entry.getRegistryName());
        }

        @Override @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            blockIcon = registerLegacyModernTexture(
                    "minecraft:block/pale_oak_trapdoor", reg);
            ModernJsonModelBridge.prepare(entry, reg);
        }
    }

    @SideOnly(Side.CLIENT)
    private static IIcon registerLegacyModernTexture(String texture, IIconRegister register) {
        String normalized = texture.indexOf(':') < 0 ? "minecraft:" + texture : texture;
        String safe = normalized.replace(':', '_').replace('/', '_').replace('.', '_')
                + "_" + Integer.toHexString(normalized.hashCode());
        String synthetic = "modern_model/" + safe;
        String path = normalized.substring(normalized.indexOf(':') + 1);
        String target = path.startsWith("textures/") ? path : "textures/" + path;
        if (!target.endsWith(".png")) target += ".png";
        ModernAssetResourcePack.registerDynamicAlias("textures/blocks/" + synthetic + ".png", target);
        return register.registerIcon("minecraft:" + synthetic);
    }

    /**
     * Copper chest compatibility blocks deliberately reuse vanilla chest behavior/inventory logic.
     * A dedicated TileEntity subclass lets the client bind copper textures without disturbing
     * normal/trapped chest rendering.
     */
    public static final class ParityCopperChestTileEntity extends TileEntityChest {
        // Pairing is supplied by the shared chest-pairing mixins. Pass 36 treats every Copper Chest
        // oxidation/wax identity as one compatible family while normal/trapped chests remain incompatible;
        // reciprocal pairing is persisted through EFRPairDirection on both halves.
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
        // Modern chiseled-bookshelf comparator state. -1 is the safe legacy/default value when an
        // older EFR save predates Pass 32; Backporter input uses the exact 0..5 compartment index.
        private int lastInteractedSlot = -1;

        public int getOccupancyMask() {
            int mask = 0;
            for (int slot = 0; slot < books.length; slot++) {
                if (books[slot] != null) mask |= 1 << slot;
            }
            return mask;
        }

        public int getLastInteractedSlot() {
            return lastInteractedSlot;
        }

        public void setLastInteractedSlot(int slot) {
            int next = slot >= 0 && slot < books.length ? slot : -1;
            if (lastInteractedSlot == next) return;
            lastInteractedSlot = next;
            // Inventory mutations own the render/update packet. Keeping comparator state separate
            // avoids a second packet racing the occupied-slot packet during rapid insert/remove.
            markDirty();
            if (worldObj != null) worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType());
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
            tag.setInteger("LastInteractedSlot", lastInteractedSlot);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            lastInteractedSlot = tag.hasKey("LastInteractedSlot")
                    ? Math.max(-1, Math.min(5, tag.getInteger("LastInteractedSlot"))) : -1;
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
            // The occupied-slot geometry is compiled into the owning chunk's display list. A tile
            // packet updates the six stacks, but 1.7 does not automatically rebuild that display
            // list; explicitly dirty this one block so inserted/removed books become visible now.
            if (worldObj != null) {
                worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
            }
        }
    }

    /** Three visible full-stack slots used by every 1.21.11 Shelf wood family. */
    public static final class ParityShelfTileEntity extends TileEntity implements ISidedInventory {
        private static final int[] SLOTS = {0, 1, 2};
        private final ItemStack[] items = new ItemStack[3];

        public void markDirtyAndSync() {
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType());
            }
        }

        @Override public int getSizeInventory() { return items.length; }
        @Override public ItemStack getStackInSlot(int slot) { return slot >= 0 && slot < items.length ? items[slot] : null; }
        public int getOccupancyMask() {
            int mask = 0;
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot] != null) mask |= 1 << slot;
            }
            return mask;
        }

        /**
         * Modern Shelf hotbar swaps mutate all affected slots first and then publish one
         * block-entity update.  Keeping this primitive side-effect free prevents the 1.7
         * client from observing a half-swapped Shelf while the player's hotbar is stale.
         */
        public ItemStack swapItemNoUpdate(int slot, ItemStack stack) {
            if (slot < 0 || slot >= items.length) return stack;
            ItemStack previous = items[slot];
            items[slot] = stack;
            if (items[slot] != null && items[slot].stackSize > getInventoryStackLimit()) {
                items[slot].stackSize = getInventoryStackLimit();
            }
            return previous;
        }

        @Override public ItemStack decrStackSize(int slot, int amount) {
            ItemStack stack = getStackInSlot(slot);
            if (stack == null) return null;
            ItemStack removed;
            if (stack.stackSize <= amount) { removed = stack; items[slot] = null; }
            else removed = stack.splitStack(amount);
            markDirtyAndSync();
            return removed;
        }
        @Override public ItemStack getStackInSlotOnClosing(int slot) {
            ItemStack stack = getStackInSlot(slot);
            if (stack != null) items[slot] = null;
            return stack;
        }
        @Override public void setInventorySlotContents(int slot, ItemStack stack) {
            if (slot < 0 || slot >= items.length) return;
            items[slot] = stack;
            if (items[slot] != null && items[slot].stackSize > getInventoryStackLimit()) items[slot].stackSize = getInventoryStackLimit();
            markDirtyAndSync();
        }
        @Override public String getInventoryName() { return "container.etfuturum.shelf"; }
        @Override public boolean hasCustomInventoryName() { return false; }
        @Override public int getInventoryStackLimit() { return 64; }
        @Override public boolean isUseableByPlayer(EntityPlayer player) {
            return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                    && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
        }
        @Override public void openInventory() { }
        @Override public void closeInventory() { }
        @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return slot >= 0 && slot < 3 && stack != null; }
        @Override public int[] getAccessibleSlotsFromSide(int side) { return SLOTS; }
        @Override public boolean canInsertItem(int slot, ItemStack stack, int side) { return isItemValidForSlot(slot, stack); }
        @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { return slot >= 0 && slot < 3; }

        @Override public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            NBTTagList list = new NBTTagList();
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot] == null) continue;
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) slot);
                items[slot].writeToNBT(itemTag);
                list.appendTag(itemTag);
            }
            tag.setTag("Items", list);
        }
        @Override public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            for (int slot = 0; slot < items.length; slot++) items[slot] = null;
            NBTTagList list = tag.getTagList("Items", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound itemTag = list.getCompoundTagAt(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < items.length) items[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
        }
        @Override public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound(); writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 4, tag);
        }
        @Override public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
            if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
        }
    }

    /** Persistent hidden loot and four-step brushing progress for suspicious sand/gravel. */
    public static final class ParityBrushableTileEntity extends TileEntity {
        private ItemStack item;
        private int progress;

        public void setHiddenItem(ItemStack stack) { item = stack; markDirtyAndSync(); }

        public boolean brush(EntityPlayer player) {
            if (worldObj == null || worldObj.isRemote || !isSuspiciousBlock(getBlockType())) return false;
            progress++;
            if (progress < 4) {
                worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, progress, 3);
                markDirtyAndSync();
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "dig.sand", 0.35F, 1.15F);
                return false;
            }
            ItemStack revealed = item == null ? null : item.copy();
            Block replacement = getBlockType() == SUSPICIOUS_GRAVEL.get() ? Blocks.gravel : Blocks.sand;
            worldObj.setBlock(xCoord, yCoord, zCoord, replacement, 0, 3);
            if (revealed != null) {
                EntityItem entity = new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.45D, zCoord + 0.5D, revealed);
                double dx = player.posX - (xCoord + 0.5D), dz = player.posZ - (zCoord + 0.5D);
                double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
                entity.motionX = dx / length * 0.12D; entity.motionY = 0.08D; entity.motionZ = dz / length * 0.12D;
                worldObj.spawnEntityInWorld(entity);
            }
            worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "dig.sand", 0.8F, 0.9F);
            return true;
        }
        private void markDirtyAndSync() { markDirty(); if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord); }
        @Override public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag); tag.setInteger("brush_count", progress);
            if (item != null) { NBTTagCompound itemTag = new NBTTagCompound(); item.writeToNBT(itemTag); tag.setTag("item", itemTag); }
        }
        @Override public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag); progress = MathHelper.clamp_int(tag.getInteger("brush_count"), 0, 3);
            item = tag.hasKey("item", 10) ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item")) : null;
        }
        @Override public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound(); writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 5, tag);
        }
        @Override public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
            if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
        }
    }

    /** Persistent pressed state for the 24-state Pale Oak Button orientation contract. */
    public static final class ParityButtonTileEntity extends TileEntity {
        private boolean powered;
        /** Manual wooden-button countdown. Imported Powered=true without this field remains exact. */
        private int manualReleaseTicks;
        /** Runtime-only cause marker, persisted only so a lodged arrow survives save/reload cleanly. */
        private boolean projectileHeld;

        public boolean isPowered() { return powered; }
        public boolean isProjectileHeld() { return projectileHeld; }

        public void setPowered(boolean value) {
            if (!value) {
                manualReleaseTicks = 0;
                projectileHeld = false;
            }
            if (powered == value) return;
            powered = value;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
            }
        }

        public void armManualRelease(int ticks) {
            projectileHeld = false;
            manualReleaseTicks = Math.max(1, ticks);
            setPowered(true);
            markDirty();
        }

        public void holdByProjectile() {
            manualReleaseTicks = 0;
            projectileHeld = true;
            setPowered(true);
            markDirty();
        }

        @Override
        public void updateEntity() {
            if (worldObj == null || worldObj.isRemote || !powered) return;
            Block block = worldObj.getBlock(xCoord, yCoord, zCoord);
            if (projectileHeld) {
                if (block instanceof ParityModelBlock) {
                    ((ParityModelBlock) block).refreshPaleOakButtonProjectileState(worldObj, xCoord, yCoord, zCoord);
                }
                return;
            }
            if (manualReleaseTicks <= 0) return;
            manualReleaseTicks--;
            if (manualReleaseTicks == 0) {
                if (block instanceof ParityModelBlock) {
                    ((ParityModelBlock) block).releasePaleOakButton(worldObj, xCoord, yCoord, zCoord);
                } else {
                    setPowered(false);
                }
            }
        }

        @Override public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setBoolean("Powered", powered);
            if (manualReleaseTicks > 0) tag.setInteger("ManualReleaseTicks", manualReleaseTicks);
            if (projectileHeld) tag.setBoolean("ProjectileHeld", true);
        }
        @Override public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            powered = tag.getBoolean("Powered");
            manualReleaseTicks = Math.max(0, tag.getInteger("ManualReleaseTicks"));
            projectileHeld = tag.getBoolean("ProjectileHeld");
        }
        @Override public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound(); writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 6, tag);
        }
        @Override public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
            if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
        }
    }

    /** One-stack container plus the four modern pot-decoration identities. */
    public static final class ParityDecoratedPotTileEntity extends TileEntity implements ISidedInventory {
        private static final int[] ACCESSIBLE_SLOT = {0};
        // Modern order: back, left, right, front.
        private final String[] sherds = {"minecraft:brick", "minecraft:brick", "minecraft:brick", "minecraft:brick"};
        private ItemStack item;
        private boolean cracked;

        public boolean isCracked() { return cracked; }
        public void setCracked(boolean value) { cracked = value; markDirtyAndSync(); }

        public String[] getSherds() {
            return sherds.clone();
        }

        public void setSherdsFromItem(ItemStack stack) {
            if (stack == null || !stack.hasTagCompound()) return;
            NBTTagCompound root = stack.getTagCompound();
            NBTTagCompound source = root.hasKey("BlockEntityTag", 10)
                    ? root.getCompoundTag("BlockEntityTag") : root;
            readSherds(source);
            cracked = source.getBoolean("Cracked");
            markDirtyAndSync();
        }

        public boolean insertOne(ItemStack held) {
            if (held == null || held.getItem() == null) return false;
            if (item == null) {
                item = held.copy();
                item.stackSize = 1;
            } else {
                if (!item.isItemEqual(held) || !ItemStack.areItemStackTagsEqual(item, held)
                        || item.stackSize >= Math.min(item.getMaxStackSize(), getInventoryStackLimit())) return false;
                item.stackSize++;
            }
            markDirtyAndSync();
            return true;
        }

        public void ejectStoredItem() {
            if (worldObj == null || worldObj.isRemote || item == null) return;
            worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord + 0.5D, yCoord + 0.7D, zCoord + 0.5D, item.copy()));
            item = null;
        }

        private void markDirtyAndSync() {
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                worldObj.func_147453_f(xCoord, yCoord, zCoord, getBlockType());
            }
        }

        private void readSherds(NBTTagCompound tag) {
            NBTTagList list = tag.getTagList("sherds", 8);
            for (int i = 0; i < sherds.length; i++) {
                sherds[i] = i < list.tagCount() ? list.getStringTagAt(i) : "minecraft:brick";
                if (sherds[i] == null || sherds[i].isEmpty()) sherds[i] = "minecraft:brick";
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            NBTTagList list = new NBTTagList();
            for (String sherd : sherds) list.appendTag(new net.minecraft.nbt.NBTTagString(sherd));
            tag.setTag("sherds", list);
            tag.setBoolean("Cracked", cracked);
            if (item != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                item.writeToNBT(itemTag);
                tag.setTag("item", itemTag);
            }
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            readSherds(tag);
            cracked = tag.getBoolean("Cracked");
            item = tag.hasKey("item", 10) ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item")) : null;
        }

        @Override public int getSizeInventory() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? item : null; }
        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            if (slot != 0 || item == null) return null;
            ItemStack removed;
            if (item.stackSize <= amount) {
                removed = item;
                item = null;
            } else {
                removed = item.splitStack(amount);
            }
            markDirtyAndSync();
            return removed;
        }
        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            if (slot != 0) return null;
            ItemStack closing = item;
            item = null;
            return closing;
        }
        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (slot != 0) return;
            item = stack;
            if (item != null && item.stackSize > getInventoryStackLimit()) item.stackSize = getInventoryStackLimit();
            markDirtyAndSync();
        }
        @Override public String getInventoryName() { return "container.etfuturum.decorated_pot"; }
        @Override public boolean hasCustomInventoryName() { return false; }
        @Override public int getInventoryStackLimit() { return 64; }
        @Override public boolean isUseableByPlayer(EntityPlayer player) {
            return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
                    && player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
        }
        @Override public void openInventory() { }
        @Override public void closeInventory() { }
        @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return slot == 0 && stack != null; }
        @Override public int[] getAccessibleSlotsFromSide(int side) { return ACCESSIBLE_SLOT; }
        @Override public boolean canInsertItem(int slot, ItemStack stack, int side) { return isItemValidForSlot(slot, stack); }
        @Override public boolean canExtractItem(int slot, ItemStack stack, int side) { return slot == 0; }

        @Override
        public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 3, tag);
        }

        @Override
        public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
            if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
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

    /** Reliable 1.7.10 underside placement for Pale Hanging Moss and downward chain extension. */
    public static final class ParityPaleHangingMossItemBlock extends ItemBlock {
        public ParityPaleHangingMossItemBlock(Block block) {
            super(block);
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                int side, float hitX, float hitY, float hitZ) {
            // Modern Pale Hanging Moss is placed by clicking the underside of its support or the
            // underside of the current chain end. 1.7's generic ItemBlock/canReplace path is not
            // reliable for this downward-only placement, so resolve the target explicitly.
            if (side != 0 || stack == null || stack.stackSize <= 0) return false;
            if (!(field_150939_a instanceof ParityModelBlock)) return false;
            ParityModelBlock block = (ParityModelBlock) field_150939_a;
            int targetY = y - 1;
            if (targetY < 0 || !block.canPaleHangingMossHangAt(world, x, targetY, z)) return false;
            Block target = world.getBlock(x, targetY, z);
            if (target == null || !target.isReplaceable(world, x, targetY, z)) return false;
            if (!player.canPlayerEdit(x, targetY, z, side, stack)) return false;

            int meta = field_150939_a.onBlockPlaced(world, x, targetY, z, side, hitX, hitY, hitZ, 0);
            if (!placeBlockAt(stack, player, world, x, targetY, z, side, hitX, hitY, hitZ, meta)) return false;
            world.playSoundEffect(x + 0.5D, targetY + 0.5D, z + 0.5D,
                    field_150939_a.stepSound.func_150496_b(),
                    (field_150939_a.stepSound.getVolume() + 1.0F) / 2.0F,
                    field_150939_a.stepSound.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) stack.stackSize--;
            return true;
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

    /** Persistent, packet-synchronized six-face state shared by the Pass 30 blocks. */
    public static final class ParityMultifaceTileEntity extends TileEntity {
        private int faceMask = 1 << ForgeDirection.DOWN.ordinal();

        public int getFaceMask() {
            return faceMask & MULTIFACE_ALL_FACES;
        }

        public void setFaceMask(int mask) {
            int clean = mask & MULTIFACE_ALL_FACES;
            if (clean == faceMask) return;
            faceMask = clean;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
            }
        }

        @Override
        public boolean canUpdate() {
            return false;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setInteger(MULTIFACE_FACE_MASK_TAG, getFaceMask());
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            faceMask = tag.getInteger(MULTIFACE_FACE_MASK_TAG) & MULTIFACE_ALL_FACES;
            if (worldObj != null) worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
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

    /** Lets another item add a supported face to an existing block instead of replacing it. */
    public static final class ParityMultifaceItemBlock extends ItemBlock {
        public ParityMultifaceItemBlock(Block block) {
            super(block);
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                int side, float hitX, float hitY, float hitZ) {
            if (world.getBlock(x, y, z) == field_150939_a && field_150939_a instanceof ParityModelBlock) {
                if (stack.stackSize == 0 || !player.canPlayerEdit(x, y, z, side, stack)) return false;
                ParityModelBlock block = (ParityModelBlock) field_150939_a;
                // When the clicked cell is already multiface, the hit side itself identifies
                // the new attachment face. (Fresh placement against a support uses the opposite.)
                int face = side >= 0 && side < 6 ? side : -1;
                int bit = face < 0 ? 0 : 1 << face;
                int oldMask = getMultifaceFaceMask(world, x, y, z);
                if (bit != 0 && (oldMask & bit) == 0 && block.canAttachMultifaceFace(world, x, y, z, face)) {
                    if (!world.isRemote && block.addMultifaceFace(world, x, y, z, face)) {
                        if (!player.capabilities.isCreativeMode) stack.stackSize--;
                        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
                                field_150939_a.stepSound.func_150496_b(),
                                (field_150939_a.stepSound.getVolume() + 1.0F) / 2.0F,
                                field_150939_a.stepSound.getPitch() * 0.8F);
                    }
                    return true;
                }
            }
            return super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
        }
    }

    private static final class ParityCopperChestBlock extends BlockChest implements IDegradable {
        private final ModernMapParityBlocks entry;

        ParityCopperChestBlock(ModernMapParityBlocks entry) {
            // Use a private chest type so legacy callers still distinguish Copper Chests from vanilla.
            // Pass 36 keeps normal/trapped incompatibility while treating every Copper Chest
            // oxidation/wax identity as one modern-compatible pairing family.
            super(2);
            this.entry = entry;
            setTickRandomly(!entry.isWaxedCopperLifecycleIdentity() && entry.getCopperLifecycleStage() < 3);
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
        public void updateTick(World world, int x, int y, int z, Random random) {
            if (!entry.isWaxedCopperLifecycleIdentity()) tickDegradation(world, x, y, z, random);
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
                float hitX, float hitY, float hitZ) {
            if (tryWaxOnWaxOff(world, x, y, z, player)) return true;
            return super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ);
        }

        @Override public int getCopperMeta(int meta) { return getPass36CopperMeta(this); }
        @Override public Block getCopperBlockFromMeta(int meta) { return getPass36CopperBlock(this, meta); }
        @Override public int getFinalCopperMeta(IBlockAccess world, int x, int y, int z, int meta, int worldMeta) { return worldMeta; }

        @Override
        public void setCopperBlock(Block newBlock, int newMeta, World world, int x, int y, int z) {
            if (!(newBlock instanceof ParityCopperChestBlock) || world.isRemote) {
                IDegradable.super.setCopperBlock(newBlock, newMeta, world, x, y, z);
                return;
            }
            transitionCopperChestPair(world, x, y, z, (ParityCopperChestBlock) newBlock, newMeta);
        }

        private static void transitionCopperChestPair(World world, int x, int y, int z,
                ParityCopperChestBlock target, int targetMeta) {
            TileEntity firstTile = world.getTileEntity(x, y, z);
            if (!(firstTile instanceof TileEntityChest)) {
                // BlockChest.onBlockAdded() recalculates legacy chest facing when the registry
                // identity changes. Restore the exact pre-transition metadata after replacement.
                if (world.setBlock(x, y, z, target, targetMeta, 3))
                    world.setBlockMetadataWithNotify(x, y, z, targetMeta, 2);
                return;
            }
            byte pairDirection = IChestPairingState.NONE;
            if (firstTile instanceof IChestPairingState) {
                IChestPairingState state = (IChestPairingState) firstTile;
                state.etfu$resolvePairing();
                pairDirection = state.etfu$getPairDirection();
            }
            int px = x + ModernChestPairing.offsetX(pairDirection);
            int pz = z + ModernChestPairing.offsetZ(pairDirection);
            boolean hasPartner = ModernChestPairing.isPair(pairDirection)
                    && ModernChestPairing.areCompatibleChestBlocks(world.getBlock(x, y, z), world.getBlock(px, y, pz))
                    && world.getTileEntity(px, y, pz) instanceof TileEntityChest;

            ChestSnapshot first = ChestSnapshot.capture(world, x, y, z);
            ChestSnapshot partner = hasPartner ? ChestSnapshot.capture(world, px, y, pz) : null;
            if (first == null) return;
            first.clearInventory();
            if (partner != null) partner.clearInventory();

            if (!world.setBlock(x, y, z, target, targetMeta, 2)) {
                first.restoreIntoExisting();
                if (partner != null) partner.restoreIntoExisting();
                return;
            }
            if (partner != null && !world.setBlock(px, y, pz, target, partner.metadata, 2)) {
                // Extremely defensive rollback: restore the original identities and NBT if the second half fails.
                world.setBlock(x, y, z, first.block, first.metadata, 2);
                world.setBlockMetadataWithNotify(x, y, z, first.metadata, 2);
                first.restoreIntoExisting();
                partner.restoreIntoExisting();
                return;
            }

            // 1.7 BlockChest.onBlockAdded() overwrites orientation whenever the block identity is
            // swapped. Re-apply each half's exact saved metadata only after both replacements have
            // completed, then restore the TileEntity NBT/pair state.
            world.setBlockMetadataWithNotify(x, y, z, first.metadata, 2);
            if (partner != null) world.setBlockMetadataWithNotify(px, y, pz, partner.metadata, 2);

            first.restoreIntoReplacement();
            if (partner != null) partner.restoreIntoReplacement();
            first.resolveRestoredPair();
            if (partner != null) partner.resolveRestoredPair();
            world.markBlockForUpdate(x, y, z);
            world.notifyBlocksOfNeighborChange(x, y, z, target);
            if (partner != null) {
                world.markBlockForUpdate(px, y, pz);
                world.notifyBlocksOfNeighborChange(px, y, pz, target);
            }
        }

        private static final class ChestSnapshot {
            final World world; final int x, y, z; final Block block; final int metadata; final NBTTagCompound tag;
            final TileEntityChest original;
            private ChestSnapshot(World world, int x, int y, int z, Block block, int metadata,
                    NBTTagCompound tag, TileEntityChest original) {
                this.world=world; this.x=x; this.y=y; this.z=z; this.block=block; this.metadata=metadata; this.tag=tag; this.original=original;
            }
            static ChestSnapshot capture(World world, int x, int y, int z) {
                TileEntity tile=world.getTileEntity(x,y,z);
                if (!(tile instanceof TileEntityChest)) return null;
                NBTTagCompound tag=new NBTTagCompound(); tile.writeToNBT(tag);
                return new ChestSnapshot(world,x,y,z,world.getBlock(x,y,z),world.getBlockMetadata(x,y,z),tag,(TileEntityChest)tile);
            }
            void clearInventory() {
                for (int slot=0; slot<original.getSizeInventory(); slot++) original.setInventorySlotContents(slot,null);
            }
            void restoreIntoExisting() { original.readFromNBT(tag); original.markDirty(); }
            void restoreIntoReplacement() {
                TileEntity tile=world.getTileEntity(x,y,z);
                if (!(tile instanceof TileEntityChest)) {
                    tile=new ParityCopperChestTileEntity(); world.setTileEntity(x,y,z,tile);
                }
                tile.readFromNBT(tag); tile.markDirty();
                if (tile instanceof TileEntityChest) ((TileEntityChest)tile).updateContainingBlockInfo();
            }
            void resolveRestoredPair() {
                TileEntity tile=world.getTileEntity(x,y,z);
                if (tile instanceof IChestPairingState) ((IChestPairingState)tile).etfu$resolvePairing();
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            // Use stone only during the bootstrap stitch. Once AssetDirector has resolved
            // the 1.21.11 model, publish one of that model's own atlas icons as the 1.7
            // fallback/particle icon instead of leaving a misleading placeholder behind.
            blockIcon = reg.registerIcon("minecraft:stone");
            ModernJsonModelBridge.PreparedModels prepared = ModernJsonModelBridge.prepare(entry, reg);
            if (entry == REPEATING_COMMAND_BLOCK) ModernJsonModelBridge.prepareVanillaCommandBlock(reg);
            IIcon fallback = ModernJsonModelBridge.getFallbackIcon(prepared);
            if (fallback != null) blockIcon = fallback;
        }
    }

    /** Real wooden pressure-plate mechanics with AssetDirector-authored 1.21.11 visuals. */
    private static final class ParityPressurePlateBlock extends BlockPressurePlate {
        private final ModernMapParityBlocks entry;

        ParityPressurePlateBlock(ModernMapParityBlocks entry) {
            super("pale_oak_planks", Material.wood, BlockPressurePlate.Sensitivity.everything);
            this.entry = entry;
            setHardness(0.5F);
            setStepSound(soundTypeWood);
        }

        @Override @SideOnly(Side.CLIENT)
        public void registerBlockIcons(IIconRegister reg) {
            blockIcon = reg.registerIcon("minecraft:planks_oak");
            ModernJsonModelBridge.PreparedModels prepared = ModernJsonModelBridge.prepare(entry, reg);
            IIcon fallback = ModernJsonModelBridge.getFallbackIcon(prepared);
            if (fallback != null) blockIcon = fallback;
        }
        @Override public int getRenderType() { return RenderIDs.MODERN_MAP_PARITY; }
        @Override public boolean renderAsNormalBlock() { return false; }
        @Override public boolean isOpaqueCube() { return false; }
        @Override public int damageDropped(int meta) { return 0; }
    }

    /** Pass 34 full 2 x 3^4 Pale Moss Carpet visible state. */
    public static final class ParityPaleMossCarpetTileEntity extends TileEntity {
        private boolean bottom = true;
        private int north;
        private int east;
        private int south;
        private int west;

        public boolean hasBottom() { return bottom; }
        public int getNorth() { return north; }
        public int getEast() { return east; }
        public int getSouth() { return south; }
        public int getWest() { return west; }
        public int getStateIndex() { return paleMossStateIndex(bottom, north, east, south, west); }

        public void setVisualState(boolean bottom, int north, int east, int south, int west) {
            boolean changed = this.bottom != bottom || this.north != clampPaleMossSide(north)
                    || this.east != clampPaleMossSide(east) || this.south != clampPaleMossSide(south)
                    || this.west != clampPaleMossSide(west);
            this.bottom = bottom;
            this.north = clampPaleMossSide(north);
            this.east = clampPaleMossSide(east);
            this.south = clampPaleMossSide(south);
            this.west = clampPaleMossSide(west);
            if (changed) sync();
        }

        private void sync() {
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        @Override public void writeToNBT(NBTTagCompound tag) {
            super.writeToNBT(tag);
            tag.setBoolean("Bottom", bottom);
            tag.setByte("North", (byte) north);
            tag.setByte("East", (byte) east);
            tag.setByte("South", (byte) south);
            tag.setByte("West", (byte) west);
        }

        @Override public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            bottom = !tag.hasKey("Bottom") || tag.getBoolean("Bottom");
            north = clampPaleMossSide(tag.getByte("North"));
            east = clampPaleMossSide(tag.getByte("East"));
            south = clampPaleMossSide(tag.getByte("South"));
            west = clampPaleMossSide(tag.getByte("West"));
        }

        @Override public Packet getDescriptionPacket() {
            NBTTagCompound tag = new NBTTagCompound();
            writeToNBT(tag);
            return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 34, tag);
        }

        @Override public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /** Pass 35 state-only Vault TE. Facing/ominous remain in metadata; only VaultState needs extra bits. */
    public static final class ParityVaultStateTileEntity extends TileEntity {
        private byte vaultState;
        // Pass 35d1: chunk/TE validation may occur while World#getTileEntity is still installing this TE.
        // Relighting from validate/readFromNBT re-enters getLightValue -> getVaultState -> getTileEntity and can recurse.
        private boolean relightPending;
        public int getVaultState() { return vaultState & 3; }
        public void setVaultState(int state) {
            int clamped = state < 0 ? 0 : state > 3 ? 3 : state;
            if ((vaultState & 3) == clamped) return;
            vaultState = (byte) clamped; sync();
        }
        private void relight() {
            if (worldObj != null) worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord);
        }
        private void sync() {
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                relightPending = false;
                relight();
            } else {
                relightPending = true;
            }
        }
        @Override public void validate() {
            super.validate();
            // Defer until the TE is fully installed; never call updateLightByType from validate().
            relightPending = true;
        }
        @Override public void updateEntity() {
            if (relightPending && worldObj != null) {
                relightPending = false;
                relight();
            }
        }
        @Override public void writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); tag.setByte("VaultState", vaultState); }
        @Override public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            vaultState = (byte) Math.max(0, Math.min(3, tag.getByte("VaultState")));
            // NBT may be read before/while the TE is attached to its chunk. Relight safely on the next TE tick.
            relightPending = true;
        }
        @Override public Packet getDescriptionPacket() { NBTTagCompound tag=new NBTTagCompound(); writeToNBT(tag); return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,35,tag); }
        @Override public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) {
            readFromNBT(packet.func_148857_g());
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);
                // Packet application happens after the TE is installed, so immediate client relight is safe.
                relightPending = false;
                relight();
            }
        }
    }

    /** Pass 35 Crafter TE stores only model-affecting booleans; no inventory/crafting logic is introduced. */
    public static final class ParityCrafterStateTileEntity extends TileEntity {
        private boolean triggered;
        private boolean crafting;
        public boolean isTriggered() { return triggered; }
        public boolean isCrafting() { return crafting; }
        public int getVisualFlags() { return (triggered ? 1 : 0) | (crafting ? 2 : 0); }
        public void setVisualState(boolean triggered, boolean crafting) {
            if (this.triggered == triggered && this.crafting == crafting) return;
            this.triggered=triggered; this.crafting=crafting; sync();
        }
        private void sync() { markDirty(); if (worldObj != null) worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
        @Override public void writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); tag.setBoolean("Triggered",triggered); tag.setBoolean("Crafting",crafting); }
        @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); triggered=tag.getBoolean("Triggered"); crafting=tag.getBoolean("Crafting"); }
        @Override public Packet getDescriptionPacket() { NBTTagCompound tag=new NBTTagCompound(); writeToNBT(tag); return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,35,tag); }
        @Override public void onDataPacket(NetworkManager network, S35PacketUpdateTileEntity packet) { readFromNBT(packet.func_148857_g()); if (worldObj != null) worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
    }

    /** Pass 36 applies IDegradable only to actual copper lifecycle identities, never every parity shell. */
    private static final class ParityCopperLifecycleModelBlock extends ParityModelBlock implements IDegradable {
        ParityCopperLifecycleModelBlock(ModernMapParityBlocks entry) { super(entry); }
        @Override public int getCopperMeta(int meta) { return getPass36CopperMeta(this); }
        @Override public Block getCopperBlockFromMeta(int meta) { return getPass36CopperBlock(this, meta); }
        @Override public int getFinalCopperMeta(IBlockAccess world, int x, int y, int z, int meta, int worldMeta) { return worldMeta; }
    }

    /** Keeps Copper Golem Statue pose metadata through pick/drop/re-placement without creative sub-items. */
    public static final class ParityStatefulItemBlock extends ItemBlock {
        public ParityStatefulItemBlock(Block block) { super(block); setHasSubtypes(true); }
        @Override public int getMetadata(int damage) { return damage & 15; }
    }

    private static class ParityModelBlock extends Block implements ITileEntityProvider {
        // Minecraft Java 1.21.11 TrialSpawnerState.lightLevel():
        // inactive, waiting_for_players, active, waiting_for_reward_ejection, ejecting_reward, cooldown.
        private static final int[] TRIAL_SPAWNER_LIGHT = {0, 4, 8, 8, 8, 0};
        // Minecraft Java 1.21.11 VaultState: HALF_LIT=6 for inactive; LIT=12 for every active state.
        private static final int[] VAULT_LIGHT = {6, 12, 12, 12};

        private final ModernMapParityBlocks entry;
        private final ThreadLocal<Integer> harvestedMultifaceMask = new ThreadLocal<Integer>();

        ParityModelBlock(ModernMapParityBlocks entry) {
            super(entry.material);
            this.entry = entry;
            if (entry.isPass36CopperLifecycleIdentity() && !entry.isWaxedCopperLifecycleIdentity()
                    && entry.getCopperLifecycleStage() < 3) setTickRandomly(true);
            // Block's constructor invokes isOpaqueCube() virtually before this field can be assigned.
            // Recompute the cached vanilla opacity values now that the parity entry is available.
            this.opaque = isOpaqueCube();
            this.lightOpacity = this.opaque ? 255 : 0;
            setHardness(isScaffolding() ? 0.0F : isDecoratedPot() ? 0.0F
                    : entry.material == Material.wood ? 2.0F : entry.material == Material.plants ? 0.0F : 1.5F);
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

        private boolean isDecoratedPot() {
            return entry == DECORATED_POT;
        }

        private boolean isPaleOakButton() { return entry == PALE_OAK_BUTTON; }
        private boolean isPaleMossCarpet() { return entry == PALE_MOSS_CARPET; }
        private boolean isPaleHangingMoss() { return entry == PALE_HANGING_MOSS; }
        private boolean isCreakingHeart() { return entry == CREAKING_HEART; }
        private boolean isDriedGhast() { return entry == DRIED_GHAST; }
        private boolean isTorchflowerCrop() { return entry == TORCHFLOWER_CROP; }
        private boolean isPitcherCrop() { return entry == PITCHER_CROP; }
        private boolean isPitcherPlant() { return entry == PITCHER_PLANT; }
        private boolean isSnifferEgg() { return entry == SNIFFER_EGG; }
        private boolean isSeaPickle() { return entry == SEA_PICKLE; }
        private boolean isTallSeagrass() { return entry == TALL_SEAGRASS; }
        private boolean isCopperTorch() { return entry == COPPER_TORCH || entry == COPPER_WALL_TORCH; }
        private boolean isCopperWallTorch() { return entry == COPPER_WALL_TORCH; }
        private boolean isCopperGolemStatue() { return entry.isCopperGolemStatueIdentity(); }
        private boolean isPass36CopperLifecycle() { return entry.isPass36CopperLifecycleIdentity(); }
        private boolean isTrialSpawner() { return entry == TRIAL_SPAWNER; }
        private boolean isVault() { return entry == VAULT; }
        private boolean isCrafter() { return entry == CRAFTER; }
        private boolean isBell() { return entry == BELL; }
        private boolean isRespawnAnchor() { return entry == RESPAWN_ANCHOR; }
        private boolean isSculkSensor() { return entry == SCULK_SENSOR; }
        private boolean isCalibratedSculkSensor() { return entry == CALIBRATED_SCULK_SENSOR; }
        private boolean isSculkShrieker() { return entry == SCULK_SHRIEKER; }
        private boolean isJigsaw() { return entry == JIGSAW; }
        private boolean isModernCommandVariant() { return entry == REPEATING_COMMAND_BLOCK || entry == CHAIN_COMMAND_BLOCK; }
        private boolean isStructureBlock() { return entry == STRUCTURE_BLOCK; }

        private boolean isSuspicious() {
            return entry == SUSPICIOUS_SAND || entry == SUSPICIOUS_GRAVEL;
        }

        private boolean isFroglight() {
            String name = entry.getRegistryName();
            return name.endsWith("_froglight");
        }

        private boolean isCopperChain() {
            return entry.getRegistryName().endsWith("copper_chain");
        }

        private boolean isAxisLog() {
            return entry.style == Style.LOG;
        }

        private boolean isMultiface() {
            return entry.usesMultifaceState();
        }

        private boolean isSculkVein() {
            return entry == SCULK_VEIN;
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

        private boolean canPaleHangingMossHangAt(IBlockAccess world, int x, int y, int z) {
            Block above = world.getBlock(x, y + 1, z);
            if (above == this) return true;
            // Pale Hanging Moss naturally hangs from foliage as well as full cubes. Requiring a
            // legacy "solid DOWN face" rejects valid leaf/support blocks in 1.7, so accept any
            // real non-replaceable support and still reject air/fluids/plants.
            return above != null && above != Blocks.air
                    && !above.getMaterial().isLiquid()
                    && !above.isReplaceable(world, x, y + 1, z);
        }

        private boolean canPitcherPlantStandAt(IBlockAccess world, int x, int y, int z) {
            Block below = world.getBlock(x, y - 1, z);
            return below != null && (below == Blocks.farmland
                    || World.doesBlockHaveSolidTopSurface(world, x, y - 1, z));
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
            if (isMultiface()) {
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F / 16.0F, 1.0F);
                return;
            }
            if (isPaleOakButton()) {
                setBlockBounds(5.0F/16.0F, 6.0F/16.0F, 14.0F/16.0F, 11.0F/16.0F, 10.0F/16.0F, 1.0F);
                return;
            }
            if (isCopperTorch()) {
                setBlockBounds(6.0F/16.0F, 0.0F, 6.0F/16.0F, 10.0F/16.0F, 10.0F/16.0F, 10.0F/16.0F);
                return;
            }
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
                        11.0F / 16.0F, 7.0F / 16.0F, 11.0F / 16.0F);
                return;
            }
            if (isBell()) {
                setBlockBounds(3.0F/16.0F, 0.0F, 3.0F/16.0F, 13.0F/16.0F, 1.0F, 13.0F/16.0F);
                return;
            }
            if ("heavy_core".equals(name)) {
                setBlockBounds(0.25F, 0.0F, 0.25F, 0.75F, 0.5F, 0.75F);
                return;
            }
            if ("sea_pickle".equals(name)) {
                setBlockBounds(6.0F/16.0F, 0.0F, 6.0F/16.0F, 10.0F/16.0F, 6.0F/16.0F, 10.0F/16.0F);
                return;
            }
            if (isPaleMossCarpet()) {
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F/16.0F, 1.0F);
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

        private boolean buttonPowered(IBlockAccess world, int x, int y, int z) {
            TileEntity tile = world == null ? null : world.getTileEntity(x, y, z);
            return tile instanceof ParityButtonTileEntity && ((ParityButtonTileEntity) tile).isPowered();
        }

        /** Notifies both the button's neighbours and the neighbours of its attached support block. */
        private void notifyPaleOakButtonNeighbors(World world, int x, int y, int z, int state) {
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            int face = state / 4, facing = state & 3;
            if (face == 1) world.notifyBlocksOfNeighborChange(x, y - 1, z, this);
            else if (face == 2) world.notifyBlocksOfNeighborChange(x, y + 1, z, this);
            else if (facing == 0) world.notifyBlocksOfNeighborChange(x, y, z + 1, this);
            else if (facing == 1) world.notifyBlocksOfNeighborChange(x - 1, y, z, this);
            else if (facing == 2) world.notifyBlocksOfNeighborChange(x, y, z - 1, this);
            else world.notifyBlocksOfNeighborChange(x + 1, y, z, this);
        }

        /** Releases a runtime activation without altering imported idle Powered=true state. */
        private void releasePaleOakButton(World world, int x, int y, int z) {
            if (world == null || world.isRemote) return;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof ParityButtonTileEntity)) return;
            ParityButtonTileEntity button = (ParityButtonTileEntity) tile;
            if (!button.isPowered()) return;
            int state = world.getBlockMetadata(x, y, z) & 15;
            button.setPowered(false);
            notifyPaleOakButtonNeighbors(world, x, y, z, state);
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.5F);
        }

        private boolean paleOakButtonHasArrow(World world, int x, int y, int z) {
            setPaleOakButtonBounds(world, x, y, z);
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                    x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
            return !world.getEntitiesWithinAABB(EntityArrow.class, box).isEmpty();
        }

        private void refreshPaleOakButtonProjectileState(World world, int x, int y, int z) {
            if (world == null || world.isRemote) return;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof ParityButtonTileEntity)) return;
            ParityButtonTileEntity button = (ParityButtonTileEntity) tile;
            if (paleOakButtonHasArrow(world, x, y, z)) {
                boolean wasPowered = button.isPowered();
                button.holdByProjectile();
                if (!wasPowered) {
                    int state = world.getBlockMetadata(x, y, z) & 15;
                    notifyPaleOakButtonNeighbors(world, x, y, z, state);
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.6F);
                }
            } else if (button.isProjectileHeld()) {
                releasePaleOakButton(world, x, y, z);
            }
        }

        private void activatePaleOakButtonFromArrow(World world, int x, int y, int z) {
            if (world == null || world.isRemote) return;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof ParityButtonTileEntity)) return;
            ParityButtonTileEntity button = (ParityButtonTileEntity) tile;
            boolean wasPowered = button.isPowered();
            button.holdByProjectile();
            if (!wasPowered) {
                int state = world.getBlockMetadata(x, y, z) & 15;
                notifyPaleOakButtonNeighbors(world, x, y, z, state);
                world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.6F);
            }
        }

        private int paleOakButtonStrongPowerSide(int state) {
            int face = state / 4, facing = state & 3;
            if (face == 1) return 1; // attached below
            if (face == 2) return 0; // attached above
            switch (facing) {
                case 0: return 2; // north-facing, attached south
                case 1: return 5; // east-facing, attached west
                case 2: return 3; // south-facing, attached north
                default:return 4; // west-facing, attached east
            }
        }

        private void setPaleOakButtonBounds(IBlockAccess world, int x, int y, int z) {
            int state = world.getBlockMetadata(x, y, z) & 15;
            int face = state / 4;
            int facing = state & 3;
            float depth = buttonPowered(world, x, y, z) ? 1.0F/16.0F : 2.0F/16.0F;
            float a = 5.0F/16.0F, b = 11.0F/16.0F, c = 6.0F/16.0F, d = 10.0F/16.0F;
            if (face == 1) {
                if ((facing & 1) == 0) setBlockBounds(a, 0.0F, c, b, depth, d);
                else setBlockBounds(c, 0.0F, a, d, depth, b);
            } else if (face == 2) {
                if ((facing & 1) == 0) setBlockBounds(a, 1.0F-depth, c, b, 1.0F, d);
                else setBlockBounds(c, 1.0F-depth, a, d, 1.0F, b);
            } else {
                switch (facing) {
                    case 0: setBlockBounds(a, c, 1.0F-depth, b, d, 1.0F); break; // north-facing, support south
                    case 1: setBlockBounds(0.0F, c, a, depth, d, b); break; // east-facing, support west
                    case 2: setBlockBounds(a, c, 0.0F, b, d, depth); break; // south-facing, support north
                    default: setBlockBounds(1.0F-depth, c, a, 1.0F, d, b); break; // west-facing, support east
                }
            }
        }

        private void setCopperTorchBounds(int meta) {
            float r = 2.0F/16.0F;
            if (!isCopperWallTorch()) { setBlockBounds(0.5F-r, 0.0F, 0.5F-r, 0.5F+r, 10.0F/16.0F, 0.5F+r); return; }
            switch (normaliseHorizontalSide(meta)) {
                case 2: setBlockBounds(0.5F-r, 3.0F/16.0F, 10.0F/16.0F, 0.5F+r, 13.0F/16.0F, 1.0F); break;
                case 3: setBlockBounds(0.5F-r, 3.0F/16.0F, 0.0F, 0.5F+r, 13.0F/16.0F, 6.0F/16.0F); break;
                case 4: setBlockBounds(10.0F/16.0F, 3.0F/16.0F, 0.5F-r, 1.0F, 13.0F/16.0F, 0.5F+r); break;
                default: setBlockBounds(0.0F, 3.0F/16.0F, 0.5F-r, 6.0F/16.0F, 13.0F/16.0F, 0.5F+r); break;
            }
        }

        private int facingIndexForSide(int side) {
            switch (side) { case 2: return 0; case 5: return 1; case 3: return 2; case 4: return 3; default: return 0; }
        }

        private boolean paleOakButtonSupported(IBlockAccess world, int x, int y, int z, int state) {
            int face = state / 4, facing = state & 3;
            if (face == 1) return world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);
            if (face == 2) return world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN, false);
            switch (facing) {
                case 0: return world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH, false);
                case 1: return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST, false);
                case 2: return world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH, false);
                default:return world.isSideSolid(x + 1, y, z, ForgeDirection.WEST, false);
            }
        }

        /** Pass 35 Bell support contract. Attachment 0=floor, 1=ceiling, 2=single wall, 3=double wall. */
        private boolean bellSupportAt(IBlockAccess world, int x, int y, int z, int facing) {
            switch (facing & 3) {
                case 0: return world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH, false);
                case 1: return world.isSideSolid(x + 1, y, z, ForgeDirection.WEST, false);
                case 2: return world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH, false);
                default: return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST, false);
            }
        }

        private boolean bellSupported(IBlockAccess world, int x, int y, int z, int meta) {
            int attachment = (meta >> 2) & 3;
            int facing = meta & 3;
            if (attachment == 0) return world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);
            if (attachment == 1) return world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN, false);
            if (attachment == 2) return bellSupportAt(world, x, y, z, facing);
            return bellSupportAt(world, x, y, z, facing) && bellSupportAt(world, x, y, z, (facing + 2) & 3);
        }

        private boolean copperTorchSupported(IBlockAccess world, int x, int y, int z, int side) {
            if (!isCopperWallTorch()) {
                Block below = world.getBlock(x, y - 1, z);
                return below != null && (World.doesBlockHaveSolidTopSurface(world, x, y - 1, z)
                        || (world instanceof World && below.canPlaceTorchOnTop((World) world, x, y - 1, z)));
            }
            switch (normaliseHorizontalSide(side)) {
                case 2: return world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH, false);
                case 3: return world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH, false);
                case 4: return world.isSideSolid(x + 1, y, z, ForgeDirection.WEST, false);
                default:return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST, false);
            }
        }

        private boolean connectsTo(IBlockAccess world, int x, int y, int z, Style style) {
            if (style == Style.WALL) return ModernWallState.canConnectWallTo(this, world, x, y, z);
            Block other = world.getBlock(x, y, z);
            if (other == this) return true;
            if (style == Style.FENCE && other instanceof BlockFence) return true;
            if (style == Style.FENCE && other instanceof BlockFenceGate) return true;
            ModernMapParityBlocks otherEntry = ModernMapParityBlocks.fromBlock(other);
            if (otherEntry != null) {
                if (otherEntry.style == style) return true;
                if (style == Style.FENCE && otherEntry.style == Style.FENCE_GATE) return true;
            }
            return other != null && other.isOpaqueCube();
        }

        @Override
        public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
            if (isPaleOakButton()) {
                setPaleOakButtonBounds(world, x, y, z);
                return;
            }
            if (isCopperTorch()) {
                setCopperTorchBounds(world.getBlockMetadata(x, y, z) & 7);
                return;
            }
            if (isBell()) {
                int meta=world.getBlockMetadata(x,y,z)&15;
                int attachment=(meta>>2)&3, facing=meta&3;
                if (attachment == 0) { // floor
                    setBlockBounds(3.0F/16.0F,0.0F,3.0F/16.0F,13.0F/16.0F,1.0F,13.0F/16.0F);
                } else if (attachment == 1) { // ceiling
                    setBlockBounds(3.0F/16.0F,0.0F,3.0F/16.0F,13.0F/16.0F,1.0F,13.0F/16.0F);
                } else if ((facing & 1) == 0) {
                    setBlockBounds(2.0F/16.0F,2.0F/16.0F,3.0F/16.0F,14.0F/16.0F,1.0F,13.0F/16.0F);
                } else {
                    setBlockBounds(3.0F/16.0F,2.0F/16.0F,2.0F/16.0F,13.0F/16.0F,1.0F,14.0F/16.0F);
                }
                return;
            }
            if (entry.style == Style.WALL) {
                ModernWallState.State state = ModernWallState.derive(this, world, x, y, z);
                float minX = state.west.isConnected() ? 0.0F : 0.25F;
                float maxX = state.east.isConnected() ? 1.0F : 0.75F;
                float minZ = state.north.isConnected() ? 0.0F : 0.25F;
                float maxZ = state.south.isConnected() ? 1.0F : 0.75F;
                setBlockBounds(minX, 0.0F, minZ, maxX, state.getVisualMaxY(), maxZ);
                return;
            }
            if (isMultiface()) {
                setMultifaceBounds(getMultifaceFaceMask(world, x, y, z));
                return;
            }
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
                float maxY = hanging ? 8.0F / 16.0F : 7.0F / 16.0F;
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
            if (isPaleMossCarpet()) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityPaleMossCarpetTileEntity) {
                    ParityPaleMossCarpetTileEntity moss = (ParityPaleMossCarpetTileEntity) tile;
                    int maxSide = Math.max(Math.max(moss.getNorth(), moss.getSouth()), Math.max(moss.getEast(), moss.getWest()));
                    float maxY = moss.hasBottom() ? 1.0F/16.0F : 0.0F;
                    if (maxSide == 1) maxY = Math.max(maxY, 10.0F/16.0F);
                    else if (maxSide == 2 || (!moss.hasBottom() && maxSide == 0)) maxY = 1.0F;
                    setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, Math.max(maxY, 1.0F/16.0F), 1.0F);
                    return;
                }
            }
            if (isSeaPickle()) {
                int count = (world.getBlockMetadata(x, y, z) & 3) + 1;
                if (count == 1) setBlockBounds(6.0F/16.0F,0.0F,6.0F/16.0F,10.0F/16.0F,6.0F/16.0F,10.0F/16.0F);
                else if (count == 2) setBlockBounds(3.0F/16.0F,0.0F,3.0F/16.0F,13.0F/16.0F,6.0F/16.0F,13.0F/16.0F);
                else if (count == 3) setBlockBounds(2.0F/16.0F,0.0F,2.0F/16.0F,14.0F/16.0F,6.0F/16.0F,14.0F/16.0F);
                else setBlockBounds(2.0F/16.0F,0.0F,2.0F/16.0F,14.0F/16.0F,7.0F/16.0F,14.0F/16.0F);
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
            if (entry == MANGROVE_PROPAGULE && ModBlocks.SAPLING.isEnabled() && ConfigBlocksItems.enableMangroveWoodFamily)
                return ModBlocks.SAPLING.getItem();
            if (isCopperWallTorch() && COPPER_TORCH.get() != null) return Item.getItemFromBlock(COPPER_TORCH.get());
            if (isCoralWallFan()) {
                ModernMapParityBlocks floor = coralFanCompanion(false);
                if (floor != null && floor.get() != null) return Item.getItemFromBlock(floor.get());
            }
            return super.getItemDropped(meta, random, fortune);
        }

        @Override
        public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
            if (entry == MANGROVE_PROPAGULE && ModBlocks.SAPLING.isEnabled() && ConfigBlocksItems.enableMangroveWoodFamily)
                return new ItemStack(ModBlocks.SAPLING.get(), 1, 0);
            if (isCopperWallTorch() && COPPER_TORCH.get() != null) return new ItemStack(COPPER_TORCH.get());
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
            if (isMultiface()) return side >= 0 && side < 6 ? ForgeDirection.OPPOSITES[side] : ForgeDirection.DOWN.ordinal();
            if (isPaleOakButton()) {
                int facing = side >= 2 && side <= 5 ? facingIndexForSide(side) : 0;
                int face = side == 1 ? 1 : side == 0 ? 2 : 0;
                return face * 4 + facing;
            }
            if (isCopperTorch()) {
                if (isCopperWallTorch()) return side >= 2 && side <= 5 ? side : 2;
                return side >= 2 && side <= 5 ? side : 0;
            }
            if (isFroglight() || isCopperChain()) {
                if (side == 4 || side == 5) return 1; // X
                if (side == 2 || side == 3) return 2; // Z
                return 0; // Y
            }
            if (isAxisLog()) {
                if (side == 4 || side == 5) return 1; // X
                if (side == 2 || side == 3) return 2; // Z
                return 0; // Y
            }
            if (isCopperLantern()) {
                if (side == 0) return 1;
                if (side == 1) return 0;
                return canCopperLanternStand(world, x, y, z) ? 0 : 1;
            }
            if (isCopperGolemStatue()) return meta & 15; // pose is carried by item; facing filled by onBlockPlacedBy
            if (isTrialSpawner()) return meta >= 0 && meta < 12 ? meta : 0;
            if (isVault()) return meta & 7;
            if (isCrafter()) return meta >= 0 && meta < 12 ? meta : 5; // north_up default
            if (isBell()) {
                if (meta >= 0 && meta < 16 && meta != 0) return meta; // importer/stateful placement
                if (side == 0) return 4; // ceiling,north; facing filled by player
                if (side == 1) return 0; // floor,north
                if (side >= 2 && side <= 5) {
                    int facing = side == 2 ? 2 : side == 3 ? 0 : side == 4 ? 1 : 3;
                    if (bellSupportAt(world, x, y, z, facing)
                            && bellSupportAt(world, x, y, z, (facing + 2) & 3))
                        return 12 + facing; // double_wall
                    return 8 + facing; // single_wall
                }
                return 0;
            }
            if (isRespawnAnchor()) return meta >= 0 && meta <= 4 ? meta : 0;
            if (isSculkSensor()) return meta >= 0 && meta < 3 ? meta : 0;
            if (isCalibratedSculkSensor()) return meta >= 0 && meta < 12 ? meta : 0;
            if (isSculkShrieker()) return meta & 3;
            if (isJigsaw()) return meta >= 0 && meta < 12 ? meta : 5;
            if (isModernCommandVariant()) {
                int facing = side >= 0 && side < 6 ? side : 2;
                return facing; // conditional=false; importer may set 6..11 directly
            }
            if (isStructureBlock()) return meta & 3;
            if (isCreakingHeart()) {
                int axis = (side == 4 || side == 5) ? 0 : (side == 0 || side == 1) ? 1 : 2;
                return axis; // dormant state index 0; x/y/z = 0/1/2
            }
            if (isDriedGhast()) return 0; // hydration 0; facing is filled by onBlockPlacedBy
            if (isPaleHangingMoss()) return 1; // a newly placed chain end is the visible tip
            if (isPitcherPlant()) return 0; // lower half; onBlockPlacedBy creates the upper half
            if (isSeaPickle()) return 4; // one live pickle by default; bit 2 is bounded live/dead visual state
            if (isTorchflowerCrop() || isPitcherCrop() || isSnifferEgg() || isTallSeagrass()) return 0;
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
            // The 1.7 ItemBlock clicked-side convention is opposite the modern JSON rod's
            // horizontal head direction at this renderer compatibility boundary. Vertical
            // placement is already correct; invert only wall placement.
            switch (side) {
                case 0: return 0;
                case 1: return 1;
                case 2: return 3;
                case 3: return 2;
                case 4: return 5;
                case 5: return 4;
                default: return 1;
            }
        }

        @Override
        public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
            if (isMultiface()) {
                int face = world.getBlockMetadata(x, y, z) & 7;
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityMultifaceTileEntity && face < 6) {
                    ((ParityMultifaceTileEntity) tile).setFaceMask(1 << face);
                }
                world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                return;
            }
            int quadrant = MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
            if (isCopperGolemStatue()) {
                int meta = world.getBlockMetadata(x,y,z) & 15;
                int pose = (meta >> 2) & 3;
                world.setBlockMetadataWithNotify(x,y,z,(pose << 2) | ((quadrant + 2) & 3),2);
                return;
            }
            if (isVault()) {
                int meta=world.getBlockMetadata(x,y,z)&15;
                int ominous=meta&4;
                world.setBlockMetadataWithNotify(x,y,z,ominous | ((quadrant + 2) & 3),2);
                return;
            }
            if (isCrafter()) {
                // Preserve importer-supplied vertical FrontAndTop states; normal horizontal placement
                // uses the player's cardinal direction with TOP=up.
                int[] horizontal = {6, 11, 5, 4}; // south_up, west_up, north_up, east_up for S/W/N/E placer quadrants
                world.setBlockMetadataWithNotify(x,y,z,horizontal[quadrant],2);
                return;
            }
            if (isBell()) {
                int meta=world.getBlockMetadata(x,y,z)&15;
                int attachment=(meta >> 2)&3;
                // Wall attachment facing is determined by the support face; floor/ceiling use player facing.
                if (attachment < 2) world.setBlockMetadataWithNotify(x,y,z,(attachment << 2) | ((quadrant + 2)&3),2);
                return;
            }
            if (isCalibratedSculkSensor()) {
                int meta=world.getBlockMetadata(x,y,z)&15;
                int phase=(meta >> 2)&3; if (phase>2) phase=0;
                world.setBlockMetadataWithNotify(x,y,z,(phase << 2) | ((quadrant + 2)&3),2);
                return;
            }
            if (isPaleOakButton()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                int face = meta / 4;
                if (face != 0) world.setBlockMetadataWithNotify(x, y, z, face * 4 + ((quadrant + 2) & 3), 2);
                return;
            }
            if (isPaleHangingMoss()) {
                // Normal placement creates the bottom tip. If it extends an existing chain, the
                // former tip immediately becomes the body model. Imported metadata remains exact:
                // only player ItemBlock placement enters this path.
                world.setBlockMetadataWithNotify(x, y, z, 1, 2);
                if (world.getBlock(x, y + 1, z) == this)
                    world.setBlockMetadataWithNotify(x, y + 1, z, 0, 3);
                return;
            }
            if (isPitcherPlant()) {
                // The obtainable mature Pitcher Plant is itself a modern double-height block.
                // Normal item placement creates lower+upper; importers may still set either half
                // directly by metadata 0/1 when reconstructing a source map.
                world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                if (!world.isRemote && world.isAirBlock(x, y + 1, z))
                    world.setBlock(x, y + 1, z, this, 1, 3);
                return;
            }
            if (isDriedGhast()) {
                int hydration = (world.getBlockMetadata(x, y, z) >> 2) & 3;
                int facing = (quadrant + 2) & 3; // N/E/S/W model index, matching campfire convention
                world.setBlockMetadataWithNotify(x, y, z, (hydration << 2) | facing, 2);
                return;
            }
            if (isTallSeagrass()) {
                // The parity identity represents both modern HALF states. Normal placement creates
                // the canonical two-block pair; Backporter placement may still set either half
                // directly by metadata without this ItemBlock callback.
                if (!world.isRemote && world.isAirBlock(x, y + 1, z))
                    world.setBlock(x, y + 1, z, this, 1, 3);
                return;
            }
            if (entry == COPPER_TORCH) {
                int side = world.getBlockMetadata(x, y, z) & 7;
                if (side >= 2 && side <= 5 && COPPER_WALL_TORCH.get() != null)
                    world.setBlock(x, y, z, COPPER_WALL_TORCH.get(), side, 3);
                else if (side != 0) world.setBlockMetadataWithNotify(x, y, z, 0, 2);
                return;
            }
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
            if (isDecoratedPot()) {
                int storedWater = world.getBlockMetadata(x, y, z) & 4;
                world.setBlockMetadataWithNotify(x, y, z, storedWater | quadrant, 2);
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityDecoratedPotTileEntity) {
                    ((ParityDecoratedPotTileEntity) tile).setSherdsFromItem(stack);
                }
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

        private static void syncPlayerInventory(EntityPlayer player) {
            player.inventoryContainer.detectAndSendChanges();
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }

        private static int chiseledBookshelfSlot(int facing, float hitX, float hitY, float hitZ) {
            float horizontal;
            switch (facing & 3) {
                case 1: horizontal = 1.0F - hitZ; break; // east: player's left is south
                case 2: horizontal = hitX; break;        // south: player's left is west
                case 3: horizontal = hitZ; break;        // west: player's left is north
                default: horizontal = 1.0F - hitX; break; // north: player's left is east
            }
            int column = Math.max(0, Math.min(2, (int) (horizontal * 3.0F)));
            int row = hitY >= 0.5F ? 0 : 1;
            return row * 3 + column;
        }

        private static int frontSideForFacing(int facing) {
            return facing == 0 ? 2 : facing == 1 ? 5 : facing == 2 ? 3 : 4;
        }

        private static int shelfSlot(int facing, float hitX, float hitZ) {
            float horizontal;
            switch (facing & 3) {
                case 1: horizontal = hitZ; break;
                case 2: horizontal = hitX; break;
                case 3: horizontal = 1.0F - hitZ; break;
                default: horizontal = 1.0F - hitX; break;
            }
            return Math.max(0, Math.min(2, (int) (horizontal * 3.0F)));
        }

        private boolean isConnectedPoweredShelf(World world, int x, int y, int z, int facing) {
            ModernMapParityBlocks neighborEntry = ModernMapParityBlocks.fromBlock(world.getBlock(x, y, z));
            if (neighborEntry == null || neighborEntry.getStyle() != Style.SHELF) return false;
            int meta = world.getBlockMetadata(x, y, z) & 7;
            return (meta & 3) == facing && (meta & 4) != 0
                    && world.getTileEntity(x, y, z) instanceof ParityShelfTileEntity;
        }

        private void swapPoweredShelfGroup(World world, int x, int y, int z, EntityPlayer player, int facing) {
            int leftX = 0, leftZ = 0;
            switch (facing & 3) {
                case 1: leftZ = 1; break;
                case 2: leftX = -1; break;
                case 3: leftZ = -1; break;
                default: leftX = 1; break;
            }
            int startX = x, startZ = z;
            for (int i = 0; i < 2 && isConnectedPoweredShelf(world, startX + leftX, y, startZ + leftZ, facing); i++) {
                startX += leftX; startZ += leftZ;
            }
            int rightX = -leftX, rightZ = -leftZ;
            int shelfCount = 0, countX = startX, countZ = startZ;
            while (shelfCount < 3 && isConnectedPoweredShelf(world, countX, y, countZ, facing)) {
                shelfCount++;
                countX += rightX;
                countZ += rightZ;
            }
            int shelfX = startX, shelfZ = startZ;
            // Modern mapping is always the rightmost N slots: one disconnected powered Shelf
            // swaps 6..8, two connected Shelves swap 3..8, and three swap 0..8.
            int hotbar = 9 - shelfCount * 3;
            for (int shelfIndex = 0; shelfIndex < shelfCount; shelfIndex++) {
                ParityShelfTileEntity shelf = (ParityShelfTileEntity) world.getTileEntity(shelfX, y, shelfZ);
                for (int slot = 0; slot < 3; slot++, hotbar++) {
                    ItemStack carried = player.inventory.getStackInSlot(hotbar);
                    // Remove the server-side hotbar reference before placing it into the Shelf.
                    // This mirrors modern removeItemNoUpdate/swapItemNoUpdate semantics and avoids
                    // a transient duplicate reference during the exchange.
                    player.inventory.setInventorySlotContents(hotbar, null);
                    ItemStack stored = shelf.swapItemNoUpdate(slot, carried);
                    player.inventory.setInventorySlotContents(hotbar, stored);
                }
                // Publish the three Shelf slot changes together instead of once per slot.
                shelf.markDirtyAndSync();
                shelfX += rightX; shelfZ += rightZ;
            }
            player.inventory.markDirty();
            // 1.7 does not automatically synchronize direct InventoryPlayer mutations made by a
            // block interaction.  Without this, only the selected/held slot is corrected client-
            // side and the rest of the hotbar appears duplicated until another inventory update.
            syncPlayerInventory(player);
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
                float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem();

            if (isPass36CopperLifecycle()) {
                if (((IDegradable) this).tryWaxOnWaxOff(world, x, y, z, player)) return true;
                if (isCopperGolemStatue()) {
                    // Modern uses the axe interaction for Copper Golem reanimation. The entity phase is deferred,
                    // so an otherwise-inert axe must not accidentally cycle the statue pose.
                    if (held != null && held.getItem().getToolClasses(held).contains("axe")) return false;
                    if (!world.isRemote) {
                        int oldMeta = world.getBlockMetadata(x, y, z) & 15;
                        int facing = oldMeta & 3;
                        int nextMeta = cycleCopperGolemStatuePoseMeta(oldMeta);
                        // Flag 3 already notifies neighbours and refreshes comparator output for
                        // blocks with hasComparatorInputOverride(); do not call func_147453_f a
                        // second time. Reassert only the exact state client-side if a legacy
                        // neighbour callback touched our facing bits during that notification.
                        world.setBlockMetadataWithNotify(x, y, z, nextMeta, 3);
                        if ((world.getBlockMetadata(x, y, z) & 3) != facing)
                            world.setBlockMetadataWithNotify(x, y, z, nextMeta, 2);
                        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
                                Tags.MC_ASSET_VER + ":entity.copper_golem_become_statue", 1.0F, 1.0F);
                    }
                    return true;
                }
            }

            if ((isTorchflowerCrop() || isPitcherCrop()) && isBoneMeal(held)) {
                return applyAncientCropBoneMeal(world, x, y, z, player, held);
            }

            if (isSeaPickle() && held != null && held.getItem() == Item.getItemFromBlock(this)) {
                int meta = world.getBlockMetadata(x, y, z) & 7;
                int count = (meta & 3) + 1;
                if (count >= 4) return false;
                if (!world.isRemote) {
                    int nextMeta = (meta & 4) | count;
                    world.setBlockMetadataWithNotify(x, y, z, nextMeta, 3);
                    if (!player.capabilities.isCreativeMode && --held.stackSize <= 0)
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                    world.updateLightByType(EnumSkyBlock.Block, x, y, z);
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
                            stepSound.func_150496_b(),
                            (stepSound.getVolume() + 1.0F) / 2.0F,
                            stepSound.getPitch() * 0.8F);
                }
                return true;
            }

            if (isShelf()) {
                int facing = world.getBlockMetadata(x, y, z) & 3;
                if (side != frontSideForFacing(facing)) return false;
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof ParityShelfTileEntity)) return false;
                if (!world.isRemote) {
                    if ((world.getBlockMetadata(x, y, z) & 4) != 0) {
                        swapPoweredShelfGroup(world, x, y, z, player, facing);
                    } else {
                        ParityShelfTileEntity shelf = (ParityShelfTileEntity) tile;
                        int slot = shelfSlot(facing, hitX, hitZ);
                        ItemStack stored = shelf.swapItemNoUpdate(slot, held);
                        // Modern creative-mode single-slot placement keeps the held stack when the
                        // Shelf slot was empty.  Swapping with an occupied slot still exchanges it.
                        ItemStack replacement = player.capabilities.isCreativeMode && stored == null && held != null
                                ? held.copy() : stored;
                        player.inventory.setInventorySlotContents(player.inventory.currentItem, replacement);
                        shelf.markDirtyAndSync();
                        player.inventory.markDirty();
                        syncPlayerInventory(player);
                    }
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 0.5F, 1.0F);
                }
                return true;
            }

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

            if (isPaleOakButton()) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof ParityButtonTileEntity)) return false;
                ParityButtonTileEntity button = (ParityButtonTileEntity) tile;
                if (button.isPowered()) return true;
                if (!world.isRemote) {
                    int state = world.getBlockMetadata(x, y, z) & 15;
                    button.armManualRelease(tickRate(world));
                    notifyPaleOakButtonNeighbors(world, x, y, z, state);
                    // Keep the normal scheduled block tick as a second release path. The TE countdown
                    // makes manual release deterministic even on legacy hosts that coalesce block ticks.
                    world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
                    world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.3F, 0.6F);
                }
                return true;
            }

            if (isDecoratedPot()) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof ParityDecoratedPotTileEntity)) return false;
                ParityDecoratedPotTileEntity pot = (ParityDecoratedPotTileEntity) tile;
                int meta = world.getBlockMetadata(x, y, z) & 7;
                if (held != null && held.getItem() == Items.water_bucket && (meta & 4) == 0) {
                    if (!world.isRemote) {
                        world.setBlockMetadataWithNotify(x, y, z, meta | 4, 3);
                        replaceBucket(player, held, Items.bucket);
                        playModernSound(world, x, y, z, "item.bucket.empty", 1.0F, 1.0F);
                    }
                    return true;
                }
                if (held != null && held.getItem() == Items.bucket && (meta & 4) != 0) {
                    if (!world.isRemote) {
                        world.setBlockMetadataWithNotify(x, y, z, meta & 3, 3);
                        replaceBucket(player, held, Items.water_bucket);
                        playModernSound(world, x, y, z, "item.bucket.fill", 1.0F, 1.0F);
                    }
                    return true;
                }
                if (held != null && held.getItem() != Item.getItemFromBlock(this)) {
                    if (!world.isRemote && pot.insertOne(held)) {
                        if (!player.capabilities.isCreativeMode && --held.stackSize <= 0) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                        }
                        playModernSound(world, x, y, z, "block.decorated_pot.insert", 1.0F, 1.0F);
                    }
                    return true;
                }
                return false;
            }

            if (isChiseledBookshelf()) {
                int facing = world.getBlockMetadata(x, y, z) & 3;
                int expectedSide = frontSideForFacing(facing);
                if (side != expectedSide) return false;
                TileEntity tile = world.getTileEntity(x, y, z);
                if (!(tile instanceof ParityChiseledBookshelfTileEntity)) return false;
                ParityChiseledBookshelfTileEntity shelf = (ParityChiseledBookshelfTileEntity) tile;
                int slot = chiseledBookshelfSlot(facing, hitX, hitY, hitZ);
                ItemStack stored = shelf.getStackInSlot(slot);
                if (stored != null) {
                    if (!world.isRemote) {
                        shelf.setLastInteractedSlot(slot);
                        ItemStack removed = shelf.getStackInSlotOnClosing(slot);
                        shelf.markDirtyAndSync();
                        if (!player.inventory.addItemStackToInventory(removed)) {
                            world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, removed));
                        }
                        syncPlayerInventory(player);
                        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 0.5F, 0.85F);
                    }
                    return true;
                }
                if (isBook(held)) {
                    if (!world.isRemote) {
                        ItemStack inserted = held.copy();
                        inserted.stackSize = 1;
                        shelf.setLastInteractedSlot(slot);
                        shelf.setInventorySlotContents(slot, inserted);
                        if (!player.capabilities.isCreativeMode && --held.stackSize <= 0) {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                        }
                        syncPlayerInventory(player);
                        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.pop", 0.5F, 1.0F);
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

        private static void replaceBucket(EntityPlayer player, ItemStack held, Item replacement) {
            if (player.capabilities.isCreativeMode) return;
            if (held.stackSize <= 1) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(replacement));
            } else {
                held.stackSize--;
                ItemStack result = new ItemStack(replacement);
                if (!player.inventory.addItemStackToInventory(result) && player.worldObj != null) {
                    player.worldObj.spawnEntityInWorld(new EntityItem(player.worldObj, player.posX, player.posY, player.posZ, result));
                }
            }
        }

        private void setMetadataAndRelight(World world, int x, int y, int z, int meta) {
            world.setBlockMetadataWithNotify(x, y, z, meta, 3);
            world.updateLightByType(EnumSkyBlock.Block, x, y, z);
        }

        private static boolean isBoneMeal(ItemStack stack) {
            return stack != null && stack.getItem() == Items.dye && stack.getItemDamage() == 15;
        }

        private static void consumeBoneMeal(EntityPlayer player, ItemStack stack) {
            if (player == null || stack == null || player.capabilities.isCreativeMode) return;
            if (--stack.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }

        /**
         * Bounded Pass-34c crop acceleration. 1.21.11 uses a one-stage bonemeal increment for
         * both ancient crops. Torchflower's two crop states are followed by the ordinary
         * Torchflower block; Pitcher becomes two blocks tall from age 3 onward.
         */
        private boolean applyAncientCropBoneMeal(World world, int x, int y, int z, EntityPlayer player, ItemStack held) {
            if (!isBoneMeal(held)) return false;

            if (isTorchflowerCrop()) {
                int age = world.getBlockMetadata(x, y, z) & 1;
                if (!world.isRemote) {
                    if (age == 0) {
                        world.setBlockMetadataWithNotify(x, y, z, 1, 3);
                    } else if (TORCHFLOWER.get() != null) {
                        world.setBlock(x, y, z, TORCHFLOWER.get(), 0, 3);
                    } else {
                        return false;
                    }
                    consumeBoneMeal(player, held);
                    world.playAuxSFX(2005, x, y, z, 0);
                }
                return true;
            }

            if (isPitcherCrop()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                boolean upper = meta >= 5;
                int lowerY = upper ? y - 1 : y;
                if (world.getBlock(x, lowerY, z) != this) return false;
                int lowerMeta = world.getBlockMetadata(x, lowerY, z) & 15;
                int age = lowerMeta >= 5 ? lowerMeta - 5 : lowerMeta;
                if (age >= 4) return false;

                int nextAge = age + 1;
                if (nextAge >= 3 && world.getBlock(x, lowerY + 1, z) != this
                        && !world.isAirBlock(x, lowerY + 1, z)) {
                    return false;
                }

                if (!world.isRemote) {
                    world.setBlockMetadataWithNotify(x, lowerY, z, nextAge, 3);
                    if (nextAge >= 3) {
                        world.setBlock(x, lowerY + 1, z, this, 5 + nextAge, 3);
                    } else if (world.getBlock(x, lowerY + 1, z) == this) {
                        int upperMeta = world.getBlockMetadata(x, lowerY + 1, z) & 15;
                        if (upperMeta >= 5) world.setBlockToAir(x, lowerY + 1, z);
                    }
                    consumeBoneMeal(player, held);
                    world.playAuxSFX(2005, x, lowerY, z, 0);
                }
                return true;
            }

            return false;
        }

        @Override
        public int quantityDropped(int meta, int fortune, Random random) {
            if (isPitcherPlant()) return 1;
            if (isSegmentedGroundDecal()) return ((meta >> 2) & 3) + 1;
            if (isSeaPickle()) return (meta & 3) + 1;
            if (isCandle() || isTurtleEgg()) return (meta & 3) + 1;
            return super.quantityDropped(meta, fortune, random);
        }

        @Override
        public int damageDropped(int meta) {
            if (isCopperGolemStatue()) return meta & 15;
            return (isSegmentedGroundDecal() || isCandle() || isTurtleEgg() || isCampfire() || isScaffolding()
                    || isFroglight() || isCopperChain() || isCopperLantern() || isShelf() || isChiseledBookshelf()
                    || isDecoratedPot() || isAxisLog() || isMultiface() || isPaleOakButton() || isCopperTorch()
                    || isPaleMossCarpet() || isPaleHangingMoss() || isCreakingHeart() || isDriedGhast()
                    || isTorchflowerCrop() || isPitcherCrop() || isPitcherPlant() || isSnifferEgg() || isSeaPickle() || isTallSeagrass()
                    || isTrialSpawner() || isVault() || isCrafter() || isBell() || isRespawnAnchor() || isSculkSensor()
                    || isCalibratedSculkSensor() || isSculkShrieker() || isJigsaw() || isModernCommandVariant() || isStructureBlock())
                    ? 0 : super.damageDropped(meta);
        }

        @Override
        public int getDamageValue(World world, int x, int y, int z) {
            if (isCopperGolemStatue()) return world.getBlockMetadata(x,y,z) & 15;
            return (isSegmentedGroundDecal() || isCandle() || isTurtleEgg() || isCampfire() || isScaffolding()
                    || isFroglight() || isCopperChain() || isCopperLantern() || isShelf() || isChiseledBookshelf()
                    || isDecoratedPot() || isAxisLog() || isMultiface() || isPaleOakButton() || isCopperTorch()
                    || isPaleMossCarpet() || isPaleHangingMoss() || isCreakingHeart() || isDriedGhast()
                    || isTorchflowerCrop() || isPitcherCrop() || isPitcherPlant() || isSnifferEgg() || isSeaPickle() || isTallSeagrass()
                    || isTrialSpawner() || isVault() || isCrafter() || isBell() || isRespawnAnchor() || isSculkSensor()
                    || isCalibratedSculkSensor() || isSculkShrieker() || isJigsaw() || isModernCommandVariant() || isStructureBlock())
                    ? 0 : super.getDamageValue(world, x, y, z);
        }

        @Override
        public boolean hasTileEntity(int metadata) {
            return isSign() || isHangingSign() || isCampfire() || isChiseledBookshelf()
                    || isDecoratedPot() || isShelf() || isSuspicious() || isMultiface() || isPaleOakButton()
                    || isPaleMossCarpet() || isVault() || isCrafter();
        }

        @Override
        public TileEntity createTileEntity(World world, int metadata) {
            if (isSign() || isHangingSign()) return new ParitySignTileEntity();
            if (isCampfire()) return new ParityCampfireTileEntity();
            if (isChiseledBookshelf()) return new ParityChiseledBookshelfTileEntity();
            if (isDecoratedPot()) return new ParityDecoratedPotTileEntity();
            if (isShelf()) return new ParityShelfTileEntity();
            if (isSuspicious()) return new ParityBrushableTileEntity();
            if (isMultiface()) return new ParityMultifaceTileEntity();
            if (isPaleOakButton()) return new ParityButtonTileEntity();
            if (isPaleMossCarpet()) return new ParityPaleMossCarpetTileEntity();
            if (isVault()) return new ParityVaultStateTileEntity();
            if (isCrafter()) return new ParityCrafterStateTileEntity();
            return null;
        }

        @Override
        public TileEntity createNewTileEntity(World world, int metadata) {
            return createTileEntity(world, metadata);
        }

        @Override
        public boolean hasComparatorInputOverride() {
            return isCopperGolemStatue() || isDecoratedPot() || isShelf() || isChiseledBookshelf() || super.hasComparatorInputOverride();
        }

        @Override
        public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (isCopperGolemStatue()) return ((world.getBlockMetadata(x, y, z) >> 2) & 3) + 1;
            if (isChiseledBookshelf()) {
                if (!(tile instanceof ParityChiseledBookshelfTileEntity)) return 0;
                int slot = ((ParityChiseledBookshelfTileEntity) tile).getLastInteractedSlot();
                return slot >= 0 && slot < 6 ? slot + 1 : 0;
            }
            if (isShelf()) {
                return tile instanceof ParityShelfTileEntity
                        ? ((ParityShelfTileEntity) tile).getOccupancyMask() : 0;
            }
            if (isDecoratedPot()) {
                return tile instanceof IInventory ? Container.calcRedstoneFromInventory((IInventory) tile) : 0;
            }
            return super.getComparatorInputOverride(world, x, y, z, side);
        }

        @Override
        public int getLightValue(IBlockAccess world, int x, int y, int z) {
            int meta = world.getBlockMetadata(x, y, z) & 15;
            if (isCandle()) return (meta & 4) != 0 ? ((meta & 3) + 1) * 3 : 0;
            if (isCandleCake()) return (meta & 1) != 0 ? 3 : 0;
            if (isCampfire()) return (meta & 4) != 0 ? (isSoulCampfire() ? 10 : 15) : 0;
            if (isSeaPickle()) return (meta & 4) != 0 ? 6 + (meta & 3) * 3 : 0;
            if (isRespawnAnchor()) {
                final int[] light = {0, 3, 7, 11, 15};
                return light[Math.min(meta, 4)];
            }
            if (isTrialSpawner()) return TRIAL_SPAWNER_LIGHT[meta % 6];
            if (isVault()) return VAULT_LIGHT[getVaultState(world, x, y, z)];
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

        private boolean canAttachMultifaceFace(IBlockAccess world, int x, int y, int z, int face) {
            if (!isMultiface() || face < 0 || face >= 6) return false;
            ForgeDirection direction = ForgeDirection.getOrientation(face);
            int sx = x + direction.offsetX;
            int sy = y + direction.offsetY;
            int sz = z + direction.offsetZ;
            return world.isSideSolid(sx, sy, sz, direction.getOpposite(), false);
        }

        private int supportedMultifaceMask(IBlockAccess world, int x, int y, int z, int mask) {
            int supported = 0;
            for (int face = 0; face < 6; face++) {
                if ((mask & (1 << face)) != 0 && canAttachMultifaceFace(world, x, y, z, face)) supported |= 1 << face;
            }
            return supported;
        }

        private boolean addMultifaceFace(World world, int x, int y, int z, int face) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (!(tile instanceof ParityMultifaceTileEntity) || !canAttachMultifaceFace(world, x, y, z, face)) return false;
            ParityMultifaceTileEntity multiface = (ParityMultifaceTileEntity) tile;
            int bit = 1 << face;
            if ((multiface.getFaceMask() & bit) != 0) return false;
            multiface.setFaceMask(multiface.getFaceMask() | bit);
            return true;
        }

        private void setMultifaceBounds(int mask) {
            float minX = 1.0F, minY = 1.0F, minZ = 1.0F;
            float maxX = 0.0F, maxY = 0.0F, maxZ = 0.0F;
            final float t = 1.0F / 16.0F;
            if ((mask & 1) != 0) { minX = 0; minY = 0; minZ = 0; maxX = 1; maxY = Math.max(maxY, t); maxZ = 1; }
            if ((mask & 2) != 0) { minX = 0; minY = Math.min(minY, 1-t); minZ = 0; maxX = 1; maxY = 1; maxZ = 1; }
            if ((mask & 4) != 0) { minX = 0; minY = 0; minZ = 0; maxX = 1; maxY = 1; maxZ = Math.max(maxZ, t); }
            if ((mask & 8) != 0) { minX = 0; minY = 0; minZ = Math.min(minZ, 1-t); maxX = 1; maxY = 1; maxZ = 1; }
            if ((mask & 16) != 0) { minX = 0; minY = 0; minZ = 0; maxX = Math.max(maxX, t); maxY = 1; maxZ = 1; }
            if ((mask & 32) != 0) { minX = Math.min(minX, 1-t); minY = 0; minZ = 0; maxX = 1; maxY = 1; maxZ = 1; }
            if ((mask & MULTIFACE_ALL_FACES) == 0) { minX = minY = minZ = 0; maxX = maxZ = 1; maxY = t; }
            setBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }

        private void setMultifaceFaceBounds(int face) {
            final float t = 1.0F / 16.0F;
            switch (face) {
                case 0: setBlockBounds(0, 0, 0, 1, t, 1); break;
                case 1: setBlockBounds(0, 1-t, 0, 1, 1, 1); break;
                case 2: setBlockBounds(0, 0, 0, 1, 1, t); break;
                case 3: setBlockBounds(0, 0, 1-t, 1, 1, 1); break;
                case 4: setBlockBounds(0, 0, 0, t, 1, 1); break;
                default: setBlockBounds(1-t, 0, 0, 1, 1, 1); break;
            }
        }

        @Override
        public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
            if (isPaleMossCarpet())
                return side == 1 && world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);
            if (isPaleHangingMoss())
                return canPaleHangingMossHangAt(world, x, y, z);
            if (isPitcherPlant())
                return side == 1 && world.isAirBlock(x, y + 1, z) && canPitcherPlantStandAt(world, x, y, z);
            if (isTallSeagrass())
                return side == 1 && world.isAirBlock(x, y + 1, z)
                        && world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);
            if (isBell()) {
                if (side == 1) return bellSupported(world, x, y, z, 0);
                if (side == 0) return bellSupported(world, x, y, z, 4);
                if (side >= 2 && side <= 5) {
                    int facing = side == 2 ? 2 : side == 3 ? 0 : side == 4 ? 1 : 3;
                    return bellSupported(world, x, y, z, 8 + facing);
                }
                return false;
            }
            if (isMultiface()) {
                int face = side >= 0 && side < 6 ? ForgeDirection.OPPOSITES[side] : -1;
                return canAttachMultifaceFace(world, x, y, z, face);
            }
            if (isPaleOakButton()) {
                if (side == 1) return world.isSideSolid(x, y - 1, z, ForgeDirection.UP);
                if (side == 0) return world.isSideSolid(x, y + 1, z, ForgeDirection.DOWN);
                int state = facingIndexForSide(side);
                return paleOakButtonSupported(world, x, y, z, state);
            }
            if (entry == COPPER_TORCH) {
                if (side == 1) return copperTorchSupported(world, x, y, z, 0);
                if (side == 2) return world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH);
                if (side == 3) return world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH);
                if (side == 4) return world.isSideSolid(x + 1, y, z, ForgeDirection.WEST);
                if (side == 5) return world.isSideSolid(x - 1, y, z, ForgeDirection.EAST);
                return false;
            }
            if (isCopperWallTorch()) return side >= 2 && side <= 5 && copperTorchSupported(world, x, y, z, side);
            return super.canPlaceBlockOnSide(world, x, y, z, side);
        }

        @Override
        public boolean canPlaceBlockAt(World world, int x, int y, int z) {
            if (isPaleMossCarpet())
                return world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);
            if (isPaleHangingMoss())
                return canPaleHangingMossHangAt(world, x, y, z);
            if (isPitcherPlant())
                return world.isAirBlock(x, y + 1, z) && canPitcherPlantStandAt(world, x, y, z);
            if (isSeaPickle())
                return world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);
            if (isTallSeagrass())
                return world.isAirBlock(x, y + 1, z)
                        && world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false);
            if (isBell()) {
                if (bellSupported(world, x, y, z, 0) || bellSupported(world, x, y, z, 4)) return true;
                for (int facing = 0; facing < 4; facing++) if (bellSupported(world, x, y, z, 8 + facing)) return true;
                return false;
            }
            if (isPaleOakButton()) {
                for (int state = 0; state < 12; state++) if (paleOakButtonSupported(world, x, y, z, state)) return true;
                return false;
            }
            if (entry == COPPER_TORCH) {
                if (copperTorchSupported(world, x, y, z, 0)) return true;
                return world.isSideSolid(x, y, z + 1, ForgeDirection.NORTH) || world.isSideSolid(x, y, z - 1, ForgeDirection.SOUTH)
                        || world.isSideSolid(x + 1, y, z, ForgeDirection.WEST) || world.isSideSolid(x - 1, y, z, ForgeDirection.EAST);
            }
            if (isCopperWallTorch()) return copperTorchSupported(world, x, y, z, world.getBlockMetadata(x, y, z) & 7);
            if (isMultiface()) {
                for (int face = 0; face < 6; face++) if (canAttachMultifaceFace(world, x, y, z, face)) return true;
                return false;
            }
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
        public void onBlockHarvested(World world, int x, int y, int z, int meta, EntityPlayer player) {
            super.onBlockHarvested(world, x, y, z, meta, player);
            if (world.isRemote) return;
            if (isPitcherPlant()) {
                boolean upper = (meta & 1) != 0;
                int otherY = upper ? y - 1 : y + 1;
                if (world.getBlock(x, otherY, z) == this
                        && ((world.getBlockMetadata(x, otherY, z) & 1) != 0) != upper)
                    world.setBlockToAir(x, otherY, z);
                return;
            }
            if (isPitcherCrop()) {
                boolean upper = (meta & 15) >= 5;
                int otherY = upper ? y - 1 : y + 1;
                if (world.getBlock(x, otherY, z) == this) {
                    int other = world.getBlockMetadata(x, otherY, z) & 15;
                    if (upper != (other >= 5)) world.setBlockToAir(x, otherY, z);
                }
                return;
            }
            if (isTallSeagrass()) {
                boolean upper = (meta & 1) != 0;
                int otherY = upper ? y - 1 : y + 1;
                if (world.getBlock(x, otherY, z) == this
                        && ((world.getBlockMetadata(x, otherY, z) & 1) != 0) != upper)
                    world.setBlockToAir(x, otherY, z);
            }
        }

        @Override
        public void onBlockAdded(World world, int x, int y, int z) {
            super.onBlockAdded(world, x, y, z);
            if (isTrialSpawner() || isVault()) world.updateLightByType(EnumSkyBlock.Block, x, y, z);
            if (isSuspicious()) world.scheduleBlockUpdate(x, y, z, this, 2);
        }

        /** Mirrors BlockFalling while retaining the buried item/progress block-entity data. */
        private void fallSuspiciousBlock(World world, int x, int y, int z) {
            if (world.isRemote || y < 0 || !BlockFalling.func_149831_e(world, x, y - 1, z)) return;

            int meta = world.getBlockMetadata(x, y, z);
            NBTTagCompound tileData = null;
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile != null) {
                tileData = new NBTTagCompound();
                tile.writeToNBT(tileData);
            }

            byte radius = 32;
            if (!BlockFalling.fallInstantly && world.checkChunksExist(
                    x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)) {
                EntityFallingBlock falling = new EntityFallingBlock(
                        world, x + 0.5D, y + 0.5D, z + 0.5D, this, meta);
                falling.field_145810_d = tileData;
                world.spawnEntityInWorld(falling);
                return;
            }

            world.setBlockToAir(x, y, z);
            int landingY = y;
            while (landingY > 0 && BlockFalling.func_149831_e(world, x, landingY - 1, z)) landingY--;
            if (landingY <= 0 || !world.setBlock(x, landingY, z, this, meta, 3) || tileData == null) return;
            TileEntity landed = world.getTileEntity(x, landingY, z);
            if (landed != null) {
                tileData.setInteger("x", x);
                tileData.setInteger("y", landingY);
                tileData.setInteger("z", z);
                landed.readFromNBT(tileData);
                landed.markDirty();
                world.markBlockForUpdate(x, landingY, z);
            }
        }

        @Override
        public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
            if (isMultiface()) {
                if (!world.isRemote) {
                    TileEntity tile = world.getTileEntity(x, y, z);
                    int oldMask = getMultifaceFaceMask(world, x, y, z);
                    int nextMask = supportedMultifaceMask(world, x, y, z, oldMask);
                    if (nextMask == 0) {
                        if (entry == RESIN_CLUMP) {
                            dropBlockAsItem(world, x, y, z,
                                    new ItemStack(Item.getItemFromBlock(this), Integer.bitCount(oldMask), 0));
                        }
                        world.setBlockToAir(x, y, z);
                    } else if (nextMask != oldMask && tile instanceof ParityMultifaceTileEntity) {
                        ((ParityMultifaceTileEntity) tile).setFaceMask(nextMask);
                    }
                }
                return;
            } else if (isPaleHangingMoss()) {
                if (!canPaleHangingMossHangAt(world, x, y, z)) {
                    if (!world.isRemote) dropBlockAsItem(world, x, y, z, 0, 0);
                    world.setBlockToAir(x, y, z);
                    return;
                }
            } else if (isPitcherPlant()) {
                int meta = world.getBlockMetadata(x, y, z) & 1;
                if (meta == 0 && !canPitcherPlantStandAt(world, x, y, z)) {
                    if (!world.isRemote) dropBlockAsItem(world, x, y, z, 0, 0);
                    world.setBlockToAir(x, y, z);
                    if (world.getBlock(x, y + 1, z) == this) world.setBlockToAir(x, y + 1, z);
                    return;
                }
                if (meta == 1 && world.getBlock(x, y - 1, z) != this) {
                    world.setBlockToAir(x, y, z);
                    return;
                }
            } else if (isSeaPickle()) {
                if (!world.isSideSolid(x, y - 1, z, ForgeDirection.UP, false)) {
                    if (!world.isRemote) dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
                    world.setBlockToAir(x, y, z);
                    return;
                }
            } else if (isBell()) {
                int meta = world.getBlockMetadata(x, y, z) & 15;
                int attachment = (meta >> 2) & 3;
                int facing = meta & 3;
                if (attachment == 2) {
                    boolean primary = bellSupportAt(world, x, y, z, facing);
                    boolean opposite = bellSupportAt(world, x, y, z, (facing + 2) & 3);
                    if (primary && opposite) {
                        // Modern Bell updateShape(): a wall bell reciprocally upgrades when its opposite support appears.
                        world.setBlockMetadataWithNotify(x, y, z, 12 + facing, 3);
                        return;
                    }
                } else if (attachment == 3) {
                    boolean primary = bellSupportAt(world, x, y, z, facing);
                    boolean opposite = bellSupportAt(world, x, y, z, (facing + 2) & 3);
                    if (primary && opposite) return;
                    if (primary || opposite) {
                        int remainingFacing = primary ? facing : ((facing + 2) & 3);
                        world.setBlockMetadataWithNotify(x, y, z, 8 + remainingFacing, 3);
                        return;
                    }
                }
                if (!bellSupported(world, x, y, z, meta)) {
                    if (!world.isRemote) dropBlockAsItem(world, x, y, z, 0, 0);
                    world.setBlockToAir(x, y, z);
                    return;
                }
            } else if (isPaleOakButton()) {
                if (!paleOakButtonSupported(world, x, y, z, world.getBlockMetadata(x, y, z) & 15)) {
                    if (!world.isRemote) dropBlockAsItem(world, x, y, z, 0, 0);
                    world.setBlockToAir(x, y, z);
                    return;
                }
            } else if (isCopperTorch()) {
                int side = world.getBlockMetadata(x, y, z) & 7;
                if (!copperTorchSupported(world, x, y, z, side)) {
                    if (!world.isRemote) dropBlockAsItem(world, x, y, z, 0, 0);
                    world.setBlockToAir(x, y, z);
                    return;
                }
            } else if (isScaffolding()) {
                world.scheduleBlockUpdate(x, y, z, this, 1);
            } else if (isSuspicious()) {
                world.scheduleBlockUpdate(x, y, z, this, 2);
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
        public int tickRate(World world) {
            return isPaleOakButton() ? 30 : super.tickRate(world);
        }

        @Override
        public void updateTick(World world, int x, int y, int z, Random random) {
            if (isPaleOakButton()) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityButtonTileEntity
                        && ((ParityButtonTileEntity) tile).isProjectileHeld()) {
                    refreshPaleOakButtonProjectileState(world, x, y, z);
                } else {
                    releasePaleOakButton(world, x, y, z);
                }
                return;
            }
            if (isSuspicious()) {
                fallSuspiciousBlock(world, x, y, z);
                return;
            }
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
            if (isPass36CopperLifecycle() && !entry.isWaxedCopperLifecycleIdentity()) {
                ((IDegradable) this).tickDegradation(world, x, y, z, random);
                return;
            }
            super.updateTick(world, x, y, z, random);
        }

        @Override
        public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
            if (isPaleOakButton() && entity instanceof EntityArrow && !world.isRemote) {
                activatePaleOakButtonFromArrow(world, x, y, z);
            }
            if (isDecoratedPot() && entity instanceof IProjectile && !world.isRemote) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityDecoratedPotTileEntity) {
                    for (String sherd : ((ParityDecoratedPotTileEntity) tile).getSherds()) {
                        ItemStack drop = ModernPotterySherds.ingredientFor(sherd);
                        world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, drop));
                    }
                }
                playModernSound(world, x, y, z, "block.decorated_pot.shatter", 1.0F, 1.0F);
                world.setBlockToAir(x, y, z);
                entity.setDead();
                return;
            }
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
            if (isCopperTorch()) {
                spawnCopperTorchFlame(world, x, y, z, meta);
            }
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
        private void spawnCopperTorchFlame(World world, int x, int y, int z, int meta) {
            double px = x + 0.5D, py = y + 0.7D, pz = z + 0.5D;
            if (isCopperWallTorch()) {
                final double rise = 0.22D, offset = 0.27D;
                py += rise;
                switch (normaliseHorizontalSide(meta & 7)) {
                    case 2: pz += offset; break;
                    case 3: pz -= offset; break;
                    case 4: px += offset; break;
                    default:px -= offset; break;
                }
            }
            world.spawnParticle("smoke", px, py, pz, 0.0D, 0.0D, 0.0D);
            CustomParticles.spawnCopperFireFlame(world, px, py, pz);
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
            boolean promotePaleHangingMossTip = isPaleHangingMoss() && !world.isRemote
                    && world.getBlock(x, y + 1, z) == this;
            if (isPaleOakButton() && !world.isRemote && buttonPowered(world, x, y, z)) {
                notifyPaleOakButtonNeighbors(world, x, y, z, meta & 15);
            }
            boolean releaseWater = isDecoratedPot() && (meta & 4) != 0;
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
            if (isShelf() && !world.isRemote) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityShelfTileEntity) {
                    ParityShelfTileEntity shelf = (ParityShelfTileEntity) tile;
                    for (int slot = 0; slot < shelf.getSizeInventory(); slot++) {
                        ItemStack stored = shelf.getStackInSlot(slot);
                        if (stored != null) world.spawnEntityInWorld(new EntityItem(world, x + 0.5D, y + 0.5D, z + 0.5D, stored.copy()));
                    }
                }
            }
            if (isDecoratedPot() && !world.isRemote) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityDecoratedPotTileEntity) {
                    ((ParityDecoratedPotTileEntity) tile).ejectStoredItem();
                }
            }
            super.breakBlock(world, x, y, z, block, meta);
            if (promotePaleHangingMossTip && world.getBlock(x, y + 1, z) == this) {
                // Removing the chain end exposes the block above as the new tip.
                world.setBlockMetadataWithNotify(x, y + 1, z, 1, 3);
            }
            if (releaseWater && !world.isRemote && world.getBlock(x, y, z) == Blocks.air) {
                world.setBlock(x, y, z, Blocks.water, 0, 3);
            }
        }

        @Override
        public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
            if (isSuspicious()) return new ArrayList<ItemStack>();
            if (isMultiface()) {
                ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                if (entry == RESIN_CLUMP) {
                    int count = Integer.bitCount(getMultifaceFaceMask(world, x, y, z));
                    if (count > 0) drops.add(new ItemStack(Item.getItemFromBlock(this), count, 0));
                }
                return drops;
            }
            if (entry == POTTED_TORCHFLOWER) {
                ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                drops.add(new ItemStack(Item.getItemFromBlock(Blocks.flower_pot)));
                if (TORCHFLOWER.get() != null) drops.add(new ItemStack(Item.getItemFromBlock(TORCHFLOWER.get())));
                return drops;
            }
            if (isDecoratedPot()) {
                ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                ItemStack pot = new ItemStack(Item.getItemFromBlock(this));
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityDecoratedPotTileEntity) {
                    NBTTagCompound blockEntityTag = new NBTTagCompound();
                    NBTTagList list = new NBTTagList();
                    for (String sherd : ((ParityDecoratedPotTileEntity) tile).getSherds()) {
                        list.appendTag(new net.minecraft.nbt.NBTTagString(sherd));
                    }
                    blockEntityTag.setTag("sherds", list);
                    blockEntityTag.setBoolean("Cracked", ((ParityDecoratedPotTileEntity) tile).isCracked());
                    pot.setTagInfo("BlockEntityTag", blockEntityTag);
                }
                drops.add(pot);
                return drops;
            }
            return super.getDrops(world, x, y, z, metadata, fortune);
        }

        @Override
        public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
            if (isMultiface() && willHarvest) {
                harvestedMultifaceMask.set(getMultifaceFaceMask(world, x, y, z));
            }
            return super.removedByPlayer(world, player, x, y, z, willHarvest);
        }

        @Override
        public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta) {
            if (!isMultiface()) {
                super.harvestBlock(world, player, x, y, z, meta);
                return;
            }
            player.addStat(StatList.mineBlockStatArray[getIdFromBlock(this)], 1);
            player.addExhaustion(0.025F);
            Integer capturedMask = harvestedMultifaceMask.get();
            harvestedMultifaceMask.remove();
            int count = Integer.bitCount(capturedMask == null ? getMultifaceFaceMask(world, x, y, z) : capturedMask);
            boolean silk = EnchantmentHelper.getSilkTouchModifier(player);
            ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
            if (count > 0 && (entry == RESIN_CLUMP || silk)) {
                drops.add(new ItemStack(Item.getItemFromBlock(this), count, 0));
            }
            ForgeEventFactory.fireBlockHarvesting(drops, world, this, x, y, z, meta,
                    EnchantmentHelper.getFortuneModifier(player), 1.0F, silk, player);
            for (ItemStack drop : drops) {
                dropBlockAsItem(world, x, y, z, drop);
            }
        }

        @Override
        protected boolean canSilkHarvest() {
            return isSculkVein() || super.canSilkHarvest();
        }

        @Override
        public int getExpDrop(IBlockAccess world, int metadata, int fortune) {
            return isSculkVein() ? 1 : super.getExpDrop(world, metadata, fortune);
        }

        @Override public boolean canProvidePower() { return isPaleOakButton() || super.canProvidePower(); }
        @Override public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
            return isPaleOakButton() && buttonPowered(world, x, y, z) ? 15 : super.isProvidingWeakPower(world, x, y, z, side);
        }
        @Override public int isProvidingStrongPower(IBlockAccess world, int x, int y, int z, int side) {
            if (!isPaleOakButton()) return super.isProvidingStrongPower(world, x, y, z, side);
            if (!buttonPowered(world, x, y, z)) return 0;
            int state = world.getBlockMetadata(x, y, z) & 15;
            return side == paleOakButtonStrongPowerSide(state) ? 15 : 0;
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
                    || isSegmentedGroundDecal() || isPaleOakButton() || isCopperTorch()) return;
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
            if (entry.style == Style.WALL) {
                ModernWallState.State state = ModernWallState.derive(this, world, x, y, z);
                if (state.up) addCollisionBox(mask, list, x, y, z, 0.25F, 0.0F, 0.25F, 0.75F, 1.5F, 0.75F);
                if (state.north.isConnected()) addCollisionBox(mask, list, x, y, z, 5.0F/16.0F, 0.0F, 0.0F, 11.0F/16.0F, 1.5F, 0.5F);
                if (state.south.isConnected()) addCollisionBox(mask, list, x, y, z, 5.0F/16.0F, 0.0F, 0.5F, 11.0F/16.0F, 1.5F, 1.0F);
                if (state.west.isConnected()) addCollisionBox(mask, list, x, y, z, 0.0F, 0.0F, 5.0F/16.0F, 0.5F, 1.5F, 11.0F/16.0F);
                if (state.east.isConnected()) addCollisionBox(mask, list, x, y, z, 0.5F, 0.0F, 5.0F/16.0F, 1.0F, 1.5F, 11.0F/16.0F);
                return;
            }
            if (entry.style != Style.PANE && entry.style != Style.FENCE) {
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

        @Override
        public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 start, Vec3 end) {
            if (!isMultiface()) return super.collisionRayTrace(world, x, y, z, start, end);
            int mask = getMultifaceFaceMask(world, x, y, z);
            MovingObjectPosition closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (int face = 0; face < 6; face++) {
                if ((mask & (1 << face)) == 0) continue;
                setMultifaceFaceBounds(face);
                MovingObjectPosition hit = super.collisionRayTrace(world, x, y, z, start, end);
                if (hit != null && hit.hitVec != null) {
                    double distance = start.squareDistanceTo(hit.hitVec);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closest = hit;
                    }
                }
            }
            setMultifaceBounds(mask);
            return closest;
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
            if (isSegmentedGroundDecal() || isScaffolding() || isPaleOakButton()) return null;
            if (isPaleMossCarpet()) {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof ParityPaleMossCarpetTileEntity
                        && !((ParityPaleMossCarpetTileEntity) tile).hasBottom()) return null;
                return AxisAlignedBB.getBoundingBox(x, y, z, x + 1.0D, y + 1.0D/16.0D, z + 1.0D);
            }
            if (entry.style == Style.WALL) {
                ModernWallState.State state = ModernWallState.derive(this, world, x, y, z);
                double minX = state.west.isConnected() ? 0.0D : 0.25D;
                double maxX = state.east.isConnected() ? 1.0D : 0.75D;
                double minZ = state.north.isConnected() ? 0.0D : 0.25D;
                double maxZ = state.south.isConnected() ? 1.0D : 0.75D;
                return AxisAlignedBB.getBoundingBox(x + minX, y, z + minZ, x + maxX, y + 1.5D, z + maxZ);
            }
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
