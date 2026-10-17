package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.storage.EtFuturumPlayer;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import ganymedes01.etfuturum.api.tags.ItemTags;

import java.util.List;
import java.util.Random;

public class ContainerEnchantment extends Container {

	/**
	 * SlotEnchantmentTable object with ItemStack to be enchanted
	 */
	public IInventory tableInventory;

	/**
	 * current world (for bookshelf counting)
	 */
	private final World world;
    private final EntityPlayer owner;
	private final Random rand;
	public int enchantmentSeed;

	private final int posX;
	private final int posY;
	private final int posZ;

	/**
	 * 3-member array storing the enchantment levels of each slot
	 */
	public int[] enchantLevels;
	public int[] enchantmentIds;

	public final boolean noFuel;

	public ContainerEnchantment(InventoryPlayer inventory, World world, int x, int y, int z) {
		owner=inventory.player;
		tableInventory = new InventoryBasic("Enchant", true, 2) {

			@Override
			public void markDirty() {
				super.markDirty();
				ContainerEnchantment.this.onCraftMatrixChanged(this);
			}
		};
		posX = x;
		posY = y;
		posZ = z;
		rand = new Random();
		enchantLevels = new int[3];
		enchantmentIds = new int[]{-1, -1, -1};
		this.world = world;
		enchantmentSeed = getEnchantSeed(inventory.player);
		noFuel = ItemTags.getInTag(Tags.MOD_ID + ":enchantment_fuel").isEmpty();
		addSlotToContainer(new Slot(tableInventory, 0, noFuel ? 25 : 15, 47) {

			@Override
			public int getSlotStackLimit() {
				return 1;
			}
		});
		if (!noFuel) {
			addSlotToContainer(new Slot(tableInventory, 1, 35, 47) {
				@Override
				public boolean isItemValid(ItemStack stack) {
					return ItemTags.hasTag(stack.getItem(), stack.getItemDamage(), Tags.MOD_ID + ":enchantment_fuel");
				}
			});
		}
		int var4;

		for (var4 = 0; var4 < 3; ++var4) {
			for (int var5 = 0; var5 < 9; ++var5) {
				addSlotToContainer(new Slot(inventory, var5 + var4 * 9 + 9, 8 + var5 * 18, 84 + var4 * 18));
			}
		}

		for (var4 = 0; var4 < 9; ++var4) {
			addSlotToContainer(new Slot(inventory, var4, 8 + var4 * 18, 142));
		}
	}

	private static void setEnchantSeed(EntityPlayer player, int seed) {
		EtFuturumPlayer storage = EtFuturumPlayer.get(player);
		storage.setEnchantmentSeed(seed);
	}

	private static int getEnchantSeed(EntityPlayer player) {
		EtFuturumPlayer storage = EtFuturumPlayer.get(player);
		return storage.getEnchantmentSeed();
	}

	private static void chargeForEnchant(EntityPlayer player, Random rand, int amount) {
		if(!player.capabilities.isCreativeMode)player.addExperienceLevel(-amount);

		setEnchantSeed(player, rand.nextInt());
	}

	@Override
	public void addCraftingToCrafters(ICrafting p_75132_1_) {
		super.addCraftingToCrafters(p_75132_1_);
		p_75132_1_.sendProgressBarUpdate(this, 0, enchantLevels[0]);
		p_75132_1_.sendProgressBarUpdate(this, 1, enchantLevels[1]);
		p_75132_1_.sendProgressBarUpdate(this, 2, enchantLevels[2]);
		p_75132_1_.sendProgressBarUpdate(this, 3, enchantmentSeed & 0xFFF0);
        p_75132_1_.sendProgressBarUpdate(this,7,enchantmentSeed >>> 16);
		p_75132_1_.sendProgressBarUpdate(this, 4, enchantmentIds[0]);
		p_75132_1_.sendProgressBarUpdate(this, 5, enchantmentIds[1]);
		p_75132_1_.sendProgressBarUpdate(this, 6, enchantmentIds[2]);
	}

	/**
	 * Looks for changes made in the container, sends them to every listener.
	 */
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

		for (Object crafter : crafters) {
			ICrafting var2 = (ICrafting) crafter;
			var2.sendProgressBarUpdate(this, 0, enchantLevels[0]);
			var2.sendProgressBarUpdate(this, 1, enchantLevels[1]);
			var2.sendProgressBarUpdate(this, 2, enchantLevels[2]);
			var2.sendProgressBarUpdate(this, 3, enchantmentSeed & 0xFFF0);
            var2.sendProgressBarUpdate(this,7,enchantmentSeed >>> 16);
			var2.sendProgressBarUpdate(this, 4, enchantmentIds[0]);
			var2.sendProgressBarUpdate(this, 5, enchantmentIds[1]);
			var2.sendProgressBarUpdate(this, 6, enchantmentIds[2]);
		}
	}

	@Override
	public void updateProgressBar(int p_75137_1_, int p_75137_2_) {
		if (p_75137_1_ >= 0 && p_75137_1_ <= 2)
			enchantLevels[p_75137_1_] = p_75137_2_;
		else if (p_75137_1_ == 3)
			enchantmentSeed = (enchantmentSeed & 0xFFFF0000) | (p_75137_2_ & 0xFFFF);
        else if (p_75137_1_ == 7)
            enchantmentSeed=(enchantmentSeed & 0xFFFF) | ((p_75137_2_ & 0xFFFF)<<16);
		else if (p_75137_1_ >= 4 && p_75137_1_ <= 6)
			enchantmentIds[p_75137_1_ - 4] = p_75137_2_;
		else
			super.updateProgressBar(p_75137_1_, p_75137_2_);
	}

	/**
	 * Callback for when the crafting matrix is changed.
	 */
	@Override
	public void onCraftMatrixChanged(IInventory p_75130_1_) {
		if (p_75130_1_ == tableInventory) {
			ItemStack var2 = p_75130_1_.getStackInSlot(0);
			int power;

			if (var2 != null && var2.isItemEnchantable()) {
				if (!world.isRemote) {
                    enchantmentSeed=getEnchantSeed(owner);
					power = 0;
					int j;

					for (j = -1; j <= 1; ++j)
						for (int k = -1; k <= 1; ++k)
							if ((j != 0 || k != 0) && world.isAirBlock(posX + k, posY, posZ + j) && world.isAirBlock(posX + k, posY + 1, posZ + j)) {
								power += (int) ForgeHooks.getEnchantPower(world, posX + k * 2, posY, posZ + j * 2);
								power += (int) ForgeHooks.getEnchantPower(world, posX + k * 2, posY + 1, posZ + j * 2);

								if (k != 0 && j != 0) {
									power += (int) ForgeHooks.getEnchantPower(world, posX + k * 2, posY, posZ + j);
									power += (int) ForgeHooks.getEnchantPower(world, posX + k * 2, posY + 1, posZ + j);
									power += (int) ForgeHooks.getEnchantPower(world, posX + k, posY, posZ + j * 2);
									power += (int) ForgeHooks.getEnchantPower(world, posX + k, posY + 1, posZ + j * 2);
								}
							}

					rand.setSeed(enchantmentSeed);

					for (j = 0; j < 3; ++j) {
						enchantLevels[j] = EnchantmentHelper.calcItemStackEnchantability(rand, j, power, var2);
						enchantmentIds[j] = -1;

						if (enchantLevels[j] < j + 1)
							enchantLevels[j] = 0;
					}

					for (j = 0; j < 3; ++j)
						if (enchantLevels[j] > 0) {
							List<EnchantmentData> var7 = getEnchantmentList(var2, j, enchantLevels[j]);

							if (var7 != null && !var7.isEmpty()) {
								EnchantmentData var6 = var7.get(rand.nextInt(var7.size()));
								enchantmentIds[j] = var6.enchantmentobj.effectId | var6.enchantmentLevel << 8;
							} else enchantLevels[j]=0;
						}

					detectAndSendChanges();
				}
			} else
				for (power = 0; power < 3; ++power) {
					enchantLevels[power] = 0;
					enchantmentIds[power] = -1;
				}
		}
	}

	/**
	 * enchants the item on the table using the specified slot; also deducts XP from player
	 */
	@Override
	public boolean enchantItem(EntityPlayer player, int id) {
		if(id<0 || id>=3 || !canInteractWith(player)) return false;
        onCraftMatrixChanged(tableInventory);
		ItemStack slot0 = tableInventory.getStackInSlot(0);
		ItemStack slot1 = tableInventory.getStackInSlot(1);
		int var5 = id + 1;

		if (slot0==null || slot0.stackSize!=1 || !slot0.isItemEnchantable()) return false;
        if (!noFuel && (slot1 == null || slot1.stackSize < var5 || !ItemTags.hasTag(slot1.getItem(),slot1.getItemDamage(),Tags.MOD_ID+":enchantment_fuel")) && !player.capabilities.isCreativeMode) {
			return false;
		} else if (enchantLevels[id] > 0 && slot0 != null && (player.experienceLevel >= var5 && player.experienceLevel >= enchantLevels[id] || player.capabilities.isCreativeMode)) {
			if (!world.isRemote) {
				List<EnchantmentData> var6 = getEnchantmentList(slot0, id, enchantLevels[id]);
                if(var6==null || var6.isEmpty())return false;
				boolean var7 = slot0.getItem() == Items.book;

				if (var6 != null && !var6.isEmpty()) {
					chargeForEnchant(player, rand, var5);
					world.playSoundEffect(posX + 0.5F, posY + 0.5F, posZ + 0.5F, Tags.MC_ASSET_VER + ":block.enchantment_table.use", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);

					if (var7) {
                        slot0.func_150996_a(Items.enchanted_book); // Retain name and unrelated custom data.
                        slot0.setItemDamage(0);
                    }

					for (EnchantmentData var9 : var6) {
						if (var7)
							Items.enchanted_book.addEnchantment(slot0, var9);
						else
							slot0.addEnchantment(var9.enchantmentobj, var9.enchantmentLevel);
					}

					if (!noFuel && !player.capabilities.isCreativeMode && slot1 != null) {
						slot1.stackSize -= var5;

						if (slot1.stackSize <= 0) {
							ItemStack containerItem = slot1.getItem().getContainerItem(slot1);
							tableInventory.setInventorySlotContents(1, containerItem != null && (!slot1.getItem().doesContainerItemLeaveCraftingGrid(slot1) || !player.inventory.addItemStackToInventory(containerItem)) ? containerItem : null);
						}
					}

					tableInventory.markDirty();
					enchantmentSeed = getEnchantSeed(player);
					onCraftMatrixChanged(tableInventory);
				}
			}

			return true;
		} else
			return false;
	}

	private List<EnchantmentData> getEnchantmentList(ItemStack stack, int seed, int level) {
		rand.setSeed(enchantmentSeed + seed);
		List<EnchantmentData> list = ganymedes01.etfuturum.recipes.EnchantingTableRecipes.build(rand, stack, level);

		if (stack.getItem() == Items.book && list != null && list.size() > 1)
			list.remove(rand.nextInt(list.size()));

		return list;
	}

	public int getLapisAmount() {
		ItemStack var1 = tableInventory.getStackInSlot(1);
		return var1 == null ? (noFuel ? Integer.MAX_VALUE : 0) : var1.stackSize;
	}

	/**
	 * Called when the container is closed.
	 */
	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);

		if (!world.isRemote)
			for (int i = 0; i < tableInventory.getSizeInventory(); i++) {
				ItemStack stack = tableInventory.getStackInSlotOnClosing(i);
				if (stack != null && (!player.isEntityAlive() || !player.inventory.addItemStackToInventory(stack)))
                    player.dropPlayerItemWithRandomChoice(stack,false);
			}
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return player==owner && ModBlocks.ENCHANTMENT_TABLE.isEnabled()
                && !ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(player)
                && world.getBlock(posX, posY, posZ) == ModBlocks.ENCHANTMENT_TABLE.get() && player.getDistanceSq(posX + 0.5D, posY + 0.5D, posZ + 0.5D) <= 64.0D;
	}

	/**
	 * Take a stack from the specified inventory slot.
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        if(!canInteractWith(playerIn) || index<0 || index>=inventorySlots.size())return null;
        int start=noFuel?1:2,end=start+36;
        Slot slot=(Slot)inventorySlots.get(index);if(!slot.getHasStack())return null;
        ItemStack stack=slot.getStack(),original=stack.copy();
        if(index<start) {
            if(!mergeItemStack(stack,start,end,true))return null;
        } else if(!noFuel && ItemTags.hasTag(stack.getItem(),stack.getItemDamage(),Tags.MOD_ID+":enchantment_fuel")) {
            if(!mergeItemStack(stack,1,2,false))return null;
        } else if(!getSlot(0).getHasStack()) {
            ItemStack single=stack.copy();single.stackSize=1;getSlot(0).putStack(single);--stack.stackSize;
        } else {
            int hotbar=start+27;
            if(!(index<hotbar?mergeItemStack(stack,hotbar,end,false):mergeItemStack(stack,start,hotbar,false)))return null;
        }
        if(stack.stackSize<=0)slot.putStack(null);else slot.onSlotChanged();
        slot.onPickupFromSlot(playerIn,stack);return original;
    }
    @Override public ItemStack slotClick(int slot,int button,int mode,EntityPlayer player) {
        if(!canInteractWith(player) || slot>=inventorySlots.size())return null;
        return super.slotClick(slot,button,mode,player);
    }
}
