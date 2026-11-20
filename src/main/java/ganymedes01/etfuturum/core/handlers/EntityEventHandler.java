package ganymedes01.etfuturum.core.handlers;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigFunctions;
import ganymedes01.etfuturum.storage.EtFuturumPlayer;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.UUID;

public final class EntityEventHandler {
	public static final EntityEventHandler INSTANCE = new EntityEventHandler();

	private EntityEventHandler() {
		// NO-OP
	}

	@SubscribeEvent
	public void onEntityConstruct(EntityEvent.EntityConstructing event) {
		if (event.entity instanceof EntityPlayer) {
			EtFuturumPlayer.register(((EntityPlayer) event.entity));
		}
	}

	@SubscribeEvent
	public void onPlayerClone(PlayerEvent.Clone event) {
		EtFuturumPlayer.clone(event.original, event.entityPlayer);
	}

	@SubscribeEvent
	public void onZombieDeath(LivingDeathEvent event) {
		if (!ConfigBlocksItems.enableLavaChicken) return;
		if (!(event.entity instanceof EntityZombie zombie)) return;
        if (zombie.worldObj.isRemote) return;
		if (!zombie.isChild()) return;
		if (!(zombie.ridingEntity instanceof EntityChicken)) return;
		var source = event.source;
		boolean validKiller = false;
		if (source.getEntity() instanceof EntityPlayer) {
			validKiller = true;
		} else if (source.getEntity() instanceof EntityWolf wolf) {
            if (wolf.isTamed()) validKiller = true;
		}
		if (!validKiller) return;
		EntityItem drop = new EntityItem(
				zombie.worldObj,
				zombie.posX,
				zombie.posY,
				zombie.posZ,
				ModItems.LAVA_CHICKEN_RECORD.newItemStack()
		);
		drop.delayBeforeCanPickup = 10;
		zombie.worldObj.spawnEntityInWorld(drop);
	}

	@SubscribeEvent
	public void onGhastDeath(LivingDeathEvent event) {
		if (!ConfigBlocksItems.enableTears) return;
		if (!(event.entity instanceof EntityGhast ghast)) return;
		if (ghast.worldObj.isRemote) return;
		var source = event.source;
		if ("fireball".equals(source.getDamageType()) && source.getEntity() instanceof EntityPlayer) {
			EntityItem drop = new EntityItem(
					ghast.worldObj,
					ghast.posX,
					ghast.posY,
					ghast.posZ,
					ModItems.TEARS_RECORD.newItemStack()
			);
			drop.delayBeforeCanPickup = 10;
			ghast.worldObj.spawnEntityInWorld(drop);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void announcePetDeath(LivingDeathEvent event) {
		if (!ConfigFunctions.petDeathMessages || event.isCanceled()
				|| !(event.entityLiving.worldObj instanceof WorldServer world) || world.isRemote) return;

		String ownerId;
		if (event.entityLiving instanceof EntityHorse horse) {
			// Vanilla horses store their owner separately from IEntityOwnable.
			if (!horse.isTame()) return;
			ownerId = horse.func_152119_ch();
		} else if (event.entityLiving instanceof IEntityOwnable ownable) {
			if (ownable instanceof EntityTameable tameable && !tameable.isTamed()) return;
			ownerId = ownable.func_152113_b();
		} else {
			return;
		}
		if (ownerId == null || ownerId.isEmpty()) return;

		// 1.7.10 has no built-in showDeathMessages rule, but packs can supply it.
		GameRules rules = world.getGameRules();
		if (rules.hasRule("showDeathMessages") && !rules.getGameRuleBooleanValue("showDeathMessages")) return;

		UUID ownerUuid;
		try {
			ownerUuid = UUID.fromString(ownerId);
		} catch (IllegalArgumentException ignored) {
			return;
		}
		if (!ownerUuid.toString().equalsIgnoreCase(ownerId)) return;

		for (EntityPlayerMP player : world.func_73046_m().getConfigurationManager().playerEntityList) {
			if (ownerUuid.equals(player.getUniqueID())) {
				player.addChatMessage(event.entityLiving.func_110142_aN().func_151521_b());
				return;
			}
		}
	}
}
