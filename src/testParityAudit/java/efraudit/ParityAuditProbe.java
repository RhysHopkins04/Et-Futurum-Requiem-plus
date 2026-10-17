package efraudit;

import com.google.gson.*;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.blocks.ISubBlocksBlock;
import ganymedes01.etfuturum.recipes.*;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Snapshot real startup registries and query modern recipe witnesses without changing gameplay. */
@Mod(modid="efr_parity_audit", name="EFR parity audit", version="audit", dependencies="required-after:etfuturum", acceptableRemoteVersions="*")
public final class ParityAuditProbe {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonElement stack(ItemStack s) {
        if (s == null || s.getItem() == null) return JsonNull.INSTANCE;
        JsonObject j = new JsonObject();
        j.addProperty("item", String.valueOf(Item.itemRegistry.getNameForObject(s.getItem())));
        j.addProperty("meta", s.getItemDamage()); j.addProperty("count", s.stackSize);
        if (s.hasTagCompound()) {
            j.addProperty("nbt",s.getTagCompound().toString());
            if (s.getTagCompound().hasKey("Color",1)) {
                JsonObject bytes=new JsonObject();bytes.addProperty("Color",s.getTagCompound().getByte("Color"));j.add("byte_tags",bytes);
            }
        }
        return j;
    }
    private static ItemStack readStack(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        JsonObject j = e.getAsJsonObject();
        Item item = (Item)Item.itemRegistry.getObject(j.get("item").getAsString());
        if (item == null) throw new IllegalArgumentException("Unknown audit input " + j);
        ItemStack stack=new ItemStack(item, j.has("count") ? j.get("count").getAsInt() : 1, j.get("meta").getAsInt());
        if (j.has("byte_tags")) {
            net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();
            for (java.util.Map.Entry<String,JsonElement> v : j.getAsJsonObject("byte_tags").entrySet()) tag.setByte(v.getKey(),v.getValue().getAsByte());
            stack.setTagCompound(tag);
        }
        return stack;
    }
    private static JsonObject block(Block b) {
        JsonObject j = new JsonObject();
        j.addProperty("registry", String.valueOf(Block.blockRegistry.getNameForObject(b)));
        j.addProperty("class", b.getClass().getName());
        j.addProperty("superclass", b.getClass().getSuperclass().getName());
        j.addProperty("item", String.valueOf(Item.itemRegistry.getNameForObject(Item.getItemFromBlock(b))));
        if (b instanceof ISubBlocksBlock) {
            JsonArray types = new JsonArray();
            for (String s : ((ISubBlocksBlock)b).getTypes()) types.add(new JsonPrimitive(s));
            j.add("subtype_names", types);
        }
        return j;
    }
    private static InventoryCrafting grid() {
        return new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer p) { return true; }
        }, 3, 3);
    }
    @Mod.EventHandler public void start(FMLServerStartedEvent e) throws Exception {
        if (!Boolean.getBoolean("efr.parityAudit")) return;
        MinecraftServer server = MinecraftServer.getServer();
        World world = server.worldServerForDimension(0);
        try {
            JsonObject root = new JsonObject();
            JsonArray blocks = new JsonArray(), vanilla = new JsonArray(), items = new JsonArray(), enums = new JsonArray(), parity = new JsonArray(), recipes = new JsonArray();
            for (Object key : Block.blockRegistry.getKeys()) {
                Block b = (Block)Block.blockRegistry.getObject(key);
                if (key.toString().startsWith("etfuturum:") || b.getClass().getName().startsWith("ganymedes01.etfuturum.")) blocks.add(block(b));
                else if (key.toString().startsWith("minecraft:")) vanilla.add(block(b));
            }
            for (Object key : Item.itemRegistry.getKeys()) {
                Item item = (Item)Item.itemRegistry.getObject(key);
                JsonObject j = new JsonObject(); j.addProperty("registry", key.toString()); j.addProperty("class", item.getClass().getName());
                JsonArray variants = new JsonArray();
                int max = item.getHasSubtypes() ? 16 : 1;
                if(item instanceof ganymedes01.etfuturum.items.BaseSubtypesItem)max=Math.max(max,((ganymedes01.etfuturum.items.BaseSubtypesItem)item).types.length);
                Block b = Block.getBlockFromItem(item);
                if (b instanceof ISubBlocksBlock) max = Math.max(max, ((ISubBlocksBlock)b).getTypes().length);
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (int meta=0;meta<max;meta++) {
                    try {
                        ItemStack s = new ItemStack(item,1,meta); String name = item.getUnlocalizedName(s);
                        boolean namedSubtype = b instanceof ISubBlocksBlock && meta < ((ISubBlocksBlock)b).getTypes().length
                                && !((ISubBlocksBlock)b).getTypes()[meta].isEmpty();
                        if (seen.add(name) || namedSubtype) {JsonObject v=stack(s).getAsJsonObject();v.addProperty("unlocalized",name);variants.add(v);}
                    } catch (Exception ex) {j.addProperty("variant_probe_error", ex.toString()); break;}
                }
                j.add("name_variants", variants); items.add(j);
            }
            for (ModBlocks b : ModBlocks.values()) {
                JsonObject j=block(b.get()); j.addProperty("enum",b.name()); j.addProperty("enabled",b.isEnabled()); enums.add(j);
            }
            for (ModItems i : ModItems.values()) {
                JsonObject j=new JsonObject();j.addProperty("enum",i.name());j.addProperty("enabled",i.isEnabled());
                j.addProperty("registry",String.valueOf(Item.itemRegistry.getNameForObject(i.get())));j.addProperty("class",i.get().getClass().getName());enums.add(j);
            }
            for (ModernMapParityBlocks b : ModernMapParityBlocks.values()) {
                JsonObject j=new JsonObject();j.addProperty("source",b.getRegistryName());j.addProperty("style",b.getStyle().name());
                if (b.get()!=null) j.add("block",block(b.get()));parity.add(j);
            }
            for (Object value : CraftingManager.getInstance().getRecipeList()) {
                IRecipe recipe=(IRecipe)value; JsonObject j=new JsonObject();
                j.addProperty("class",recipe.getClass().getName());j.add("output",stack(recipe.getRecipeOutput()));
                recipes.add(j);
            }
            JsonArray furnace=new JsonArray();
            for (Object value : FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
                java.util.Map.Entry entry=(java.util.Map.Entry)value;ItemStack input=(ItemStack)entry.getKey();
                JsonObject j=new JsonObject();j.add("input",stack(input));j.add("output",stack((ItemStack)entry.getValue()));
                j.add("blasting",stack(BlastFurnaceRecipes.smelting().getSmeltingResult(input)));
                j.add("smoking",stack(SmokerRecipes.smelting().getSmeltingResult(input)));furnace.add(j);
            }
            root.add("blocks",blocks);root.add("items",items);root.add("enums",enums);root.add("parity",parity);root.add("crafting_recipes",recipes);root.add("furnace_recipes",furnace);
            root.add("vanilla_blocks",vanilla);
            root.addProperty("modern_smithing_enabled",ganymedes01.etfuturum.recipes.ModernSmithingRecipes.enabled());
            root.addProperty("crafter_enabled",ganymedes01.etfuturum.tileentities.TileEntityCrafter.enabled());
            root.addProperty("allow_modded_armor_trims",ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowModdedArmorTrims);
            root.addProperty("require_upgrade_template",ganymedes01.etfuturum.configuration.configs.ConfigFunctions.requireNetheriteUpgradeTemplate);
            root.addProperty("lectern_enabled",ganymedes01.etfuturum.tileentities.TileEntityLectern.enabled());
            root.addProperty("cartography_enabled",ganymedes01.etfuturum.recipes.CartographyRecipes.enabled());
            root.addProperty("automatic_composting",ganymedes01.etfuturum.configuration.configs.ConfigFunctions.enableAutoAddComposter);
            root.addProperty("compost_entries",ganymedes01.etfuturum.api.CompostingRegistry.getComposts().size());
            root.addProperty("grindstone_enabled",ganymedes01.etfuturum.recipes.GrindstoneRecipes.enabled());
            root.addProperty("enchanting_enabled",ModBlocks.ENCHANTMENT_TABLE.isEnabled());
            root.addProperty("allow_lectern_book_actions",ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions);
            root.addProperty("loom_enabled",ganymedes01.etfuturum.recipes.LoomRecipes.enabled());
            root.addProperty("stonecutting_recipes",StonecutterRecipes.getAll().size());
            root.addProperty("smithing_recipes",SmithingTableRecipes.getInstance().getRecipes().size());
            root.addProperty("tileReplacementMode",ganymedes01.etfuturum.configuration.configs.ConfigWorld.tileReplacementMode);
            JsonObject observations=new JsonObject();
            if (ModBlocks.SHULKER_BOX.isEnabled()) {
                InventoryCrafting inv=grid();ItemStack box=ModBlocks.SHULKER_BOX.newItemStack();
                net.minecraft.nbt.NBTTagCompound data=new net.minecraft.nbt.NBTTagCompound();data.setString("AuditPayload","keep");box.setTagCompound(data);
                inv.setInventorySlotContents(0,box);inv.setInventorySlotContents(1,new ItemStack(net.minecraft.init.Items.dye,1,1));
                ItemStack valid=CraftingManager.getInstance().findMatchingRecipe(inv,world);
                observations.add("shulker_valid_redye",stack(valid));
                observations.addProperty("shulker_custom_nbt_preserved",valid!=null && valid.hasTagCompound() && "keep".equals(valid.getTagCompound().getString("AuditPayload")));
                inv.setInventorySlotContents(2,new ItemStack(net.minecraft.init.Items.diamond));
                observations.add("shulker_with_unrelated_diamond",stack(CraftingManager.getInstance().findMatchingRecipe(inv,world)));
            }
            if (ModItems.NETHERITE_PICKAXE.isEnabled()) {
                InventoryCrafting inv=grid();ItemStack tool=new ItemStack(net.minecraft.init.Items.diamond_pickaxe);
                net.minecraft.nbt.NBTTagCompound data=new net.minecraft.nbt.NBTTagCompound();data.setString("AuditPayload","before");tool.setTagCompound(data);
                inv.setInventorySlotContents(0,tool);inv.setInventorySlotContents(1,ModItems.NETHERITE_INGOT.newItemStack());
                ItemStack upgraded=SmithingTableRecipes.getInstance().findMatchingRecipe(inv,world);
                if (upgraded!=null && upgraded.hasTagCompound()) upgraded.getTagCompound().setString("AuditPayload","modified-preview");
                observations.addProperty("smithing_preview_mutation_changes_input_nbt","modified-preview".equals(tool.getTagCompound().getString("AuditPayload")));
            }
            root.add("behaviour_observations",observations);
            root.add("capabilities",ganymedes01.etfuturum.world.imported.ImportCapabilities.describe((net.minecraft.world.WorldServer)world));
            Files.write(Paths.get("runtime-inventory.json"),GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
            if (Files.isRegularFile(Paths.get("recipe-requests.json"))) {
                JsonArray requests=new JsonParser().parse(new String(Files.readAllBytes(Paths.get("recipe-requests.json")),StandardCharsets.UTF_8)).getAsJsonArray();
                JsonArray results=new JsonArray();
                for (JsonElement request : requests) {
                    JsonObject q=request.getAsJsonObject();JsonObject result=new JsonObject();result.add("id",q.get("id"));
                    try {
                        String type=q.get("type").getAsString();JsonArray inputs=q.getAsJsonArray("inputs");ItemStack output=null;
                        if(type.equals("modern_smithing")) {
                            output=ganymedes01.etfuturum.recipes.ModernSmithingRecipes.result(readStack(inputs.get(0)),readStack(inputs.get(1)),readStack(inputs.get(2)),world);
                        } else if (type.equals("crafting") || type.equals("smithing")) {
                            InventoryCrafting inv=grid();for(int i=0;i<inputs.size();i++) inv.setInventorySlotContents(i,readStack(inputs.get(i)));
                            output=type.equals("crafting") ? CraftingManager.getInstance().findMatchingRecipe(inv,world) : SmithingTableRecipes.getInstance().findMatchingRecipe(inv,world);
                        } else if (type.equals("smelting")) output=FurnaceRecipes.smelting().getSmeltingResult(readStack(inputs.get(0)));
                        else if (type.equals("blasting")) output=BlastFurnaceRecipes.smelting().getSmeltingResult(readStack(inputs.get(0)));
                        else if (type.equals("smoking")) output=SmokerRecipes.smelting().getSmeltingResult(readStack(inputs.get(0)));
                        else if(type.equals("stonecutting")) {
                            net.minecraftforge.common.util.FakePlayer player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.world.WorldServer)world);
                            int x=-160,y=80,z=96;world.setBlock(x,y,z,ModBlocks.STONECUTTER.get(),0,3);player.setPosition(x+.5D,y+.5D,z+.5D);
                            player.capabilities.isCreativeMode=false;player.inventory.setItemStack(null);
                            ganymedes01.etfuturum.inventory.ContainerStonecutter container=new ganymedes01.etfuturum.inventory.ContainerStonecutter(player.inventory,world,x,y,z);
                            container.input.setInventorySlotContents(0,readStack(inputs.get(0)));
                            for(int index=0;index<container.getRecipes().size();index++) {
                                if(!container.getRecipes().get(index).getId().equals(q.get("recipe").getAsString()))continue;
                                if(!container.enchantItem(player,index))throw new IllegalStateException("Stonecutter selection rejected");
                                container.slotClick(1,0,0,player);
                                output=player.inventory.getItemStack();
                                if(container.input.getStackInSlot(0)!=null)throw new IllegalStateException("Stonecutting did not consume input");
                                if(output!=null)output=output.copy();break;
                            }
                            player.inventory.setItemStack(null);container.onContainerClosed(player);world.setBlockToAir(x,y,z);
                        }
                        else throw new IllegalArgumentException("Unsupported audit query " + type);
                        result.add("output",stack(output));
                    } catch (Throwable ex) {result.addProperty("error",ex.toString());}
                    results.add(result);
                }
                Files.write(Paths.get("recipe-results.json"),GSON.toJson(results).getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("EFR_PARITY_AUDIT PASSED: registry snapshot and recipe queries exported");
        } catch (Throwable ex) {System.out.println("EFR_PARITY_AUDIT FAILED: "+ex);throw new RuntimeException(ex);}
        finally {server.initiateShutdown();}
    }
}
