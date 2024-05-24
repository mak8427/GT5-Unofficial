package gregtech.common.tileentities.machines.multi.purification;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static gregtech.api.enums.GT_HatchElement.InputHatch;
import static gregtech.api.enums.GT_HatchElement.OutputHatch;
import static gregtech.api.enums.GT_Values.AuthorNotAPenguin;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_GLOW;
import static gregtech.api.recipe.RecipeMaps.purificationPlasmaHeatingRecipes;
import static gregtech.api.util.GT_StructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Mods;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_StructureUtility;

public class GT_MetaTileEntity_PurificationUnitPlasmaHeater
    extends GT_MetaTileEntity_PurificationUnitBase<GT_MetaTileEntity_PurificationUnitPlasmaHeater>
    implements ISurvivalConstructable {

    private static final int CASING_INDEX_HEATER = getTextureIndex(GregTech_API.sBlockCasings9, 10);
    private static final int CASING_INDEX_TOWER = getTextureIndex(GregTech_API.sBlockCasings9, 4);

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final int STRUCTURE_X_OFFSET = 2;
    private static final int STRUCTURE_Y_OFFSET = 14;
    private static final int STRUCTURE_Z_OFFSET = 5;
    private static final long CONSUME_INTERVAL = 20;

    private long currentTemperature = 0;
    private int cyclesCompleted = 0;
    private boolean ruinedCycle = false;

    private enum CycleState {
        // Was previously at 0K, currently waiting to heat to 10000K
        Heating,
        // Was previously at 10000K, currently waiting to cool down to 0K
        Cooling
    }

    private CycleState state;

    // A cycle is 30s at shortest, a purification plant cycle is 120s. 33% chance per heating cycle
    // will give you plenty of room for delay and still get to 99% chance.
    public static final long SUCCESS_PER_CYCLE = 33;

    // Consumption rates in liters/second
    public static final long MAX_PLASMA_PER_SEC = 10;
    public static final long MAX_COOLANT_PER_SEC = 100;
    // Change in temperature per consumed liter of plasma
    public static final long PLASMA_TEMP_PER_LITER = 100;
    // Change in temperature per consumed liter of coolant
    public static final long COOLANT_TEMP_PER_LITER = -5;
    // Temperature at which the batch is ruined
    public static final long MAX_TEMP = 12500;
    // Point at which the heating point of the cycle is reached
    public static final long HEATING_POINT = 10000;

    private static final Materials plasmaMaterial = Materials.Helium;
    private static final Materials coolantMaterial = Materials.SuperCoolant;

    private GT_MetaTileEntity_Hatch_Input plasmaInputHatch;
    private GT_MetaTileEntity_Hatch_Input coolantInputHatch;

    private static final String[][] structure = new String[][] {
        // spotless:off
        { "            DDDDDDD    ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "             DDDDD     ", "             DDDDD     ", "             DDKDD     " },
        { "          DD       DD  ", "            DDDDDDD    ", "             DDDDD     ", "                       ", "                       ", "                       ", "                       ", "                       ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "           DD     DD   ", "           DD     DD   ", "           DDDDDDDDD   " },
        { "         D           D ", "          DDD     DDD  ", "            DDDDDDD    ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "          D         D  ", "          D         D  ", "          DDDDDDDDDDD  " },
        { "         D           D ", "          D        DD  ", "           DD      D   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "          D         D  ", "          D         D  ", "          D         D  ", "          D         D  ", "         D           D ", "         D           D ", "         DDDDDDDDDDDDD " },
        { "        D             D", "         DD         DD ", "          DD        D  ", "           D       D   ", "           D       D   ", "           D       D   ", "           D       D   ", "           D       D   ", "          D         D  ", "          D         D  ", "          D         D  ", "          D         D  ", "         D           D ", "         D           D ", "         DDDDDDDDDDDDD " },
        { "        D             D", "         D           D ", "         DD          D ", "          D         D  ", "          D         D  ", "GBBBG     D         D  ", "G   G     D         D  ", "G   G     D         D  ", "G   G    D           D ", "G   G    D           D ", "G   G    D           D ", "G   G    D           D ", "G   G   D             D", "G   G   D             D", "GB~BG   DDDDDDDDDDDDDDD" },
        { "        D             D", "         D           D ", "         DD          D ", "          D         D  ", " BBB      D         D  ", "BBBBB     D         D  ", " EEE      D         D  ", " EEE      D         D  ", " EEE     D           D ", " EEE     D           D ", " EEE     D           D ", " EEE     D           D ", " EEE    D             D", " EEEBBBBD             D", "BAAAB   DDDDDDDDDDDDDDD" },
        { "        D             D", "         D           D ", "         DD          D ", "          D         D  ", " BBB      D         D  ", "BBBBB     D         D  ", " EFE      D         D  ", " EFE      D         D  ", " EFE     D           D ", " EFE     D           D ", " EFE     D           D ", " EFE     D           D ", " EFEBBBBD             D", " EFE    D             D", "PAAABBBBDDDDDDDDDDDDDDD" },
        { "        D             D", "         D           D ", "         DD          D ", "          D         D  ", " BBB      D         D  ", "BBBBB     D         D  ", " EEE      D         D  ", " EEE      D         D  ", " EEE     D           D ", " EEE     D           D ", " EEE     D           D ", " EEE     D           D ", " EEE    D             D", " EEEBBBBD             D", "BAAAB   DDDDDDDDDDDDDDD" },
        { "        D             D", "         D           D ", "         DD          D ", "          D         D  ", "          D         D  ", "GBBBG     D         D  ", "G   G     D         D  ", "G   G     D         D  ", "G   G    D           D ", "G   G    D           D ", "G   G    D           D ", "G   G    D           D ", "G   G   D             D", "G   G   D             D", "GBBBG   DDDDDDDDDDDDDDD" },
        { "        D             D", "         D           D ", "          D         D  ", "           D       DD  ", "           D       DD  ", "           D       DD  ", "           D       DD  ", "           D       DD  ", "          D          D ", "          D          D ", "          D          D ", "          D          D ", "         D           D ", "         D           D ", "         DDDDDDDDDDDDD " },
        { "         D           DD", "          D         D  ", "           D       D   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "          D         D  ", "          D         D  ", "          D         D  ", "          D         D  ", "         D           D ", "         D           D ", "         DDDDDDDDDDDDD " },
        { "         D           D ", "          DD       DD  ", "            D     D    ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "           DD     DD   ", "          D         D  ", "          D         D  ", "          DDDDDDDDDDD  " },
        { "          DD       DD  ", "            DDDDDDD    ", "             DDDDD     ", "                       ", "                       ", "                       ", "                       ", "                       ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     ", "           DD     DD   ", "           DD     DD   ", "           DDDDDDDDD   " },
        { "            DDDDDDD    ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "                       ", "             DDDDD     ", "             DDDDD     ", "             DDDDD     " } };
    // spotless:on

    private static final IStructureDefinition<GT_MetaTileEntity_PurificationUnitPlasmaHeater> STRUCTURE_DEFINITION = StructureDefinition
        .<GT_MetaTileEntity_PurificationUnitPlasmaHeater>builder()
        .addShape(STRUCTURE_PIECE_MAIN, structure)
        // Superconducting coil block
        .addElement('A', ofBlock(GregTech_API.sBlockCasings1, 15))
        // Plasma Heating Casing
        .addElement(
            'B',
            ofChain(
                lazy(
                    t -> GT_StructureUtility.<GT_MetaTileEntity_PurificationUnitPlasmaHeater>buildHatchAdder()
                        .atLeastList(t.getAllowedHatches())
                        .dot(1)
                        .casingIndex(CASING_INDEX_HEATER)
                        .build()),
                ofBlock(GregTech_API.sBlockCasings9, 10)))
        // Water Plant Concrete Casing
        .addElement('D', ofBlock(GregTech_API.sBlockCasings9, 4))
        // Any Tinted Glass
        .addElement('E', ofBlockAnyMeta(GregTech_API.sBlockTintedGlass, 0))
        // Neonite, with fallback to air
        .addElement('F', lazy(t -> {
            if (Mods.Chisel.isModLoaded()) {
                Block neonite = GameRegistry.findBlock(Mods.Chisel.ID, "neonite");
                return ofBlockAnyMeta(neonite, 7);
            } else {
                return ofBlockAnyMeta(Blocks.air);
            }
        }))
        // Superconductor Base ZPM frame box
        .addElement('G', ofFrame(Materials.Tetranaquadahdiindiumhexaplatiumosminid))
        // Coolant input hatch
        .addElement(
            'K',
            lazy(
                t -> GT_StructureUtility.<GT_MetaTileEntity_PurificationUnitPlasmaHeater>buildHatchAdder()
                    .atLeast(InputHatch)
                    .dot(2)
                    .adder(GT_MetaTileEntity_PurificationUnitPlasmaHeater::addCoolantHatchToMachineList)
                    .cacheHint(() -> "Input Hatch (Coolant)")
                    .casingIndex(CASING_INDEX_TOWER)
                    .build()))
        // Plasma input hatch
        .addElement(
            'P',
            lazy(
                t -> GT_StructureUtility.<GT_MetaTileEntity_PurificationUnitPlasmaHeater>buildHatchAdder()
                    .atLeast(InputHatch)
                    .dot(3)
                    .adder(GT_MetaTileEntity_PurificationUnitPlasmaHeater::addPlasmaHatchToMachineList)
                    .cacheHint(() -> "Input Hatch (Plasma)")
                    .casingIndex(CASING_INDEX_HEATER)
                    .build()))
        .build();

    private List<IHatchElement<? super GT_MetaTileEntity_PurificationUnitPlasmaHeater>> getAllowedHatches() {
        return ImmutableList.of(InputHatch, OutputHatch);
    }

    public GT_MetaTileEntity_PurificationUnitPlasmaHeater(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_PurificationUnitPlasmaHeater(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_PurificationUnitPlasmaHeater(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean active, boolean redstoneLevel) {
        if (side == facing) {
            if (active) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(CASING_INDEX_HEATER),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(CASING_INDEX_HEATER),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(CASING_INDEX_HEATER) };
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            hintsOnly,
            STRUCTURE_X_OFFSET,
            STRUCTURE_Y_OFFSET,
            STRUCTURE_Z_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        return survivialBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            STRUCTURE_X_OFFSET,
            STRUCTURE_Y_OFFSET,
            STRUCTURE_Z_OFFSET,
            elementBudget,
            env,
            true);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_PurificationUnitPlasmaHeater> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return purificationPlasmaHeatingRecipes;
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addInfo("Each cycle completed boosts success by " + EnumChatFormatting.RED + SUCCESS_PER_CYCLE + "%");
        tt.addInfo(AuthorNotAPenguin);
        tt.toolTipFinisher("GregTech");
        return tt;
    }

    @Override
    public void startCycle(int cycleTime, int progressTime) {
        super.startCycle(cycleTime, progressTime);
        this.cyclesCompleted = 0;
        this.currentTemperature = 0;
        this.ruinedCycle = false;
        this.state = CycleState.Heating;
    }

    // Drains up to maxAmount of a fluid if it is the same fluid as given, returns the amount drained
    private long drainFluidLimited(GT_MetaTileEntity_Hatch_Input inputHatch, FluidStack fluid, long maxAmount) {
        FluidStack hatchStack = inputHatch.getDrainableStack();
        if (hatchStack == null) return 0;
        if (hatchStack.isFluidEqual(fluid)) {
            long amountToDrain = Math.min(maxAmount, hatchStack.amount);
            if (amountToDrain > 0) {
                inputHatch.drain((int) amountToDrain, true);
            }
            return amountToDrain;
        } else {
            return 0;
        }
    }

    @NotNull
    @Override
    public CheckRecipeResult checkProcessing() {
        RecipeMap<?> recipeMap = this.getRecipeMap();

        GT_Recipe recipe = recipeMap.findRecipeQuery()
            .fluids(
                this.getStoredFluids()
                    .toArray(new FluidStack[] {}))
            .find();

        this.endRecipeProcessing();
        if (recipe == null) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        if (this.protectsExcessFluid() && !this.canOutputAll(recipe.mFluidOutputs)) {
            return CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
        }

        this.currentRecipe = recipe;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.runMachine(aBaseMetaTileEntity, aTick);
        if (mMaxProgresstime > 0 && aTick % CONSUME_INTERVAL == 0) {
            // Drain plasma and coolant up to limited amount per second
            long plasmaDrained = drainFluidLimited(plasmaInputHatch, plasmaMaterial.getPlasma(1L), MAX_PLASMA_PER_SEC);
            long coolantDrained = drainFluidLimited(
                coolantInputHatch,
                coolantMaterial.getFluid(1L),
                MAX_COOLANT_PER_SEC);
            // Calculate temperature change
            long tempChance = plasmaDrained * PLASMA_TEMP_PER_LITER + coolantDrained * COOLANT_TEMP_PER_LITER;
            currentTemperature = Math.max(0, currentTemperature + tempChance);
            // Check if batch was ruined
            if (currentTemperature > MAX_TEMP) {
                ruinedCycle = true;
            }
            // Update cycle state.
            switch (state) {
                case Heating -> {
                    // Heating state can change to cooling when temperature exceeds 10000K
                    if (currentTemperature >= HEATING_POINT) {
                        state = CycleState.Cooling;
                    }
                }
                case Cooling -> {
                    if (currentTemperature == 0) {
                        state = CycleState.Heating;
                        cyclesCompleted += 1;
                    }
                }
            }
        }
    }

    public boolean addCoolantHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            ((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity).mRecipeMap = null;
            coolantInputHatch = (GT_MetaTileEntity_Hatch_Input) aMetaTileEntity;
            return true;
        }
        return false;
    }

    public boolean addPlasmaHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Input) {
            ((GT_MetaTileEntity_Hatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
            ((GT_MetaTileEntity_Hatch_Input) aMetaTileEntity).mRecipeMap = null;
            plasmaInputHatch = (GT_MetaTileEntity_Hatch_Input) aMetaTileEntity;
            return true;
        }
        return false;
    }

    @Override
    public float calculateFinalSuccessChance() {
        if (ruinedCycle) return 0.0f;
        // Success chance directly depends on number of cycles completed.
        return cyclesCompleted * SUCCESS_PER_CYCLE;
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> infoData = new ArrayList<>(Arrays.asList(super.getInfoData()));
        infoData.add("Current temperature: " + EnumChatFormatting.YELLOW + currentTemperature + "K");
        infoData.add("Heating cycles completed this run: " + EnumChatFormatting.YELLOW + cyclesCompleted);
        return infoData.toArray(new String[] {});
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setLong("mCurrentTemperature", currentTemperature);
        aNBT.setInteger("mCyclesCompleted", cyclesCompleted);
        aNBT.setBoolean("mRuinedCycle", ruinedCycle);
        aNBT.setString("mCycleState", state.toString());
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        currentTemperature = aNBT.getLong("mCurrentTemperature");
        cyclesCompleted = aNBT.getInteger("mCyclesCompleted");
        ruinedCycle = aNBT.getBoolean("mRuinedCycle");
        state = CycleState.valueOf(aNBT.getString("mCycleState"));
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public int getWaterTier() {
        return 5;
    }

    @Override
    public long getActivePowerUsage() {
        return TierEU.RECIPE_UV;
    }

    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        if (!checkPiece(STRUCTURE_PIECE_MAIN, STRUCTURE_X_OFFSET, STRUCTURE_Y_OFFSET, STRUCTURE_Z_OFFSET)) return false;
        return super.checkMachine(aBaseMetaTileEntity, aStack);
    }
}
