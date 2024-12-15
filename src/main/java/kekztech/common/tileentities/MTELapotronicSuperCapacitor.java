package kekztech.common.tileentities;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onlyIf;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.withChannel;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.metatileentity.BaseTileEntity.TOOLTIP_DELAY;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.filterByMTEClass;
import static java.lang.Math.min;
import static kekztech.util.Util.toPercentageFrom;
import static kekztech.util.Util.toStandardForm;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnewhorizon.structurelib.util.ItemStackPredicate.NBTMode;
import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.UITexture;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import bartworks.API.BorosilicateGlass;
import gregtech.api.enums.Dyes;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.IGTHatchAdder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.widget.ShutDownReasonSyncer;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.misc.spaceprojects.SpaceProjectManager;
import kekztech.client.gui.KTUITextures;
import kekztech.common.Blocks;
import kekztech.common.itemBlocks.ItemBlockLapotronicEnergyUnit;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoTunnel;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyMulti;
import tectech.thing.metaTileEntity.hatch.MTEHatchEnergyTunnel;

public class MTELapotronicSuperCapacitor extends MTEEnhancedMultiBlockBase<MTELapotronicSuperCapacitor>
    implements ISurvivalConstructable {

    private enum TopState {
        MayBeTop,
        Top,
        NotTop
    }

    private boolean canUseWireless = false;
    private boolean wireless_mode = false;
    private int counter = 1;
    private boolean balanced = false;

    private final Queue<Long> energyInputValues = new LinkedList<>();
    private final Queue<Long> energyOutputValues = new LinkedList<>();

    private final Queue<Long> energyInputValues5m = new LinkedList<>();
    private final Queue<Long> energyOutputValues5m = new LinkedList<>();

    private final Queue<Long> energyInputValues1h = new LinkedList<>();
    private final Queue<Long> energyOutputValues1h = new LinkedList<>();

    private final long max_passive_drain_eu_per_tick_per_uhv_cap = 1_000_000;
    private final long max_passive_drain_eu_per_tick_per_uev_cap = 100_000_000;
    private final long max_passive_drain_eu_per_tick_per_uiv_cap = (long) Math.pow(10, 10);
    private final long max_passive_drain_eu_per_tick_per_umv_cap = (long) Math.pow(10, 12);

    private final BigInteger guiCapacityStoredReformatLimit = BigInteger.valueOf(1_000_000_000_000L);

    private enum Capacitor {

        IV(2, BigInteger.valueOf(ItemBlockLapotronicEnergyUnit.IV_cap_storage)),
        LuV(3, BigInteger.valueOf(ItemBlockLapotronicEnergyUnit.LuV_cap_storage)),
        ZPM(4, BigInteger.valueOf(ItemBlockLapotronicEnergyUnit.ZPM_cap_storage)),
        UV(5, BigInteger.valueOf(ItemBlockLapotronicEnergyUnit.UV_cap_storage)),
        UHV(6, MAX_LONG),
        None(0, BigInteger.ZERO),
        EV(1, BigInteger.valueOf(ItemBlockLapotronicEnergyUnit.EV_cap_storage)),
        UEV(7, MAX_LONG),
        UIV(8, BigInteger.valueOf(ItemBlockLapotronicEnergyUnit.UIV_cap_storage)),
        UMV(9, ItemBlockLapotronicEnergyUnit.UMV_cap_storage);

        private final int minimalGlassTier;
        private final BigInteger providedCapacity;
        static final Capacitor[] VALUES = values();
        static final Capacitor[] VALUES_BY_TIER = Arrays.stream(values())
            .sorted(Comparator.comparingInt(Capacitor::getMinimalGlassTier))
            .toArray(Capacitor[]::new);

        Capacitor(int minimalGlassTier, BigInteger providedCapacity) {
            this.minimalGlassTier = minimalGlassTier;
            this.providedCapacity = providedCapacity;
        }

        public int getMinimalGlassTier() {
            return minimalGlassTier;
        }

        public BigInteger getProvidedCapacity() {
            return providedCapacity;
        }

        public static int getIndexFromGlassTier(int glassTier) {
            for (int index = 0; index < VALUES.length; index++) {
                if (VALUES[index].getMinimalGlassTier() == glassTier) {
                    return index;
                }
            }
            return -1;
        }
    }

    private static final String STRUCTURE_PIECE_BASE = "base";
    private static final String STRUCTURE_PIECE_LAYER = "slice";
    private static final String STRUCTURE_PIECE_TOP = "top";
    private static final String STRUCTURE_PIECE_MID = "mid";
    private static final int GLASS_TIER_UNSET = -2;

    private static final Block LSC_PART = Blocks.lscLapotronicEnergyUnit;
    private static final Item LSC_PART_ITEM = Item.getItemFromBlock(LSC_PART);
    private static final int CASING_META = 0;
    private static final int CASING_TEXTURE_ID = (42 << 7) | 127;

    private static final int DURATION_AVERAGE_TICKS = 100;

    // height channel for height.
    // glass channel for glass
    // capacitor channel for capacitor, but it really just pick whatever capacitor it can find in survival
    private static final IStructureDefinition<MTELapotronicSuperCapacitor> STRUCTURE_DEFINITION = IStructureDefinition
        .<MTELapotronicSuperCapacitor>builder()
        .addShape(
            STRUCTURE_PIECE_BASE,
            transpose(
                new String[][] { { "bbbbb", "bbbbb", "bbbbb", "bbbbb", "bbbbb", },
                    { "bb~bb", "bbbbb", "bbbbb", "bbbbb", "bbbbb", }, }))
        .addShape(
            STRUCTURE_PIECE_LAYER,
            transpose(new String[][] { { "ggggg", "gcccg", "gcccg", "gcccg", "ggggg", }, }))
        .addShape(STRUCTURE_PIECE_TOP, transpose(new String[][] { { "ggggg", "ggggg", "ggggg", "ggggg", "ggggg", }, }))
        .addShape(STRUCTURE_PIECE_MID, transpose(new String[][] { { "ggggg", "gCCCg", "gCCCg", "gCCCg", "ggggg", }, }))
        .addElement(
            'b',
            buildHatchAdder(
                MTELapotronicSuperCapacitor.class).atLeast(LSCHatchElement.Energy, LSCHatchElement.Dynamo, Maintenance)
                    .hatchItemFilterAnd(
                        (t, h) -> ChannelDataAccessor.getChannelData(h, "glass") < 6
                            ? filterByMTEClass(ImmutableList.of(MTEHatchEnergyTunnel.class, MTEHatchDynamoTunnel.class))
                                .negate()
                            : s -> true)
                    .casingIndex(CASING_TEXTURE_ID)
                    .dot(1)
                    .buildAndChain(onElementPass(te -> te.casingAmount++, ofBlock(LSC_PART, CASING_META))))
        .addElement(
            'g',
            withChannel(
                "glass",
                BorosilicateGlass
                    .ofBoroGlass((byte) GLASS_TIER_UNSET, (te, t) -> te.glassTier = t, te -> te.glassTier)))
        .addElement(
            'c',
            ofChain(
                onlyIf(
                    te -> te.topState != TopState.NotTop,
                    onElementPass(
                        te -> te.topState = TopState.Top,
                        withChannel(
                            "glass",
                            BorosilicateGlass.ofBoroGlass(
                                (byte) GLASS_TIER_UNSET,
                                (te, t) -> te.glassTier = t,
                                te -> te.glassTier)))),
                onlyIf(
                    te -> te.topState != TopState.Top,
                    onElementPass(
                        te -> te.topState = TopState.NotTop,
                        new IStructureElement<MTELapotronicSuperCapacitor>() {

                            @Override
                            public boolean check(MTELapotronicSuperCapacitor t, World world, int x, int y, int z) {
                                Block worldBlock = world.getBlock(x, y, z);
                                int meta = worldBlock.getDamageValue(world, x, y, z);
                                if (LSC_PART != worldBlock || meta == 0) return false;
                                t.capacitors[meta - 1]++;
                                return true;
                            }

                            private int getHint(ItemStack stack) {
                                return Capacitor.VALUES_BY_TIER[Math.min(
                                    Capacitor.VALUES_BY_TIER.length,
                                    ChannelDataAccessor.getChannelData(stack, "capacitor")) - 1].getMinimalGlassTier()
                                    + 1;
                            }

                            @Override
                            public boolean spawnHint(MTELapotronicSuperCapacitor t, World world, int x, int y, int z,
                                ItemStack trigger) {
                                StructureLibAPI.hintParticle(world, x, y, z, LSC_PART, getHint(trigger));
                                return true;
                            }

                            @Override
                            public boolean placeBlock(MTELapotronicSuperCapacitor t, World world, int x, int y, int z,
                                ItemStack trigger) {
                                world.setBlock(x, y, z, LSC_PART, getHint(trigger), 3);
                                return true;
                            }

                            @Override
                            public PlaceResult survivalPlaceBlock(MTELapotronicSuperCapacitor t, World world, int x,
                                int y, int z, ItemStack trigger, IItemSource source, EntityPlayerMP actor,
                                Consumer<IChatComponent> chatter) {
                                if (check(t, world, x, y, z)) return PlaceResult.SKIP;
                                int glassTier = ChannelDataAccessor.getChannelData(trigger, "glass") + 2;
                                ItemStack targetStack = source
                                    .takeOne(
                                        s -> s != null && s.stackSize >= 0
                                            && s.getItem() == LSC_PART_ITEM
                                            && Capacitor.VALUES[Math.min(s.getItemDamage(), Capacitor.VALUES.length)
                                                - 1].getMinimalGlassTier() > glassTier,
                                        true);
                                if (targetStack == null) return PlaceResult.REJECT;
                                return StructureUtility.survivalPlaceBlock(
                                    targetStack,
                                    NBTMode.EXACT,
                                    targetStack.stackTagCompound,
                                    true,
                                    world,
                                    x,
                                    y,
                                    z,
                                    source,
                                    actor,
                                    chatter);
                            }
                        }))))
        .addElement('C', ofBlock(LSC_PART, 1))
        .build();

    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    private final Set<MTEHatchEnergyMulti> mEnergyHatchesTT = new HashSet<>();
    private final Set<MTEHatchDynamoMulti> mDynamoHatchesTT = new HashSet<>();
    private final Set<MTEHatchEnergyTunnel> mEnergyTunnelsTT = new HashSet<>();
    private final Set<MTEHatchDynamoTunnel> mDynamoTunnelsTT = new HashSet<>();
    /**
     * Count the amount of capacitors of each tier in each slot. Index = meta - 1
     */
    private final int[] capacitors = new int[10];

    private BigInteger capacity = BigInteger.ZERO;
    private BigInteger stored = BigInteger.ZERO;
    private long passiveDischargeAmount = 0;
    private long inputLastTick = 0;
    private long outputLastTick = 0;
    private int repairStatusCache = 0;

    private byte glassTier = -1;
    private int casingAmount = 0;
    private TopState topState = TopState.MayBeTop;

    private long mMaxEUIn = 0;
    private long mMaxEUOut = 0;

    public MTELapotronicSuperCapacitor(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTELapotronicSuperCapacitor(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity var1) {
        return new MTELapotronicSuperCapacitor(super.mName);
    }

    @Override
    public IStructureDefinition<MTELapotronicSuperCapacitor> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    private void processInputHatch(MTEHatch aHatch, int aBaseCasingIndex) {
        mMaxEUIn += aHatch.maxEUInput() * aHatch.maxAmperesIn();
        aHatch.updateTexture(aBaseCasingIndex);
    }

    private void processOutputHatch(MTEHatch aHatch, int aBaseCasingIndex) {
        mMaxEUOut += aHatch.maxEUOutput() * aHatch.maxAmperesOut();
        aHatch.updateTexture(aBaseCasingIndex);
    }

    private boolean addBottomHatches(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null || aTileEntity.isDead()) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (!(aMetaTileEntity instanceof MTEHatch)) return false;
        if (aMetaTileEntity instanceof MTEHatchMaintenance) {
            ((MTEHatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            return MTELapotronicSuperCapacitor.this.mMaintenanceHatches.add((MTEHatchMaintenance) aMetaTileEntity);
        } else if (aMetaTileEntity instanceof MTEHatchEnergy) {
            // Add GT hatches
            final MTEHatchEnergy tHatch = ((MTEHatchEnergy) aMetaTileEntity);
            processInputHatch(tHatch, aBaseCasingIndex);
            return mEnergyHatches.add(tHatch);
        } else if (aMetaTileEntity instanceof MTEHatchEnergyTunnel) {
            // Add TT Laser hatches
            final MTEHatchEnergyTunnel tHatch = ((MTEHatchEnergyTunnel) aMetaTileEntity);
            processInputHatch(tHatch, aBaseCasingIndex);
            return mEnergyTunnelsTT.add(tHatch);
        } else if (aMetaTileEntity instanceof MTEHatchEnergyMulti) {
            // Add TT hatches
            final MTEHatchEnergyMulti tHatch = (MTEHatchEnergyMulti) aMetaTileEntity;
            processInputHatch(tHatch, aBaseCasingIndex);
            return mEnergyHatchesTT.add(tHatch);
        } else if (aMetaTileEntity instanceof MTEHatchDynamo) {
            // Add GT hatches
            final MTEHatchDynamo tDynamo = (MTEHatchDynamo) aMetaTileEntity;
            processOutputHatch(tDynamo, aBaseCasingIndex);
            return mDynamoHatches.add(tDynamo);
        } else if (aMetaTileEntity instanceof MTEHatchDynamoTunnel) {
            // Add TT Laser hatches
            final MTEHatchDynamoTunnel tDynamo = (MTEHatchDynamoTunnel) aMetaTileEntity;
            processOutputHatch(tDynamo, aBaseCasingIndex);
            return mDynamoTunnelsTT.add(tDynamo);
        } else if (aMetaTileEntity instanceof MTEHatchDynamoMulti) {
            // Add TT hatches
            final MTEHatchDynamoMulti tDynamo = (MTEHatchDynamoMulti) aMetaTileEntity;
            processOutputHatch(tDynamo, aBaseCasingIndex);
            return mDynamoHatchesTT.add(tDynamo);
        }
        return false;
    }

    private int getUHVCapacitorCount() {
        return capacitors[4];
    }

    private int getUEVCapacitorCount() {
        return capacitors[7];
    }

    private int getUIVCapacitorCount() {
        return capacitors[8];
    }

    private int getUMVCapacitorCount() {
        return capacitors[9];
    }

    private int wirelessCapableCapacitors() {
        return capacitors[4] + capacitors[7] + capacitors[8] + capacitors[9];
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Energy Storage, LSC")
            .addInfo("Loses energy equal to 1% of the total capacity every 24 hours.")
            .addInfo(
                "Capped at " + EnumChatFormatting.RED
                    + GTUtility.formatNumbers(max_passive_drain_eu_per_tick_per_uhv_cap)
                    + EnumChatFormatting.GRAY
                    + " EU/t passive loss per "
                    + GTValues.TIER_COLORS[9]
                    + GTValues.VN[9]
                    + EnumChatFormatting.GRAY
                    + " capacitor.")
            .addInfo(
                "The passive loss increases " + EnumChatFormatting.DARK_RED
                    + "100"
                    + EnumChatFormatting.GRAY
                    + "-fold"
                    + " for every capacitor tier above.")
            .addInfo("Passive loss is multiplied by the number of maintenance issues present.")
            .addSeparator()
            .addInfo("Glass shell has to be Tier - 3 of the highest capacitor tier.")
            .addTecTechHatchInfo()
            .addInfo(
                GTValues.TIER_COLORS[8] + GTValues.VN[8]
                    + EnumChatFormatting.GRAY
                    + "-tier glass required for "
                    + EnumChatFormatting.BLUE
                    + "Tec"
                    + EnumChatFormatting.DARK_BLUE
                    + "Tech"
                    + EnumChatFormatting.GRAY
                    + " Laser Hatches.")
            .addInfo("Add more or better capacitors to increase capacity.")
            .addSeparator()
            .addInfo("Wireless mode can be enabled by right clicking with a screwdriver.")
            .addInfo(
                "This mode can only be enabled if you have a " + GTValues.TIER_COLORS[9]
                    + GTValues.VN[9]
                    + EnumChatFormatting.GRAY
                    + "+ capacitor in the multiblock.")
            .addInfo(
                "When enabled every " + EnumChatFormatting.BLUE
                    + GTUtility
                        .formatNumbers(ItemBlockLapotronicEnergyUnit.LSC_time_between_wireless_rebalance_in_ticks)
                    + EnumChatFormatting.GRAY
                    + " ticks the LSC will attempt to re-balance against your")
            .addInfo("wireless EU network.")
            .addInfo(
                "If there is less than " + EnumChatFormatting.RED
                    + GTUtility.formatNumbers(ItemBlockLapotronicEnergyUnit.LSC_wireless_eu_cap)
                    + EnumChatFormatting.GRAY
                    + "("
                    + GTValues.TIER_COLORS[9]
                    + GTValues.VN[9]
                    + EnumChatFormatting.GRAY
                    + ") EU in the LSC")
            .addInfo("it will withdraw from the network and add to the LSC.")
            .addInfo("If there is more it will add the EU to the network and remove it from the LSC.")
            .addInfo(
                "The threshold increases " + EnumChatFormatting.DARK_RED
                    + "100"
                    + EnumChatFormatting.GRAY
                    + "-fold"
                    + " for every capacitor tier above.")
            .beginVariableStructureBlock(5, 5, 4, 50, 5, 5, false)
            .addStructureInfo("Modular height of 4-50 blocks.")
            .addController("Front center bottom")
            .addOtherStructurePart("Lapotronic Super Capacitor Casing", "5x2x5 base (at least 17x)")
            .addOtherStructurePart(
                "Lapotronic Capacitor (" + GTValues.TIER_COLORS[4]
                    + GTValues.VN[4]
                    + EnumChatFormatting.GRAY
                    + "-"
                    + GTValues.TIER_COLORS[8]
                    + GTValues.VN[8]
                    + EnumChatFormatting.GRAY
                    + "), Ultimate Capacitor ("
                    + GTValues.TIER_COLORS[9]
                    + GTValues.VN[9]
                    + EnumChatFormatting.GRAY
                    + "-"
                    + GTValues.TIER_COLORS[12]
                    + GTValues.VN[12]
                    + EnumChatFormatting.GRAY
                    + ")",
                "Center 3x(1-47)x3 above base (9-423 blocks)")
            .addStructureInfo(
                "You can also use the Empty Capacitor to save materials if you use it for less than half the blocks")
            .addOtherStructurePart("Borosilicate Glass (any)", "41-777x, Encase capacitor pillar")
            .addEnergyHatch("Any casing")
            .addDynamoHatch("Any casing")
            .addOtherStructurePart(
                "Laser Target/Source Hatches",
                "Any casing, must be using " + GTValues.TIER_COLORS[8]
                    + GTValues.VN[8]
                    + EnumChatFormatting.GRAY
                    + "-tier glass")
            .addStructureInfo("You can have several I/O Hatches")
            .addSubChannelUsage("glass", "Borosilicate Glass Tier")
            .addSubChannelUsage("capacitor", "Maximum Capacitor Tier")
            .addSubChannelUsage("height", "Height of structure")
            .addMaintenanceHatch("Any casing")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side,
        ForgeDirection forgeDirectionacing, int colorIndex, boolean aActive, boolean aRedstone) {
        ITexture[] sTexture = new ITexture[] {
            TextureFactory.of(BlockIcons.MACHINE_CASING_FUSION_GLASS, Dyes.getModulation(-1, Dyes._NULL.mRGBa)) };
        if (side == forgeDirectionacing && aActive) {
            sTexture = new ITexture[] { TextureFactory
                .of(BlockIcons.MACHINE_CASING_FUSION_GLASS_YELLOW, Dyes.getModulation(-1, Dyes._NULL.mRGBa)) };
        }
        return sTexture;
    }

    private UUID global_energy_user_uuid;

    @Override
    public void onFirstTick(IGregTechTileEntity tileEntity) {
        super.onFirstTick(tileEntity);

        if (!tileEntity.isServerSide()) return;

        global_energy_user_uuid = tileEntity.getOwnerUuid();
        SpaceProjectManager.checkOrCreateTeam(global_energy_user_uuid);
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack stack) {
        return true;
    }

    @Override
    public boolean checkRecipe(ItemStack stack) {
        this.mProgresstime = 1;
        this.mMaxProgresstime = 1;
        this.mEUt = 0;
        this.mEfficiencyIncrease = 10000;
        return true;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity thisController, ItemStack guiSlotItem) {
        // Reset capacitor counts
        Arrays.fill(capacitors, 0);
        // Clear TT hatches
        mEnergyHatchesTT.clear();
        mDynamoHatchesTT.clear();
        mEnergyTunnelsTT.clear();
        mDynamoTunnelsTT.clear();

        mMaxEUIn = 0;
        mMaxEUOut = 0;

        glassTier = GLASS_TIER_UNSET;
        casingAmount = 0;

        if (!checkPiece(STRUCTURE_PIECE_BASE, 2, 1, 0)) return false;

        if (casingAmount < 17) return false;

        topState = TopState.NotTop; // need at least one layer of capacitor to form, obviously
        int layer = 2;
        while (true) {
            if (!checkPiece(STRUCTURE_PIECE_LAYER, 2, layer, 0)) return false;
            layer++;
            if (topState == TopState.Top) break; // top found, break out
            topState = TopState.MayBeTop;
            if (layer > 50) return false; // too many layers
        }

        // Make sure glass tier is T-2 of the highest tier capacitor in the structure
        // Count down from the highest tier until an entry is found
        // Borosilicate glass after 5 are just recolours of 0
        for (int highestGlassTier = capacitors.length - 1; highestGlassTier >= 0; highestGlassTier--) {
            int highestCapacitor = Capacitor.getIndexFromGlassTier(highestGlassTier);
            if (capacitors[highestCapacitor] > 0) {
                if (Capacitor.VALUES[highestCapacitor].getMinimalGlassTier() > glassTier) return false;
                break;
            }
        }

        // Glass has to be at least UV-tier to allow TT Laser hatches
        if (glassTier < 8) {
            if (!mEnergyTunnelsTT.isEmpty() || !mDynamoTunnelsTT.isEmpty()) return false;
        }

        // Check if enough (more than 50%) non-empty caps
        if (capacitors[5] > capacitors[0] + capacitors[1]
            + capacitors[2]
            + capacitors[3]
            + getUHVCapacitorCount()
            + capacitors[6]
            + getUEVCapacitorCount()
            + getUIVCapacitorCount()
            + getUMVCapacitorCount()) return false;

        // Calculate total capacity
        capacity = BigInteger.ZERO;
        for (int i = 0; i < capacitors.length; i++) {
            int count = capacitors[i];
            capacity = capacity.add(
                Capacitor.VALUES[i].getProvidedCapacity()
                    .multiply(BigInteger.valueOf(count)));
        }
        // Calculate how much energy to void each tick
        passiveDischargeAmount = recalculateLossWithMaintenance(getRepairStatus());
        return mMaintenanceHatches.size() == 1;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        int layer = min(stackSize.stackSize + 3, 50);
        buildPiece(STRUCTURE_PIECE_BASE, stackSize, hintsOnly, 2, 1, 0);
        for (int i = 2; i < layer - 1; i++) buildPiece(STRUCTURE_PIECE_MID, stackSize, hintsOnly, 2, i, 0);
        buildPiece(STRUCTURE_PIECE_TOP, stackSize, hintsOnly, 2, layer - 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, IItemSource source, EntityPlayerMP actor) {
        if (mMachine) return -1;
        int layer = Math.min(ChannelDataAccessor.getChannelData(stackSize, "height") + 3, 50);
        int built;
        built = survivialBuildPiece(
            STRUCTURE_PIECE_BASE,
            stackSize,
            2,
            1,
            0,
            elementBudget,
            source,
            actor,
            false,
            true);
        if (built >= 0) return built;
        for (int i = 2; i < layer - 1; i++) built = survivialBuildPiece(
            STRUCTURE_PIECE_MID,
            stackSize,
            2,
            i,
            0,
            elementBudget,
            source,
            actor,
            false,
            true);
        if (built >= 0) return built;
        return survivialBuildPiece(
            STRUCTURE_PIECE_TOP,
            stackSize,
            2,
            layer - 1,
            0,
            elementBudget,
            source,
            actor,
            false,
            true);
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        // Reset I/O cache
        inputLastTick = 0;
        outputLastTick = 0;

        long temp_stored = 0L;

        // Draw energy from GT hatches
        for (MTEHatchEnergy eHatch : super.mEnergyHatches) {
            if (eHatch == null || !eHatch.isValid()) {
                continue;
            }
            final long power = getPowerToDraw(eHatch.maxEUInput() * eHatch.maxAmperesIn());
            if (eHatch.getEUVar() >= power) {
                eHatch.setEUVar(eHatch.getEUVar() - power);
                temp_stored += power;
                inputLastTick += power;
            }
        }

        // Output energy to GT hatches
        for (MTEHatchDynamo eDynamo : super.mDynamoHatches) {
            if (eDynamo == null || !eDynamo.isValid()) {
                continue;
            }
            final long power = getPowerToPush(eDynamo.maxEUOutput() * eDynamo.maxAmperesOut());
            if (power <= eDynamo.maxEUStore() - eDynamo.getEUVar()) {
                eDynamo.setEUVar(eDynamo.getEUVar() + power);
                temp_stored -= power;
                outputLastTick += power;
            }
        }

        // Draw energy from TT hatches
        for (MTEHatchEnergyMulti eHatch : mEnergyHatchesTT) {
            if (eHatch == null || !eHatch.isValid()) {
                continue;
            }
            final long power = getPowerToDraw(eHatch.maxEUInput() * eHatch.maxAmperesIn());
            if (eHatch.getEUVar() >= power) {
                eHatch.setEUVar(eHatch.getEUVar() - power);
                temp_stored += power;
                inputLastTick += power;
            }
        }

        // Output energy to TT hatches
        for (MTEHatchDynamoMulti eDynamo : mDynamoHatchesTT) {
            if (eDynamo == null || !eDynamo.isValid()) {
                continue;
            }
            final long power = getPowerToPush(eDynamo.maxEUOutput() * eDynamo.maxAmperesOut());
            if (power <= eDynamo.maxEUStore() - eDynamo.getEUVar()) {
                eDynamo.setEUVar(eDynamo.getEUVar() + power);
                temp_stored -= power;
                outputLastTick += power;
            }
        }

        // Draw energy from TT Laser hatches
        for (MTEHatchEnergyTunnel eHatch : mEnergyTunnelsTT) {
            if (eHatch == null || !eHatch.isValid()) {
                continue;
            }
            final long ttLaserWattage = eHatch.maxEUInput() * eHatch.Amperes - (eHatch.Amperes / 20);
            final long power = getPowerToDraw(ttLaserWattage);
            if (eHatch.getEUVar() >= power) {
                eHatch.setEUVar(eHatch.getEUVar() - power);
                temp_stored += power;
                inputLastTick += power;
            }
        }

        // Output energy to TT Laser hatches
        for (MTEHatchDynamoTunnel eDynamo : mDynamoTunnelsTT) {
            if (eDynamo == null || !eDynamo.isValid()) {
                continue;
            }
            final long ttLaserWattage = eDynamo.maxEUOutput() * eDynamo.Amperes - (eDynamo.Amperes / 20);
            final long power = getPowerToPush(ttLaserWattage);
            if (power <= eDynamo.maxEUStore() - eDynamo.getEUVar()) {
                eDynamo.setEUVar(eDynamo.getEUVar() + power);
                temp_stored -= power;
                outputLastTick += power;
            }
        }

        if (wirelessCapableCapacitors() <= 0) {
            wireless_mode = false;
        }

        // Every LSC_time_between_wireless_rebalance_in_ticks check against wireless network for re-balancing.
        counter++;
        if (wireless_mode && (counter >= ItemBlockLapotronicEnergyUnit.LSC_time_between_wireless_rebalance_in_ticks)) {

            // Reset tick counter.
            counter = rebalance();
        }

        // Lose some energy.
        // Re-calculate if the repair status changed.
        if (super.getRepairStatus() != repairStatusCache) {
            passiveDischargeAmount = recalculateLossWithMaintenance(super.getRepairStatus());
        }

        // This will break if you transfer more than 2^63 EU/t, so don't do that. Thanks <3
        temp_stored -= passiveDischargeAmount;
        stored = stored.add(BigInteger.valueOf(temp_stored));

        // Check that the machine has positive EU stored.
        stored = (stored.compareTo(BigInteger.ZERO) <= 0) ? BigInteger.ZERO : stored;

        IGregTechTileEntity tBMTE = this.getBaseMetaTileEntity();

        tBMTE.injectEnergyUnits(ForgeDirection.UNKNOWN, inputLastTick, 1L);
        tBMTE.drainEnergyUnits(ForgeDirection.UNKNOWN, outputLastTick, 1L);

        // Add I/O values to Queues
        if (energyInputValues.size() > DURATION_AVERAGE_TICKS) {
            energyInputValues.remove();
        }
        energyInputValues.offer(inputLastTick);

        if (energyOutputValues.size() > DURATION_AVERAGE_TICKS) {
            energyOutputValues.remove();
        }

        energyOutputValues.offer(outputLastTick);

        // Add I/O values to Queues 5 min
        if (energyInputValues5m.size() > 6000) {
            energyInputValues5m.remove();
        }
        energyInputValues5m.offer(inputLastTick);

        if (energyOutputValues5m.size() > 6000) {
            energyOutputValues5m.remove();
        }

        energyOutputValues5m.offer(outputLastTick);

        // Add I/O values to Queues 1 hour
        if (energyInputValues1h.size() > 72000) {
            energyInputValues1h.remove();
        }
        energyInputValues1h.offer(inputLastTick);

        if (energyOutputValues1h.size() > 72000) {
            energyOutputValues1h.remove();
        }

        energyOutputValues1h.offer(outputLastTick);

        return true;
    }

    private int rebalance() {

        balanced = true;

        // Find difference.
        BigInteger transferred_eu = stored.subtract(
            (ItemBlockLapotronicEnergyUnit.LSC_wireless_eu_cap.multiply(BigInteger.valueOf(getUHVCapacitorCount())))
                .add(
                    ItemBlockLapotronicEnergyUnit.UEV_wireless_eu_cap
                        .multiply(BigInteger.valueOf(getUEVCapacitorCount())))
                .add(
                    ItemBlockLapotronicEnergyUnit.UIV_wireless_eu_cap
                        .multiply(BigInteger.valueOf(getUIVCapacitorCount())))
                .add(
                    ItemBlockLapotronicEnergyUnit.UMV_wireless_eu_cap
                        .multiply(BigInteger.valueOf(getUMVCapacitorCount()))));

        if (transferred_eu.signum() == -1) {
            inputLastTick += Math.abs(transferred_eu.longValue());
        } else {
            outputLastTick += transferred_eu.longValue();
        }

        // If that difference can be added then do so.
        if (WirelessNetworkManager.addEUToGlobalEnergyMap(global_energy_user_uuid, transferred_eu)) {
            // If it succeeds there was sufficient energy so set the internal capacity as such.
            stored = ItemBlockLapotronicEnergyUnit.LSC_wireless_eu_cap
                .multiply(BigInteger.valueOf(getUHVCapacitorCount()))
                .add(
                    ItemBlockLapotronicEnergyUnit.UEV_wireless_eu_cap
                        .multiply(BigInteger.valueOf(getUEVCapacitorCount()))
                        .add(
                            ItemBlockLapotronicEnergyUnit.UIV_wireless_eu_cap
                                .multiply(BigInteger.valueOf(getUIVCapacitorCount())))
                        .add(
                            ItemBlockLapotronicEnergyUnit.UMV_wireless_eu_cap
                                .multiply(BigInteger.valueOf(getUMVCapacitorCount()))));
        }

        return 1;
    }

    /**
     * To be called whenever the maintenance status changes or the capacity was recalculated
     *
     * @param repairStatus This machine's repair status
     * @return new BigInteger instance for passiveDischargeAmount
     */
    private long recalculateLossWithMaintenance(int repairStatus) {
        repairStatusCache = repairStatus;

        long temp_capacity_divided = 0;

        if (wirelessCapableCapacitors() == 0) {
            temp_capacity_divided = capacity.divide(BigInteger.valueOf(100L * 86400L * 20L))
                .longValue();
        }

        // Passive loss is multiplied by number of UHV+ caps. Minimum of 1 otherwise loss is 0 for non-UHV+ caps
        // calculations.
        if (wirelessCapableCapacitors() != 0) {
            temp_capacity_divided = getUHVCapacitorCount() * max_passive_drain_eu_per_tick_per_uhv_cap
                + getUEVCapacitorCount() * max_passive_drain_eu_per_tick_per_uev_cap
                + getUIVCapacitorCount() * max_passive_drain_eu_per_tick_per_uiv_cap
                + getUMVCapacitorCount() * max_passive_drain_eu_per_tick_per_umv_cap;
        }

        // Passive loss is multiplied by number of maintenance issues.
        // Maximum of 100,000 EU/t drained per UHV cell. The logic is 1% of EU capacity should be drained every 86400
        // seconds (1 day).
        return temp_capacity_divided * (getIdealStatus() - repairStatus + 1);
    }

    /**
     * Calculate how much EU to draw from an Energy Hatch
     *
     * @param hatchWatts Hatch amperage * voltage
     * @return EU amount
     */
    private long getPowerToDraw(long hatchWatts) {
        final BigInteger remcapActual = capacity.subtract(stored);
        final BigInteger recampLimited = (MAX_LONG.compareTo(remcapActual) > 0) ? remcapActual : MAX_LONG;
        return min(hatchWatts, recampLimited.longValue());
    }

    /**
     * Calculate how much EU to push into a Dynamo Hatch
     *
     * @param hatchWatts Hatch amperage * voltage
     * @return EU amount
     */
    private long getPowerToPush(long hatchWatts) {
        final BigInteger remStoredLimited = (MAX_LONG.compareTo(stored) > 0) ? stored : MAX_LONG;
        return min(hatchWatts, remStoredLimited.longValue());
    }

    private long getAvgIn() {
        long sum = 0L;
        for (long l : energyInputValues) {
            sum += l;
        }
        return sum / Math.max(energyInputValues.size(), 1);
    }

    private long getAvgOut() {
        long sum = 0L;
        for (long l : energyOutputValues) {
            sum += l;
        }
        return sum / Math.max(energyOutputValues.size(), 1);
    }

    private long getAvgIn5m() {
        double sum = 0;
        for (long l : energyInputValues5m) {
            sum += l;
        }
        return (long) sum / Math.max(energyInputValues5m.size(), 1);
    }

    private long getAvgOut5m() {
        double sum = 0;
        for (long l : energyOutputValues5m) {
            sum += l;
        }
        return (long) sum / Math.max(energyOutputValues5m.size(), 1);
    }

    private long getAvgIn1h() {
        double sum = 0;
        for (long l : energyInputValues1h) {
            sum += l;
        }
        return (long) sum / Math.max(energyInputValues1h.size(), 1);
    }

    private long getAvgOut1h() {
        double sum = 0;
        for (long l : energyOutputValues1h) {
            sum += l;
        }
        return (long) sum / Math.max(energyOutputValues1h.size(), 1);
    }

    private String getTimeTo() {
        double avgIn = getAvgIn();
        double avgOut = getAvgOut();
        double passLoss = passiveDischargeAmount;
        double cap = capacity.doubleValue();
        double sto = stored.doubleValue();
        if (avgIn >= avgOut + passLoss) {
            // Calculate time to full if charging
            if (avgIn - passLoss > 0) {
                double timeToFull = (cap - sto) / (avgIn - (passLoss + avgOut)) / 20;
                return "Time to Full: " + formatTime(timeToFull, true);
            }
            return "Time to Something: Infinity years";
        } else {
            // Calculate time to empty if discharging
            double timeToEmpty = sto / ((avgOut + passLoss) - avgIn) / 20;
            return "Time to Empty: " + formatTime(timeToEmpty, false);
        }
    }

    private String getCapacityCache() {
        return capacity.compareTo(guiCapacityStoredReformatLimit) > 0 ? standardFormat.format(capacity)
            : numberFormat.format(capacity);
    }

    private String getStoredCache() {
        return stored.compareTo(guiCapacityStoredReformatLimit) > 0 ? standardFormat.format(stored)
            : numberFormat.format(stored);
    }

    private String getUsedPercentCache() {
        return toPercentageFrom(stored, capacity);
    }

    private String getWirelessStoredCache() {
        return standardFormat.format(WirelessNetworkManager.getUserEU(global_energy_user_uuid));
    }

    private boolean isActiveCache() {
        return getBaseMetaTileEntity().isActive();
    }

    private String getPassiveDischargeAmountCache() {
        return passiveDischargeAmount > 100_000_000_000L ? standardFormat.format(passiveDischargeAmount)
            : numberFormat.format(passiveDischargeAmount);
    }

    @Override
    public String[] getInfoData() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        int secInterval = DURATION_AVERAGE_TICKS / 20;

        final ArrayList<String> ll = new ArrayList<>();
        ll.add(EnumChatFormatting.YELLOW + "Operational Data:" + EnumChatFormatting.RESET);
        ll.add("EU Stored: " + nf.format(stored) + " EU");
        ll.add("EU Stored: " + toStandardForm(stored) + " EU");
        ll.add("Used Capacity: " + toPercentageFrom(stored, capacity));
        ll.add("Total Capacity: " + nf.format(capacity) + " EU");
        ll.add("Total Capacity: " + toStandardForm(capacity) + " EU");
        ll.add("Passive Loss: " + nf.format(passiveDischargeAmount) + " EU/t");
        ll.add("EU IN: " + GTUtility.formatNumbers(inputLastTick) + " EU/t");
        ll.add("EU OUT: " + GTUtility.formatNumbers(outputLastTick) + " EU/t");
        ll.add("Avg EU IN: " + nf.format(getAvgIn()) + " (last " + secInterval + " seconds)");
        ll.add("Avg EU OUT: " + nf.format(getAvgOut()) + " (last " + secInterval + " seconds)");
        ll.add("Avg EU IN: " + nf.format(getAvgIn5m()) + " (last " + 5 + " minutes)");
        ll.add("Avg EU OUT: " + nf.format(getAvgOut5m()) + " (last " + 5 + " minutes)");
        ll.add("Avg EU IN: " + nf.format(getAvgIn1h()) + " (last " + 1 + " hour)");
        ll.add("Avg EU OUT: " + nf.format(getAvgOut1h()) + " (last " + 1 + " hour)");

        ll.add(getTimeTo());

        ll.add(
            "Maintenance Status: " + ((super.getRepairStatus() == super.getIdealStatus())
                ? EnumChatFormatting.GREEN + "Working perfectly" + EnumChatFormatting.RESET
                : EnumChatFormatting.RED + "Has Problems" + EnumChatFormatting.RESET));
        ll.add(
            "Wireless mode: " + (wireless_mode ? EnumChatFormatting.GREEN + "enabled" + EnumChatFormatting.RESET
                : EnumChatFormatting.RED + "disabled" + EnumChatFormatting.RESET));
        ll.add(
            GTValues.TIER_COLORS[9] + GTValues.VN[9]
                + EnumChatFormatting.RESET
                + " Capacitors detected: "
                + getUHVCapacitorCount());
        ll.add(
            GTValues.TIER_COLORS[10] + GTValues.VN[10]
                + EnumChatFormatting.RESET
                + " Capacitors detected: "
                + getUEVCapacitorCount());
        ll.add(
            GTValues.TIER_COLORS[11] + GTValues.VN[11]
                + EnumChatFormatting.RESET
                + " Capacitors detected: "
                + getUIVCapacitorCount());
        ll.add(
            GTValues.TIER_COLORS[12] + GTValues.VN[12]
                + EnumChatFormatting.RESET
                + " Capacitors detected: "
                + getUMVCapacitorCount());
        ll.add(
            "Total wireless EU: " + EnumChatFormatting.RED
                + nf.format(WirelessNetworkManager.getUserEU(global_energy_user_uuid))
                + " EU");
        ll.add(
            "Total wireless EU: " + EnumChatFormatting.RED
                + toStandardForm(WirelessNetworkManager.getUserEU(global_energy_user_uuid))
                + " EU");

        final String[] a = new String[ll.size()];
        return ll.toArray(a);
    }

    protected static DecimalFormat standardFormat;

    static {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        dfs.setExponentSeparator("e");
        standardFormat = new DecimalFormat("0.00E0", dfs);
    }

    protected String capacityCache = "";
    protected String storedEUCache = "";
    protected String usedPercentCache = "";
    protected String passiveDischargeAmountCache = "";
    protected String wirelessStoreCache = "";
    protected long avgInCache;
    protected long avgOutCache;
    protected String timeToCache = "";
    protected boolean isActiveCache;

    protected void drawTexts(DynamicPositionedColumn screenElements, SlotWidget inventorySlot) {
        screenElements.setSynced(false)
            .setSpace(0)
            .setPos(10, 7);
        screenElements
            .widget(
                new TextWidget(GTUtility.trans("132", "Pipe is loose.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> !mWrench))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mWrench, val -> mWrench = val));
        screenElements
            .widget(
                new TextWidget(GTUtility.trans("133", "Screws are loose.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> !mScrewdriver))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mScrewdriver, val -> mScrewdriver = val));
        screenElements
            .widget(
                new TextWidget(GTUtility.trans("134", "Something is stuck.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> !mSoftHammer))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mSoftHammer, val -> mSoftHammer = val));
        screenElements
            .widget(
                new TextWidget(GTUtility.trans("135", "Platings are dented.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> !mHardHammer))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mHardHammer, val -> mHardHammer = val));
        screenElements
            .widget(
                new TextWidget(GTUtility.trans("136", "Circuitry burned out.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> !mSolderingTool))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mSolderingTool, val -> mSolderingTool = val));
        screenElements.widget(
            new TextWidget(GTUtility.trans("137", "That doesn't belong there.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                .setEnabled(widget -> !mCrowbar))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mCrowbar, val -> mCrowbar = val));
        screenElements
            .widget(
                new TextWidget(GTUtility.trans("138", "Incomplete Structure.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> !mMachine))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> mMachine, val -> mMachine = val));

        screenElements.widget(
            new TextWidget(GTUtility.trans("139", "Hit with Soft Mallet")).setDefaultColor(COLOR_TEXT_WHITE.get())
                .setEnabled(
                    widget -> getBaseMetaTileEntity().getErrorDisplayID() == 0 && !getBaseMetaTileEntity().isActive()))
            .widget(
                new FakeSyncWidget.IntegerSyncer(
                    () -> getBaseMetaTileEntity().getErrorDisplayID(),
                    val -> getBaseMetaTileEntity().setErrorDisplayID(val)))
            .widget(
                new FakeSyncWidget.BooleanSyncer(
                    () -> getBaseMetaTileEntity().isActive(),
                    val -> getBaseMetaTileEntity().setActive(val)));
        screenElements.widget(
            new TextWidget(GTUtility.trans("140", "to (re-)start the Machine")).setDefaultColor(COLOR_TEXT_WHITE.get())
                .setEnabled(
                    widget -> getBaseMetaTileEntity().getErrorDisplayID() == 0 && !getBaseMetaTileEntity().isActive()));
        screenElements.widget(
            new TextWidget(GTUtility.trans("141", "if it doesn't start.")).setDefaultColor(COLOR_TEXT_WHITE.get())
                .setEnabled(
                    widget -> getBaseMetaTileEntity().getErrorDisplayID() == 0 && !getBaseMetaTileEntity().isActive()));

        screenElements.widget(TextWidget.dynamicString(() -> {
            Duration time = Duration.ofSeconds((mTotalRunTime - mLastWorkingTick) / 20);
            return StatCollector.translateToLocalFormatted(
                "GT5U.gui.text.shutdown_duration",
                time.toHours(),
                time.toMinutes() % 60,
                time.getSeconds() % 60);
        })
            .setEnabled(
                widget -> shouldDisplayShutDownReason() && !getBaseMetaTileEntity().isActive()
                    && getBaseMetaTileEntity().wasShutdown()))
            .widget(new FakeSyncWidget.LongSyncer(() -> mTotalRunTime, time -> mTotalRunTime = time))
            .widget(new FakeSyncWidget.LongSyncer(() -> mLastWorkingTick, time -> mLastWorkingTick = time));
        screenElements.widget(
            TextWidget.dynamicString(
                () -> getBaseMetaTileEntity().getLastShutDownReason()
                    .getDisplayString())
                .setSynced(false)
                .setTextAlignment(Alignment.CenterLeft)
                .setEnabled(
                    widget -> shouldDisplayShutDownReason() && !getBaseMetaTileEntity().isActive()
                        && GTUtility.isStringValid(
                            getBaseMetaTileEntity().getLastShutDownReason()
                                .getDisplayString())
                        && getBaseMetaTileEntity().wasShutdown()))
            .widget(
                new ShutDownReasonSyncer(
                    () -> getBaseMetaTileEntity().getLastShutDownReason(),
                    reason -> getBaseMetaTileEntity().setShutDownReason(reason)))
            .widget(
                new FakeSyncWidget.BooleanSyncer(
                    () -> getBaseMetaTileEntity().wasShutdown(),
                    wasShutDown -> getBaseMetaTileEntity().setShutdownStatus(wasShutDown)));
        screenElements.widget(
            new TextWidget().setStringSupplier(
                () -> "Total Capacity: " + EnumChatFormatting.BLUE + capacityCache + EnumChatFormatting.WHITE + " EU")
                .setDefaultColor(COLOR_TEXT_WHITE.get())
                .setEnabled(widget -> isActiveCache))
            .widget(new FakeSyncWidget.StringSyncer(this::getCapacityCache, val -> capacityCache = val))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> "Stored: " + EnumChatFormatting.RED + storedEUCache + EnumChatFormatting.WHITE + " EU")
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> isActiveCache))
            .widget(new FakeSyncWidget.StringSyncer(this::getStoredCache, val -> storedEUCache = val))
            .widget(
                new TextWidget().setStringSupplier(() -> "Used capacity: " + EnumChatFormatting.RED + usedPercentCache)
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> isActiveCache))
            .widget(new FakeSyncWidget.StringSyncer(this::getUsedPercentCache, val -> usedPercentCache = val))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> "Passive Loss: " + EnumChatFormatting.RED
                            + passiveDischargeAmountCache
                            + EnumChatFormatting.WHITE
                            + " EU/t")
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> isActiveCache))
            .widget(
                new FakeSyncWidget.StringSyncer(
                    this::getPassiveDischargeAmountCache,
                    val -> passiveDischargeAmountCache = val))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> "Avg EU IN: " + EnumChatFormatting.GREEN
                            + (avgInCache > 100_000_000_000L ? standardFormat.format(avgInCache)
                                : numberFormat.format(avgInCache))
                            + EnumChatFormatting.WHITE
                            + " last 5s")
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> isActiveCache))
            .widget(new FakeSyncWidget.LongSyncer(this::getAvgIn, val -> avgInCache = val))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> "Avg EU OUT: " + EnumChatFormatting.RED
                            + (avgOutCache > 100_000_000_000L ? standardFormat.format(avgOutCache)
                                : numberFormat.format(avgOutCache))
                            + EnumChatFormatting.WHITE
                            + " last 5s")
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> isActiveCache))
            .widget(new FakeSyncWidget.LongSyncer(this::getAvgOut, val -> avgOutCache = val))
            .widget(
                new TextWidget().setStringSupplier(() -> EnumChatFormatting.WHITE + timeToCache)
                    .setEnabled(widget -> isActiveCache))
            .widget(new FakeSyncWidget.StringSyncer(this::getTimeTo, val -> timeToCache = val))
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> "Total wireless EU: " + EnumChatFormatting.BLUE
                            + wirelessStoreCache
                            + EnumChatFormatting.WHITE
                            + " EU")
                    .setDefaultColor(COLOR_TEXT_WHITE.get())
                    .setEnabled(widget -> isActiveCache))
            .widget(new FakeSyncWidget.StringSyncer(this::getWirelessStoredCache, val -> wirelessStoreCache = val))
            .widget(new FakeSyncWidget.BooleanSyncer(this::isActiveCache, val -> isActiveCache = val));
    }

    // Method to format time in seconds, minutes, days, and years
    private String formatTime(double time, boolean fill) {
        if (time < 1) {
            return "Completely " + (fill ? "full" : "empty");
        } else if (time < 60) {
            return String.format("%.2f seconds", time);
        } else if (time < 3600) {
            return String.format("%.2f minutes", time / 60);
        } else if (time < 86400) {
            return String.format("%.2f hours", time / 3600);
        } else if (time < 31536000) {
            return String.format("%.2f days", time / 86400);
        } else {
            double y = time / 31536000;
            return y < 9_000 ? String.format("%.2f years", y) : "Over9000 years";
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound nbt) {
        nbt = (nbt == null) ? new NBTTagCompound() : nbt;

        nbt.setByteArray("capacity", capacity.toByteArray());
        nbt.setByteArray("stored", stored.toByteArray());
        nbt.setBoolean("wireless_mode", wireless_mode);
        nbt.setInteger("wireless_mode_cooldown", counter);

        super.saveNBTData(nbt);
    }

    @Override
    public void loadNBTData(NBTTagCompound nbt) {
        nbt = (nbt == null) ? new NBTTagCompound() : nbt;

        capacity = new BigInteger(nbt.getByteArray("capacity"));
        stored = new BigInteger(nbt.getByteArray("stored"));
        wireless_mode = nbt.getBoolean("wireless_mode");
        counter = nbt.getInteger("wireless_mode_cooldown");

        super.loadNBTData(nbt);
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public int getMaxEfficiency(ItemStack stack) {
        return 10000;
    }

    @Override
    public int getPollutionPerTick(ItemStack stack) {
        return 0;
    }

    @Override
    public int getDamageToComponent(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack stack) {
        return false;
    }

    // called by the getEUCapacity() function in BaseMetaTileEntity
    @Override
    public long maxEUStore() {
        return capacity.longValue();
    }

    // called by the getEUStored() function in BaseMetaTileEntity
    @Override
    public long getEUVar() {
        return stored.longValue();
    }

    /*
     * all of these are needed for the injectEnergyUnits() and drainEnergyUnits() in IGregTechTileEntity
     */
    @Override
    public long maxEUInput() {
        if (wireless_mode) {
            return Long.MAX_VALUE;
        } else {
            return mMaxEUIn;
        }
    }

    @Override
    public long maxAmperesIn() {
        return 1L;
    }

    @Override
    public long maxEUOutput() {
        if (wireless_mode) {
            return Long.MAX_VALUE;
        } else {
            return mMaxEUOut;
        }
    }

    @Override
    public long maxAmperesOut() {
        return 1L;
    }

    @Override
    public boolean isEnetInput() {
        return true;
    }

    @Override
    public boolean isEnetOutput() {
        return true;
    }

    protected boolean canUseWireless() {
        return wirelessCapableCapacitors() != 0;
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        if (canUseWireless()) {
            wireless_mode = !wireless_mode;
            GTUtility.sendChatToPlayer(aPlayer, "Wireless network mode " + (wireless_mode ? "enabled." : "disabled."));
        } else {
            GTUtility.sendChatToPlayer(
                aPlayer,
                "Wireless mode cannot be enabled without at least 1 " + GTValues.TIER_COLORS[9]
                    + GTValues.VN[9]
                    + EnumChatFormatting.RESET
                    + "+ capacitor.");
            wireless_mode = false;
        }
    }

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        super.addUIWidgets(builder, buildContext);
        builder.widget(new ButtonWidget().setOnClick((clickData, widget) -> {
            if (!widget.isClient()) {
                canUseWireless = canUseWireless();
            }
            if (canUseWireless) {
                wireless_mode = !wireless_mode;
            }
        })
            .setPlayClickSound(true)
            .setBackground(() -> {
                List<UITexture> ret = new ArrayList<>();
                ret.add(GTUITextures.BUTTON_STANDARD);
                if (canUseWireless) {
                    if (wireless_mode) {
                        ret.add(KTUITextures.OVERLAY_BUTTON_WIRELESS_ON);
                    } else {
                        ret.add(KTUITextures.OVERLAY_BUTTON_WIRELESS_OFF);
                    }
                } else {
                    ret.add(KTUITextures.OVERLAY_BUTTON_WIRELESS_OFF_DISABLED);
                }
                return ret.toArray(new IDrawable[0]);
            })
            .setPos(80, 91)
            .setSize(16, 16)
            .addTooltip(StatCollector.translateToLocal("gui.kekztech_lapotronicenergyunit.wireless"))
            .setTooltipShowUpDelay(TOOLTIP_DELAY))
            .widget(new FakeSyncWidget.BooleanSyncer(() -> wireless_mode, val -> wireless_mode = val))
            .widget(new FakeSyncWidget.BooleanSyncer(this::canUseWireless, val -> canUseWireless = val))
            .widget(new ButtonWidget().setOnClick((clickData, widget) -> {
                if (mMachine && wireless_mode && canUseWireless && !balanced) {
                    counter = rebalance();
                }
            })
                .setPlayClickSound(true)
                .setBackground(() -> {
                    List<UITexture> ret = new ArrayList<>();
                    ret.add(GTUITextures.BUTTON_STANDARD);
                    ret.add(KTUITextures.OVERLAY_BUTTON_WIRELESS_REBALANCE);
                    return ret.toArray(new IDrawable[0]);
                })
                .setPos(98, 91)
                .setSize(16, 16)
                .setEnabled((widget) -> wireless_mode && canUseWireless && !balanced)
                .addTooltip(StatCollector.translateToLocal("gui.kekztech_lapotronicenergyunit.wireless_rebalance"))
                .setTooltipShowUpDelay(TOOLTIP_DELAY));
    }

    private enum LSCHatchElement implements IHatchElement<MTELapotronicSuperCapacitor> {

        Energy(MTEHatchEnergyMulti.class, MTEHatchEnergy.class) {

            @Override
            public long count(MTELapotronicSuperCapacitor t) {
                return t.mEnergyHatches.size() + t.mEnergyHatchesTT.size() + t.mEnergyTunnelsTT.size();
            }
        },
        Dynamo(MTEHatchDynamoMulti.class, MTEHatchDynamo.class) {

            @Override
            public long count(MTELapotronicSuperCapacitor t) {
                return t.mDynamoHatches.size() + t.mDynamoHatchesTT.size() + t.mDynamoTunnelsTT.size();
            }
        },;

        private final List<? extends Class<? extends IMetaTileEntity>> mteClasses;

        @SafeVarargs
        LSCHatchElement(Class<? extends IMetaTileEntity>... mteClasses) {
            this.mteClasses = Arrays.asList(mteClasses);
        }

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return mteClasses;
        }

        @Override
        public IGTHatchAdder<? super MTELapotronicSuperCapacitor> adder() {
            return MTELapotronicSuperCapacitor::addBottomHatches;
        }
    }
}
