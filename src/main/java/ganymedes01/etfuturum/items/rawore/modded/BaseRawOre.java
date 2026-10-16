package ganymedes01.etfuturum.items.rawore.modded;

import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.items.BaseItem;

public class BaseRawOre extends BaseItem {
	private final String subfolder;

	public BaseRawOre(String subfolder, String name) {
		this.subfolder = subfolder;
		setNames("raw_" + name);
		setCreativeTab(null);
	}

	@Override public void getSubItems(net.minecraft.item.Item item, net.minecraft.creativetab.CreativeTabs tab, java.util.List<net.minecraft.item.ItemStack> list) {}

	@Override
	public String getTextureSubfolder() {
		return subfolder;
	}

	@Override
	public String getTextureDomain() {
		return Tags.MOD_ID;
	}

	@Override
	public String getNameDomain() {
		return super.getNameDomain() + (getTextureSubfolder().isEmpty() ? "" : (super.getNameDomain().isEmpty() ? "" : ".") + getTextureSubfolder());
	}
}
