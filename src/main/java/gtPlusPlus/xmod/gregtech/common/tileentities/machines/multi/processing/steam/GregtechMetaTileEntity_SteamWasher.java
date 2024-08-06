package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.processing.steam;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.GregTech_API.*;
import static gregtech.api.enums.GT_HatchElement.InputHatch;
import static gregtech.api.enums.GT_Values.AuthorEvgenWarGold;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.recipe.metadata.PCBFactoryTierKey;
import gregtech.api.recipe.metadata.PCBFactoryUpgrade;
import gregtech.api.recipe.metadata.PCBFactoryUpgradeKey;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.VoidProtectionHelper;
import gtPlusPlus.core.util.minecraft.FluidUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import net.minecraftforge.fluids.FluidStack;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTech_API;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.gui.modularui.GT_UITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_OverclockCalculator;
import gregtech.api.util.GT_Recipe;
import gregtech.common.blocks.GT_Block_Casings1;
import gregtech.common.blocks.GT_Block_Casings2;
import gtPlusPlus.api.recipe.GTPPRecipeMaps;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_SteamMultiBase;
import ic2.core.init.BlocksItems;
import ic2.core.init.InternalName;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class GregtechMetaTileEntity_SteamWasher extends GregtechMeta_SteamMultiBase<GregtechMetaTileEntity_SteamWasher>
    implements ISurvivalConstructable {

    public GregtechMetaTileEntity_SteamWasher(String aName) {
        super(aName);
    }

    public GregtechMetaTileEntity_SteamWasher(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GregtechMetaTileEntity_SteamWasher(this.mName);
    }

    @Override
    public String getMachineType() {
        return "Washer";
    }

    private static final String STRUCTUR_PIECE_MAIN = "main";

    private IStructureDefinition<GregtechMetaTileEntity_SteamWasher> STRUCTURE_DEFINITION = null;

    private final String[][] shape = new String[][] {
        { "         ", "         ", " CCCCCC  ", "         ", "         " },
        { "         ", "         ", " C    C  ", "         ", "         " },
        { "     AAA ", "    A   A", " C  A C A", "    A   A", "     AAA " },
        { "    ADDDA", "AAA D   D", "AAA D C D", "AAA D   D", "    ADDDA" },
        { "    ADDDA", "A~A DEEED", "AAA DECED", "AAA DEEED", "    ADDDA" },
        { "    AAAAA", "AAA ABBBA", "AAA ABABA", "AAA ABBBA", "    AAAAA" } };

    private static final int HORIZONTAL_OFF_SET = 1;
    private static final int VERTICAL_OFF_SET = 4;
    private static final int DEPTH_OFF_SET = 1;

    private static final int MACHINEMODE_OREWASH = 0;
    private static final int MACHINEMODE_SIMPLEWASH = 1;
    private int amountWaterBlock = 0;

    private boolean isBroken = true;

    private int tierGearBoxCasing = -1;
    private int tierPipeCasing = -1;
    private int tierMachineCasing = -1;
    private int tierMachine = 1;

    private int tCountCasing = 0;

    public int getTierMachineCasing(Block block, int meta) {
        if (block == sBlockCasings1 && 10 == meta) {
            tCountCasing++;
            return 1;
        }
        if (block == sBlockCasings2 && 0 == meta) {
            tCountCasing++;
            return 2;
        }
        return 0;
    }

    public static int getTierGearBoxCasing(Block block, int meta) {
        if (block == sBlockCasings2 && 2 == meta) return 1;
        if (block == sBlockCasings2 && 3 == meta) return 2;
        return 0;
    }

    public static int getTierPipeCasing(Block block, int meta) {
        if (block == sBlockCasings2 && 12 == meta) return 1;
        if (block == sBlockCasings2 && 13 == meta) return 2;
        return 0;
    }

    protected void updateHatchTexture() {
        for (GT_MetaTileEntity_Hatch h : mSteamInputs) h.updateTexture(getCasingTextureID());
        for (GT_MetaTileEntity_Hatch h : mSteamOutputs) h.updateTexture(getCasingTextureID());
        for (GT_MetaTileEntity_Hatch h : mSteamInputFluids) h.updateTexture(getCasingTextureID());
        for (GT_MetaTileEntity_Hatch h : mInputHatches) h.updateTexture(getCasingTextureID());
    }

    private int getCasingTextureID() {
        if (tierGearBoxCasing == 2 || tierPipeCasing == 2 || tierMachineCasing == 2)
            return ((GT_Block_Casings2) GregTech_API.sBlockCasings2).getTextureIndex(0);
        return ((GT_Block_Casings1) GregTech_API.sBlockCasings1).getTextureIndex(10);
    }

    @Override
    public void onValueUpdate(byte aValue) {
        tierMachineCasing = aValue;
    }

    @Override
    public byte getUpdateData() {
        return (byte) tierMachineCasing;
    }

    @Override
    protected GT_RenderedTexture getFrontOverlay() {
        return new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_FRONT_STEAM_WASHER);
    }

    @Override
    protected GT_RenderedTexture getFrontOverlayActive() {
        return new GT_RenderedTexture(Textures.BlockIcons.OVERLAY_FRONT_STEAM_WASHER_ACTIVE);
    }

    @Override
    public ITexture[] getTexture(final IGregTechTileEntity aBaseMetaTileEntity, final ForgeDirection side,
        final ForgeDirection facing, final int aColorIndex, final boolean aActive, final boolean aRedstone) {
        if (side == facing) {
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()),
                aActive ? getFrontOverlayActive() : getFrontOverlay() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(getCasingTextureID()) };
    }

    @Override
    public IStructureDefinition<GregtechMetaTileEntity_SteamWasher> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {

            STRUCTURE_DEFINITION = StructureDefinition.<GregtechMetaTileEntity_SteamWasher>builder()

                .addShape(STRUCTUR_PIECE_MAIN, transpose(shape))
                .addElement(
                    'B',
                    ofBlocksTiered(
                        GregtechMetaTileEntity_SteamWasher::getTierGearBoxCasing,
                        ImmutableList.of(Pair.of(sBlockCasings2, 2), Pair.of(sBlockCasings2, 3)),
                        -1,
                        (t, m) -> t.tierGearBoxCasing = m,
                        t -> t.tierGearBoxCasing))
                .addElement(
                    'C',
                    ofBlocksTiered(
                        GregtechMetaTileEntity_SteamWasher::getTierPipeCasing,
                        ImmutableList.of(Pair.of(sBlockCasings2, 12), Pair.of(sBlockCasings2, 13)),
                        -1,
                        (t, m) -> t.tierPipeCasing = m,
                        t -> t.tierPipeCasing))
                .addElement('D', ofBlock(Blocks.glass, 0))
                .addElement(
                    'E',
                    ofChain(
                        ofBlockAnyMeta(Blocks.air),
                        ofBlockAnyMeta(Blocks.water),
                        ofBlockAnyMeta(BlocksItems.getFluidBlock(InternalName.fluidDistilledWater))))
                .addElement(
                    'A',
                    ofChain(
                        buildSteamInput(GregtechMetaTileEntity_SteamWasher.class).casingIndex(10)
                            .dot(1)
                            .allowOnly(ForgeDirection.NORTH)
                            .build(),
                        buildHatchAdder(GregtechMetaTileEntity_SteamWasher.class)
                            .atLeast(SteamHatchElement.InputBus_Steam, SteamHatchElement.OutputBus_Steam, InputHatch)
                            .casingIndex(10)
                            .dot(1)
                            .allowOnly(ForgeDirection.NORTH)
                            .buildAndChain(),
                        ofBlocksTiered(
                            this::getTierMachineCasing,
                            ImmutableList.of(Pair.of(sBlockCasings1, 10), Pair.of(sBlockCasings2, 0)),
                            -1,
                            (t, m) -> t.tierMachineCasing = m,
                            t -> t.tierMachineCasing)))
                .build();

        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        this.buildPiece(STRUCTUR_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (this.mMachine) return -1;
        return this.survivialBuildPiece(
            STRUCTUR_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFF_SET,
            VERTICAL_OFF_SET,
            DEPTH_OFF_SET,
            elementBudget,
            env,
            false,
            true);
    }

    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        tierGearBoxCasing = -1;
        tierPipeCasing = -1;
        tierMachineCasing = -1;
        tCountCasing = 0;
        if (!checkPiece(STRUCTUR_PIECE_MAIN, HORIZONTAL_OFF_SET, VERTICAL_OFF_SET, DEPTH_OFF_SET)) return false;
        if (tierGearBoxCasing < 0 && tierPipeCasing < 0 && tierMachineCasing < 0) return false;
        if (tierGearBoxCasing == 1 && tierPipeCasing == 1
            && tierMachineCasing == 1
            && tCountCasing > 55
            && !mSteamInputFluids.isEmpty()
            && !mSteamInputs.isEmpty()
            && !mSteamOutputs.isEmpty()
            && !mInputHatches.isEmpty()) {
            updateHatchTexture();
            tierMachine = 1;
            return true;
        }
        if (tierGearBoxCasing == 2 && tierPipeCasing == 2
            && tierMachineCasing == 2
            && tCountCasing > 55
            && !mSteamInputFluids.isEmpty()
            && !mSteamInputs.isEmpty()
            && !mSteamOutputs.isEmpty()
            && !mInputHatches.isEmpty()) {
            updateHatchTexture();
            tierMachine = 2;
            return true;
        }
        return false;
    }

    @Override
    public int getMaxParallelRecipes() {
        return tierMachine == 1 ? 8 : 16;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        if (machineMode == MACHINEMODE_SIMPLEWASH) {
            return GTPPRecipeMaps.simpleWasherRecipes;
        }
        return RecipeMaps.oreWasherRecipes;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {

        return new ProcessingLogic() {

            @NotNull
            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GT_Recipe recipe) {
                if (checkForWater()) {
                    return CheckRecipeResultRegistry.SUCCESSFUL;
                }
                return SimpleCheckRecipeResult.ofFailure("no_water");
            }
            @Override
            @Nonnull
            protected GT_OverclockCalculator createOverclockCalculator(@NotNull GT_Recipe recipe) {
                return GT_OverclockCalculator.ofNoOverclock(recipe)
                    .setEUtDiscount(1.33F)
                    .setSpeedBoost(1.5F);
            }
        }.setMaxParallelSupplier(this::getMaxParallelRecipes);
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType(getMachineType())
            .addInfo("Controller Block for the Steam Washer")
            .addInfo("Runs recipes up to LV tier")
            .addInfo("33.3% faster than a single block steam machine would run.")
            .addInfo(
                "On Tier 1, it uses only 66.6% of the steam/s required compared to what a single block steam machine would use.")
            .addInfo("Washes up to 8 x Tier things at a time.")
            .addSeparator()
            .beginStructureBlock(5, 5, 5, false)
            .addInputBus(EnumChatFormatting.GOLD + "1" + EnumChatFormatting.GRAY + " Any casing", 1)
            .addInputHatch(EnumChatFormatting.GOLD + "1" + EnumChatFormatting.GRAY + " Any casing", 1)
            .addOutputBus(EnumChatFormatting.GOLD + "1" + EnumChatFormatting.GRAY + " Any casing", 1)
            .addStructureInfo(
                EnumChatFormatting.WHITE + "Steam Input Hatch "
                    + EnumChatFormatting.GOLD
                    + "1"
                    + EnumChatFormatting.GRAY
                    + " Any casing")
            .addStructureInfo("")
            .addStructureInfo(EnumChatFormatting.BLUE + "Tier " + EnumChatFormatting.DARK_PURPLE + 1)
            .addStructureInfo(EnumChatFormatting.GOLD + "55-59x" + EnumChatFormatting.GRAY + " Bronze Plated Bricks")
            .addStructureInfo(EnumChatFormatting.GOLD + "24x" + EnumChatFormatting.GRAY + " Any Glass")
            .addStructureInfo(EnumChatFormatting.GOLD + "12x" + EnumChatFormatting.GRAY + " Bronze Pipe Casing")
            .addStructureInfo(EnumChatFormatting.GOLD + "8x" + EnumChatFormatting.GRAY + " Bronze Gear Box Casing")
            .addStructureInfo("")
            .addStructureInfo(EnumChatFormatting.BLUE + "Tier " + EnumChatFormatting.DARK_PURPLE + 2)
            .addStructureInfo(
                EnumChatFormatting.GOLD + "55-59x" + EnumChatFormatting.GRAY + " Solid Steel Machine Casing")
            .addStructureInfo(EnumChatFormatting.GOLD + "24x" + EnumChatFormatting.GRAY + " Any Glass")
            .addStructureInfo(EnumChatFormatting.GOLD + "12x" + EnumChatFormatting.GRAY + " Steel Pipe Casing")
            .addStructureInfo(EnumChatFormatting.GOLD + "8x" + EnumChatFormatting.GRAY + " Steel Gear Box Casing")
            .addStructureInfo("")
            .toolTipFinisher(AuthorEvgenWarGold);
        return tt;
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add("Machine Tier: " + EnumChatFormatting.YELLOW + tierMachine);
        info.add("Parallel: " + EnumChatFormatting.YELLOW + getMaxParallelRecipes());
        return info.toArray(new String[0]);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currenttip, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();

        currenttip.add(
            StatCollector.translateToLocal("GTPP.machines.tier") + ": "
                + EnumChatFormatting.YELLOW
                + tag.getInteger("tierMachine")
                + EnumChatFormatting.RESET);
        currenttip.add(
            StatCollector.translateToLocal("GT5U.multiblock.curparallelism") + ": "
                + EnumChatFormatting.BLUE
                + tag.getInteger("parallel")
                + EnumChatFormatting.RESET);
        currenttip.add(
            StatCollector.translateToLocal("GT5U.machines.oreprocessor1") + " "
                + EnumChatFormatting.WHITE
                + StatCollector.translateToLocal("GT5U.GTPP_MULTI_WASH_PLANT.mode." + tag.getInteger("mode"))
                + EnumChatFormatting.RESET);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("tierMachine", tierMachine);
        tag.setInteger("parallel", getMaxParallelRecipes());
        tag.setInteger("mode", machineMode);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("tierMachine", tierMachine);
        aNBT.setInteger("mMode", machineMode);
    }

    @Override
    public void loadNBTData(final NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        tierMachine = aNBT.getInteger("tierMachine");
        machineMode = aNBT.getInteger("mMode");
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        // don't rotate a washer, water will flow out.
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated();
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected ResourceLocation getActivitySoundLoop() {
        return SoundResource.GT_MACHINES_STEAM_WASHER_LOOP.resourceLocation;
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        if (machineMode == MACHINEMODE_OREWASH) return MACHINEMODE_SIMPLEWASH;
        else return MACHINEMODE_OREWASH;
    }

    @Override
    public void setMachineModeIcons() {
        machineModeIcons.clear();
        machineModeIcons.add(GT_UITextures.OVERLAY_BUTTON_MACHINEMODE_WASHPLANT);
        machineModeIcons.add(GT_UITextures.OVERLAY_BUTTON_MACHINEMODE_SIMPLEWASHER);
    }

    @Override
    public String getMachineModeName() {
        return StatCollector.translateToLocal("GT5U.GTPP_MULTI_WASH_PLANT.mode." + machineMode);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            if (mUpdate < 0) mUpdate = 300;
        }
    }
    private boolean checkForWater() {
        ExtendedFacing facing = getExtendedFacing();
        final ForgeDirection frontFacing = facing.getDirection();
        final Flip curFlip = facing.getFlip();
        IGregTechTileEntity gregTechTileEntity = this.getBaseMetaTileEntity();

        double xOffset = getExtendedFacing().getRelativeBackInWorld().offsetX;
        double zOffset = getExtendedFacing().getRelativeBackInWorld().offsetZ;
        double yOffset = getExtendedFacing().getRelativeBackInWorld().offsetY;

        if (frontFacing == ForgeDirection.WEST) {
            xOffset -= 1;
            if (curFlip.isHorizontallyFlipped()) {
                zOffset -= 6;
            } else zOffset += 4;
        }

        if (frontFacing == ForgeDirection.EAST) {
            xOffset -= 1;
            if (curFlip.isHorizontallyFlipped()) {
                zOffset += 4;
            } else zOffset -= 6;
        }

        if (frontFacing == ForgeDirection.NORTH) {
            zOffset -= 1;
            if (curFlip.isHorizontallyFlipped()) {
                xOffset += 4;
            } else xOffset -= 6;
        }

        if (frontFacing == ForgeDirection.SOUTH) {
            zOffset -= 1;
            if (curFlip.isHorizontallyFlipped()) {
                xOffset -= 6;
            } else xOffset += 4;
        }

        int x = gregTechTileEntity.getXCoord();
        int y = gregTechTileEntity.getYCoord();
        int z = gregTechTileEntity.getZCoord();

        amountWaterBlock = 0;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Block tBlock = this.getBaseMetaTileEntity().getWorld().getBlock((int) (i + xOffset + x), (int) (y + yOffset), (int) (j + zOffset + z));
                if (tBlock == Blocks.air || tBlock == Blocks.flowing_water) {
                    if (tryConsumeWater()) {
                        this.getBaseMetaTileEntity()
                            .getWorld()
                            .setBlock((int) (i + xOffset + x), (int) (y + yOffset), (int) (j + zOffset + z), Blocks.water);
                    }
                }
                if (tBlock == Blocks.water) {
                    amountWaterBlock++;
                }
            }
        }
        return amountWaterBlock >= 8;
    }

    private boolean tryConsumeWater() {
        if (getStoredFluids() != null) {
            for (FluidStack waterCapacity : this.getStoredFluids()) {
                if (waterCapacity.isFluidEqual(FluidUtils.getWater(1000))) {
                    if (waterCapacity.amount >= 1000) {
                        waterCapacity.amount -= 1000;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkBrokenMachine() {
        if (mMachine) {
            return false;
        } else isBroken = false;
        return false;
    }
}
