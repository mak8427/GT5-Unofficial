package gregtech.common.ores;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.gtnewhorizons.postea.api.TileEntityReplacementManager;
import com.gtnewhorizons.postea.utility.BlockInfo;

import bartworks.system.material.BWItemMetaGeneratedOre;
import bartworks.system.material.BWMetaGeneratedOres;
import bartworks.system.material.Werkstoff;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.GTMod;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneType;
import gregtech.api.interfaces.IStoneType;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GTUtility.ItemId;
import gregtech.common.GTProxy.OreDropSystem;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public enum BWOreAdapter implements IOreAdapter<Werkstoff> {
    INSTANCE;

    private final EnumMap<StoneType, Ores> ores = new EnumMap<>(StoneType.class);

    private static class Ores {
        public BWMetaGeneratedOres big, bigNatural, small, smallNatural;

        public Ores(StoneType stoneType, String bigSuffix, String smallSuffix) {
            big = new BWMetaGeneratedOres(
                "bw.blockores",
                stoneType,
                false,
                false);
            bigNatural = new BWMetaGeneratedOres(
                "bw.blockores.natural",
                stoneType,
                false,
                true);
            small = new BWMetaGeneratedOres(
                "bw.blockoresSmall",
                stoneType,
                true,
                false);
            smallNatural = new BWMetaGeneratedOres(
                "bw.blockoresSmall.natural",
                stoneType,
                true,
                true);

            GameRegistry.registerBlock(big, BWItemMetaGeneratedOre.class, "bw.blockores." + bigSuffix);
            GameRegistry.registerBlock(bigNatural, BWItemMetaGeneratedOre.class, "bw.blockores.natural." + bigSuffix);
            GameRegistry.registerBlock(small, BWItemMetaGeneratedOre.class, "bw.blockores." + smallSuffix);
            GameRegistry.registerBlock(smallNatural, BWItemMetaGeneratedOre.class, "bw.blockores.natural." + smallSuffix);
        }

        public BWMetaGeneratedOres get(boolean small, boolean natural) {
            if (small) {
                if (natural) {
                    return this.smallNatural;
                } else {
                    return this.small;
                }
            } else {
                if (natural) {
                    return this.bigNatural;
                } else {
                    return this.big;
                }
            }    
        }
    }

    public void init() {
        Ores stoneOres;
        ores.put(StoneType.Stone, stoneOres = new Ores(StoneType.Stone, "01", "02"));
        ores.put(StoneType.Moon, new Ores(StoneType.Moon, "03", "04"));

        TileEntityReplacementManager.tileEntityTransformer("bw.blockoresTE", (tag, world) -> {
            int id = tag.getInteger("m");
            boolean natural = tag.getBoolean("n");
            
            Block block = stoneOres.get(false, natural);

            return new BlockInfo(block, id);
        });
    
        TileEntityReplacementManager.tileEntityTransformer("bw.blockoresSmallTE", (tag, world) -> {
            int id = tag.getInteger("m");
            boolean natural = tag.getBoolean("n");
            
            Block block = stoneOres.get(true, natural);

            return new BlockInfo(block, id);
        });
    }

    @Override
    public boolean supports(Block block, int meta) {
        return block instanceof BWMetaGeneratedOres;
    }

    @Override
    public boolean supports(OreInfo<?> info) {
        IStoneType stone = info.stoneType;
        if (stone == null) stone = StoneType.Stone;
        if (!(stone instanceof StoneType stoneType)) return false;
        if (!this.ores.containsKey(stoneType)) return false;

        if (!(info.material instanceof Werkstoff w)) return false;
        if (!w.hasItemType(OrePrefixes.ore)) return false;
        if ((w.getGenerationFeatures().blacklist & 0b1000) != 0) return false;

        return true;
    }

    @Override
    public OreInfo<Werkstoff> getOreInfo(Block block, int meta) {
        if (!supports(block, meta)) return null;

        BWMetaGeneratedOres oreBlock = (BWMetaGeneratedOres) block;

        OreInfo<Werkstoff> info = OreInfo.getNewInfo();

        info.stoneType = oreBlock.stoneType;
        info.material = Werkstoff.werkstoffHashMap.get((Short) (short) meta);
        info.isSmall = oreBlock.isSmall;
        info.isNatural = oreBlock.isNatural;

        return info;
    }

    @Override
    public ObjectIntPair<Block> getBlock(OreInfo<?> info) {
        IStoneType stone = info.stoneType;
        if (stone == null) stone = StoneType.Stone;

        if (!(stone instanceof StoneType stoneType)) return null;
        if (!(info.material instanceof Werkstoff w)) return null;
        if (!w.hasItemType(OrePrefixes.ore)) return null;
        if ((w.getGenerationFeatures().blacklist & 0b1000) != 0) return null;

        Ores ores = this.ores.get(stoneType);
        if (ores == null) return null;

        return ObjectIntPair.of(ores.get(info.isSmall, info.isNatural), w.getmID());
    }

    @Override
    public List<ItemStack> getOreDrops(OreInfo<?> info2, boolean silktouch, int fortune) {
        if (!supports(info2)) return new ArrayList<>();

        @SuppressWarnings("unchecked")
        OreInfo<Werkstoff> info = (OreInfo<Werkstoff>) info2;

        IStoneType stone = info.stoneType;
        if (stone == null) stone = StoneType.Stone;
        if (!(stone instanceof StoneType stoneType)) return null;
        if (!this.ores.containsKey(stoneType)) return null;

        if (!info.isNatural) fortune = 0;

        if (info.isSmall) {
            return getSmallOreDrops(ThreadLocalRandom.current(), info, fortune);
        } else {
            OreDropSystem oreDropSystem = GTMod.gregtechproxy.oreDropSystem;
    
            if (silktouch) oreDropSystem = OreDropSystem.Block;
    
            return getBigOreDrops(ThreadLocalRandom.current(), oreDropSystem, info, fortune);
        }
    }
    
    @Override
    public List<ItemStack> getPotentialDrops(OreInfo<?> info2) {
        if (!supports(info2)) return new ArrayList<>();

        @SuppressWarnings("unchecked")
        OreInfo<Werkstoff> info = (OreInfo<Werkstoff>) info2;

        if (info.isSmall) {
            List<ItemId> drops = new ArrayList<>();

            for (ItemStack stack : SmallOreDrops.getDropList(info.material.getBridgeMaterial())) {
                ItemId id = ItemId.create(stack);

                if (!drops.contains(id)) drops.add(id);
            }

            List<ItemStack> drops2 = new ArrayList<>();

            for (ItemId id : drops) {
                drops2.add(id.getItemStack());
            }

            return drops2;
        } else {
            return getBigOreDrops(ThreadLocalRandom.current(), GTMod.gregtechproxy.oreDropSystem, info, 0);
        }
    }

    public ArrayList<ItemStack> getSmallOreDrops(Random random, OreInfo<Werkstoff> info, int fortune) {
        Materials bridge = info.material.getBridgeMaterial();

        ArrayList<ItemStack> possibleDrops = SmallOreDrops.getDropList(bridge);
        ArrayList<ItemStack> drops = new ArrayList<>();

        if (!possibleDrops.isEmpty()) {
            int dropCount = Math.max(1, bridge.mOreMultiplier + (fortune > 0 ? random.nextInt(1 + fortune * bridge.mOreMultiplier) : 0) / 2);

            for (int i = 0; i < dropCount; i++) {
                drops.add(GTUtility.copyAmount(1, possibleDrops.get(random.nextInt(possibleDrops.size()))));
            }
        }

        if (random.nextInt(3 + fortune) > 1) {
            drops.add(info.stoneType.getDust(random.nextInt(3) == 0, 1));
        }

        return drops;
    }

    public ArrayList<ItemStack> getBigOreDrops(Random random, OreDropSystem oreDropMode, OreInfo<Werkstoff> info, int fortune) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        
        // For Sake of god of balance!

        switch (oreDropMode) {
            case Item -> {
                drops.add(info.material.get(OrePrefixes.rawOre, info.stoneType.isRich() ? 2 : 1));
            }
            case FortuneItem -> {
                if (fortune > 0) {
                    // Max applicable fortune
                    if (fortune > 3) fortune = 3;

                    int amount = 1 + Math.max(random.nextInt(fortune * (info.stoneType.isRich() ? 2 : 1) + 2) - 1, 0);

                    for (int i = 0; i < amount; i++) {
                        drops.add(info.material.get(OrePrefixes.rawOre, 1));
                    }
                } else {
                    for (int i = 0; i < (info.stoneType.isRich() ? 2 : 1); i++) {
                        drops.add(info.material.get(OrePrefixes.rawOre, 1));
                    }
                }
            }
            case UnifiedBlock -> {
                OreInfo<Werkstoff> info2 = info.clone();

                for (int i = 0; i < (info.stoneType.isRich() ? 2 : 1); i++) {
                    info.stoneType = StoneType.Stone;
                    drops.add(getStack(info, 1));
                }

                info2.release();
            }
            case PerDimBlock -> {
                OreInfo<Werkstoff> info2 = info.clone();

                if (!info.stoneType.isDimensionSpecific()) {
                    info2.stoneType = StoneType.Stone;
                }

                drops.add(getStack(info, 1));

                info2.release();
            }
            case Block -> {
                drops.add(getStack(info, 1));
            }
        }

        return drops;
    }
}