package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_OIL_CRACKER_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.casingTexturePages;
import static gregtech.api.util.GTStructureUtility.activeCoils;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofCoil;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMultiInput;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.maps.OilCrackerBackend;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.tileentities.machines.IRecipeProcessingAwareHatch;
import gregtech.common.tileentities.machines.MTEHatchInputME;

public class MTEOilCracker extends MTEEnhancedMultiBlockBase<MTEOilCracker> implements ISurvivalConstructable {

    private static final byte CASING_INDEX = 49;
    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final IStructureDefinition<MTEOilCracker> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEOilCracker>builder()
        .addShape(
            STRUCTURE_PIECE_MAIN,
            transpose(
                new String[][] { { "lcmcr", "lcmcr", "lcmcr" }, { "lc~cr", "l---r", "lcmcr" },
                    { "lcmcr", "lcmcr", "lcmcr" }, }))
        .addElement('c', activeCoils(ofCoil(MTEOilCracker::setCoilLevel, MTEOilCracker::getCoilLevel)))
        .addElement(
            'l',
            buildHatchAdder(MTEOilCracker.class)
                .atLeast(
                    HatchElement.InputHatch.withAdder(MTEOilCracker::addLeftHatchToMachineList),
                    HatchElement.Energy,
                    HatchElement.Maintenance)
                .dot(2)
                .casingIndex(CASING_INDEX)
                .buildAndChain(onElementPass(MTEOilCracker::onCasingAdded, ofBlock(GregTechAPI.sBlockCasings4, 1))))
        .addElement(
            'r',
            buildHatchAdder(MTEOilCracker.class)
                .atLeast(
                    HatchElement.OutputHatch.withAdder(MTEOilCracker::addRightHatchToMachineList),
                    HatchElement.Energy,
                    HatchElement.Maintenance)
                .dot(3)
                .casingIndex(CASING_INDEX)
                .buildAndChain(onElementPass(MTEOilCracker::onCasingAdded, ofBlock(GregTechAPI.sBlockCasings4, 1))))
        .addElement(
            'm',
            buildHatchAdder(MTEOilCracker.class)
                .atLeast(
                    HatchElement.InputHatch.withAdder(MTEOilCracker::addMiddleInputToMachineList)
                        .withCount(t -> t.mMiddleInputHatches.size()),
                    HatchElement.InputBus,
                    HatchElement.Energy,
                    HatchElement.Maintenance)
                .dot(1)
                .casingIndex(CASING_INDEX)
                .buildAndChain(onElementPass(MTEOilCracker::onCasingAdded, ofBlock(GregTechAPI.sBlockCasings4, 1))))
        .build();
    private HeatingCoilLevel heatLevel;
    protected final List<MTEHatchInput> mMiddleInputHatches = new ArrayList<>();
    // 0 -> left, 1 -> right, any other -> not found
    protected int mInputOnSide;
    protected int mOutputOnSide;
    protected int mCasingAmount;

    public MTEOilCracker(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEOilCracker(String aName) {
        super(aName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Cracker")
            .addInfo("Thermally cracks heavy hydrocarbons into lighter fractions")
            .addInfo("More efficient than the Chemical Reactor")
            .addInfo("Gives different benefits whether it hydro or steam-cracks:")
            .addInfo("Hydro - Consumes 20% less Hydrogen and outputs 25% more cracked fluid")
            .addInfo("Steam - Outputs 50% more cracked fluid")
            .addInfo("(Values compared to cracking in the Chemical Reactor)")
            .addInfo("Place the appropriate circuit in the controller or an input bus")
            .beginStructureBlock(5, 3, 3, true)
            .addController("Front center")
            .addCasingInfoRange("Clean Stainless Steel Machine Casing", 18, 21, false)
            .addOtherStructurePart("2 Rings of 8 Coils", "Each side of the controller")
            .addInfo("Gets 10% EU/t reduction per coil tier, up to a maximum of 50%")
            .addEnergyHatch("Any casing", 1, 2, 3)
            .addMaintenanceHatch("Any casing", 1, 2, 3)
            .addInputHatch("For cracking fluid (Steam/Hydrogen/etc.) ONLY, Any middle ring casing", 1)
            .addInputHatch("Any left/right side casing", 2, 3)
            .addOutputHatch("Any right/left side casing", 2, 3)
            .addStructureInfo("Input/Output Hatches must be on opposite sides!")
            .addInputBus("Any middle ring casing, optional for programmed circuit automation")
            .addStructureHint("GT5U.cracker.io_side")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection sideDirection,
        ForgeDirection facingDirection, int colorIndex, boolean active, boolean redstoneLevel) {
        if (sideDirection == facingDirection) {
            if (active) return new ITexture[] { casingTexturePages[0][CASING_INDEX], TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_OIL_CRACKER_ACTIVE)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_OIL_CRACKER_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { casingTexturePages[0][CASING_INDEX], TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_OIL_CRACKER)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_OIL_CRACKER_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { casingTexturePages[0][CASING_INDEX] };
    }

    @Override
    public RecipeMap<OilCrackerBackend> getRecipeMap() {
        return RecipeMaps.crackingRecipes;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Nonnull
            @Override
            public CheckRecipeResult process() {
                setEuModifier(1.0F - Math.min(0.1F * (heatLevel.getTier() + 1), 0.5F));
                return super.process();
            }
        };
    }

    @Override
    public IStructureDefinition<MTEOilCracker> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    public HeatingCoilLevel getCoilLevel() {
        return heatLevel;
    }

    public void setCoilLevel(HeatingCoilLevel aCoilLevel) {
        heatLevel = aCoilLevel;
    }

    private boolean addMiddleInputToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof MTEHatchInput tHatch) {
            tHatch.updateTexture(aBaseCasingIndex);
            tHatch.mRecipeMap = getRecipeMap();
            return mMiddleInputHatches.add(tHatch);
        }
        return false;
    }

    private boolean addLeftHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof MTEHatchInput tHatch) {
            if (mInputOnSide == 1) return false;
            mInputOnSide = 0;
            mOutputOnSide = 1;
            tHatch.updateTexture(aBaseCasingIndex);
            tHatch.mRecipeMap = getRecipeMap();
            return mInputHatches.add(tHatch);
        }
        if (aMetaTileEntity instanceof MTEHatchOutput tHatch) {
            if (mOutputOnSide == 1) return false;
            mInputOnSide = 1;
            mOutputOnSide = 0;
            tHatch.updateTexture(aBaseCasingIndex);
            return mOutputHatches.add(tHatch);
        }
        return false;
    }

    private boolean addRightHatchToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) return false;
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) return false;
        if (aMetaTileEntity instanceof MTEHatchInput tHatch) {
            if (mInputOnSide == 0) return false;
            mInputOnSide = 1;
            mOutputOnSide = 0;
            tHatch.updateTexture(aBaseCasingIndex);
            tHatch.mRecipeMap = getRecipeMap();
            return mInputHatches.add(tHatch);
        }
        if (aMetaTileEntity instanceof MTEHatchOutput tHatch) {
            if (mOutputOnSide == 0) return false;
            mInputOnSide = 0;
            mOutputOnSide = 1;
            tHatch.updateTexture(aBaseCasingIndex);
            return mOutputHatches.add(tHatch);
        }
        return false;
    }

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        setCoilLevel(HeatingCoilLevel.None);
        mCasingAmount = 0;
        mMiddleInputHatches.clear();
        mInputOnSide = -1;
        mOutputOnSide = -1;
        return checkPiece(STRUCTURE_PIECE_MAIN, 2, 1, 0) && mInputOnSide != -1
            && mOutputOnSide != -1
            && mCasingAmount >= 18
            && mMaintenanceHatches.size() == 1
            && !mMiddleInputHatches.isEmpty();
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEOilCracker(this.mName);
    }

    @Override
    public ArrayList<FluidStack> getStoredFluidsForColor(Optional<Byte> color) {
        final ArrayList<FluidStack> rList = new ArrayList<>();
        Map<Fluid, FluidStack> inputsFromME = new HashMap<>();
        for (final MTEHatchInput tHatch : validMTEList(mInputHatches)) {
            byte hatchColor = tHatch.getBaseMetaTileEntity()
                .getColorization();
            if (color.isPresent() && hatchColor != -1 && hatchColor != color.get()) continue;
            tHatch.mRecipeMap = getRecipeMap();
            if (tHatch instanceof MTEHatchInputME meHatch) {
                for (FluidStack tFluid : meHatch.getStoredFluids()) {
                    if (tFluid != null && !getRecipeMap().getBackend()
                        .isValidCatalystFluid(tFluid)) {
                        inputsFromME.put(tFluid.getFluid(), tFluid);
                    }
                }
            } else if (tHatch instanceof MTEHatchMultiInput) {
                for (final FluidStack tFluid : ((MTEHatchMultiInput) tHatch).getStoredFluid()) {
                    if (tFluid != null && !getRecipeMap().getBackend()
                        .isValidCatalystFluid(tFluid)) {
                        rList.add(tFluid);
                    }
                }
            } else {
                if (tHatch.getFillableStack() != null) {
                    if (!getRecipeMap().getBackend()
                        .isValidCatalystFluid(tHatch.getFillableStack())) rList.add(tHatch.getFillableStack());
                }
            }
        }
        for (final MTEHatchInput tHatch : validMTEList(mMiddleInputHatches)) {
            byte hatchColor = tHatch.getBaseMetaTileEntity()
                .getColorization();
            if (color.isPresent() && hatchColor != -1 && hatchColor != color.get()) continue;
            tHatch.mRecipeMap = getRecipeMap();
            if (tHatch instanceof MTEHatchInputME meHatch) {
                for (FluidStack tFluid : meHatch.getStoredFluids()) {
                    if (tFluid != null && getRecipeMap().getBackend()
                        .isValidCatalystFluid(tFluid)) {
                        inputsFromME.put(tFluid.getFluid(), tFluid);
                    }
                }
            } else if (tHatch instanceof MTEHatchMultiInput) {
                for (final FluidStack tFluid : ((MTEHatchMultiInput) tHatch).getStoredFluid()) {
                    if (tFluid != null && getRecipeMap().getBackend()
                        .isValidCatalystFluid(tFluid)) {
                        rList.add(tFluid);
                    }
                }
            } else {
                if (tHatch.getFillableStack() != null) {
                    final FluidStack tStack = tHatch.getFillableStack();
                    if (getRecipeMap().getBackend()
                        .isValidCatalystFluid(tStack)) {
                        rList.add(tStack);
                    }
                }
            }
        }
        if (!inputsFromME.isEmpty()) {
            rList.addAll(inputsFromME.values());
        }
        return rList;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 2, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, 2, 1, 0, elementBudget, env, false, true);
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public void startRecipeProcessing() {
        for (MTEHatchInput hatch : validMTEList(mMiddleInputHatches)) {
            if (hatch instanceof IRecipeProcessingAwareHatch aware) {
                aware.startRecipeProcessing();
            }
        }
        super.startRecipeProcessing();
    }

    @Override
    public void endRecipeProcessing() {
        super.endRecipeProcessing();
        for (MTEHatchInput hatch : validMTEList(mMiddleInputHatches)) {
            if (hatch instanceof IRecipeProcessingAwareHatch aware) {
                setResultIfFailure(aware.endRecipeProcessing(this));
            }
        }
    }
}
