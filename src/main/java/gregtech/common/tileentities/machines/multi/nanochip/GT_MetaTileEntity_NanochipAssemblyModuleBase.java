package gregtech.common.tileentities.machines.multi.nanochip;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static gregtech.common.tileentities.machines.multi.nanochip.GT_MetaTileEntity_NanochipAssemblyComplex.CASING_INDEX_BASE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTech_API;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GT_HatchElementBuilder;
import gregtech.api.util.GT_OverclockCalculator;
import gregtech.api.util.GT_ParallelHelper;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.IGT_HatchAdder;
import gregtech.api.util.shutdown.ShutDownReasonRegistry;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.tileentities.machines.multi.nanochip.hatches.GT_MetaTileEntity_Hatch_VacuumConveyor_Input;
import gregtech.common.tileentities.machines.multi.nanochip.hatches.GT_MetaTileEntity_Hatch_VacuumConveyor_Output;
import gregtech.common.tileentities.machines.multi.nanochip.util.CCInputConsumer;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponent;
import gregtech.common.tileentities.machines.multi.nanochip.util.CircuitComponentPacket;
import gregtech.common.tileentities.machines.multi.nanochip.util.VacuumConveyorHatchMap;

public abstract class GT_MetaTileEntity_NanochipAssemblyModuleBase<T extends GT_MetaTileEntity_ExtendedPowerMultiBlockBase<T>>
    extends GT_MetaTileEntity_ExtendedPowerMultiBlockBase<T> implements ISurvivalConstructable {

    protected static final String STRUCTURE_PIECE_BASE = "base";
    protected static final String[][] base_structure = new String[][] { { "V~V" }, { "VVV" }, { "VVV" } };

    protected static final int BASE_STRUCTURE_OFFSET_X = 1;
    protected static final int BASE_STRUCTURE_OFFSET_Y = 0;
    protected static final int BASE_STRUCTURE_OFFSET_Z = 0;

    private boolean isConnected = false;

    private long availableEUt = 0;

    private final ArrayList<ItemStack> inputFakeItems = new ArrayList<>();
    private final ArrayList<ItemStack> outputFakeItems = new ArrayList<>();
    private byte outputColor = -1;
    private int currentParallel;

    // Something, needs to be tested further what this should really be (probably MUCH higher and scale with hatch tier)
    protected static long EU_BUFFER_BASE_SIZE = 160008000L * 1024;
    protected final long euBufferSize = EU_BUFFER_BASE_SIZE;

    protected final VacuumConveyorHatchMap<GT_MetaTileEntity_Hatch_VacuumConveyor_Input> vacuumConveyorInputs = new VacuumConveyorHatchMap<>();
    protected final VacuumConveyorHatchMap<GT_MetaTileEntity_Hatch_VacuumConveyor_Output> vacuumConveyorOutputs = new VacuumConveyorHatchMap<>();

    public static <B extends GT_MetaTileEntity_NanochipAssemblyModuleBase<B>> StructureDefinition.Builder<B> addBaseStructure(
        StructureDefinition.Builder<B> structure) {
        return structure.addShape(STRUCTURE_PIECE_BASE, base_structure)
            .addElement(
                'V',
                GT_HatchElementBuilder.<B>builder()
                    .atLeast(ModuleHatchElement.VacuumConveyorHatch)
                    .casingIndex(CASING_INDEX_BASE)
                    .dot(2)
                    .buildAndChain(ofBlock(GregTech_API.sBlockCasings4, 0)));
    }

    public enum ModuleHatchElement implements IHatchElement<GT_MetaTileEntity_NanochipAssemblyModuleBase<?>> {

        VacuumConveyorHatch(GT_MetaTileEntity_NanochipAssemblyModuleBase::addConveyorToMachineList,
            GT_MetaTileEntity_NanochipAssemblyModuleBase.class) {

            @Override
            public long count(GT_MetaTileEntity_NanochipAssemblyModuleBase<?> tileEntity) {
                return tileEntity.vacuumConveyorInputs.size() + tileEntity.vacuumConveyorOutputs.size();
            }
        };

        private final List<Class<? extends IMetaTileEntity>> mteClasses;
        private final IGT_HatchAdder<GT_MetaTileEntity_NanochipAssemblyModuleBase<?>> adder;

        @SafeVarargs
        ModuleHatchElement(IGT_HatchAdder<GT_MetaTileEntity_NanochipAssemblyModuleBase<?>> adder,
            Class<? extends IMetaTileEntity>... mteClasses) {
            this.mteClasses = Collections.unmodifiableList(Arrays.asList(mteClasses));
            this.adder = adder;
        }

        @Override
        public List<? extends Class<? extends IMetaTileEntity>> mteClasses() {
            return mteClasses;
        }

        public IGT_HatchAdder<? super GT_MetaTileEntity_NanochipAssemblyModuleBase<?>> adder() {
            return adder;
        }
    }

    /**
     * Create new nanochip assembly module
     *
     * @param aID           ID of this module
     * @param aName         Name of this module
     * @param aNameRegional Localized name of this module
     */
    protected GT_MetaTileEntity_NanochipAssemblyModuleBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    protected GT_MetaTileEntity_NanochipAssemblyModuleBase(String aName) {
        super(aName);
    }

    // Only checks the base structure piece
    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        this.vacuumConveyorInputs.clear();
        this.vacuumConveyorOutputs.clear();
        fixAllIssues();
        return checkPiece(
            STRUCTURE_PIECE_BASE,
            BASE_STRUCTURE_OFFSET_X,
            BASE_STRUCTURE_OFFSET_Y,
            BASE_STRUCTURE_OFFSET_Z);
    }

    public boolean addConveyorToMachineList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        }
        IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) {
            return false;
        }
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_VacuumConveyor_Input hatch) {
            hatch.updateTexture(aBaseCasingIndex);
            return vacuumConveyorInputs.addHatch(hatch);
        }
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_VacuumConveyor_Output hatch) {
            hatch.updateTexture(aBaseCasingIndex);
            return vacuumConveyorOutputs.addHatch(hatch);
        }
        return false;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    protected boolean supportsCraftingMEBuffer() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    protected static class ItemInputInformation {

        /**
         * A map containing one entry per unique item, with in each entry the color of the last hatch it was seen in.
         * This can be used to determine the output color
         */
        public final Map<GT_Utility.ItemId, Byte> colors;
        public final Map<GT_Utility.ItemId, ItemStack> inputs;

        public ItemInputInformation(Map<GT_Utility.ItemId, Byte> colors, Map<GT_Utility.ItemId, ItemStack> inputs) {
            this.colors = colors;
            this.inputs = inputs;
        }
    }

    /**
     * Find all inputs stored in the vacuum conveyor inputs.
     * Clears inputFakeItems and then adds all fake items to this hatch. Note that different stacks with the same id
     * are merged into one entry in this list, which makes lookup and parallel calculation a bit easier.
     *
     * @return Info about which hatches contained the items, and a full list of item inputs indexed by id to make
     *         parallel
     *         calculation easier
     */
    private ItemInputInformation refreshInputItems() {
        Map<GT_Utility.ItemId, Byte> itemColorMap = new HashMap<>();
        Map<GT_Utility.ItemId, ItemStack> inputs = new HashMap<>();
        // Clear input items before processing
        this.inputFakeItems.clear();
        // Refresh fake stacks represented by items in the conveyor hatches.
        // Note that we only take the first hatch with items and process it
        for (ArrayList<GT_MetaTileEntity_Hatch_VacuumConveyor_Input> conveyorList : this.vacuumConveyorInputs
            .allHatches()) {
            for (GT_MetaTileEntity_Hatch_VacuumConveyor_Input conveyor : conveyorList) {
                // Get the contents of this hatch as fake items.
                if (conveyor.contents == null) continue;
                List<ItemStack> itemsInHatch = conveyor.contents.getItemRepresentations();
                // Store the color of this hatch for each ItemStack
                byte conveyorColor = conveyor.getColorization();
                for (ItemStack stack : itemsInHatch) {
                    GT_Utility.ItemId id = GT_Utility.ItemId.createNoCopy(stack);
                    // Merge stack into the input map, so we have a list of entries that are all unique.
                    inputs.merge(
                        id,
                        stack,
                        (a, b) -> new ItemStack(a.getItem(), a.getItemDamage(), a.stackSize + b.stackSize));
                    // Also register its color
                    itemColorMap.put(id, conveyorColor);
                    // Also add the item to the list of individual input items for recipe checking
                    this.inputFakeItems.add(stack);
                }
            }
        }
        return new ItemInputInformation(itemColorMap, inputs);
    }

    /**
     * Find the color hatch that we want to use for output of the given recipe.
     *
     * @param recipe     The recipe that we are going to run
     * @param itemColors The colors the hatch each ItemStack in the recipe input can be found in
     * @return The color that the output needs to end up in. If no hatch with this color exists, the module will report
     *         that no output space is available.
     */
    protected byte findOutputColor(GT_Recipe recipe, Map<GT_Utility.ItemId, Byte> itemColors) {
        ItemStack firstInput = recipe.mInputs[0];
        GT_Utility.ItemId id = GT_Utility.ItemId.createNoCopy(firstInput);
        // If this recipe was valid and found, this should never not exist, or we have a bug
        return itemColors.get(id);
    }

    /**
     * Try to find a recipe in the recipe map using the given stored inputs
     *
     * @return A recipe if one was found, null otherwise
     */
    protected GT_Recipe findRecipe(ArrayList<ItemStack> inputs) {
        RecipeMap<?> recipeMap = this.getRecipeMap();
        return recipeMap.findRecipeQuery()
            .items(inputs.toArray(new ItemStack[] {}))
            .find();
    }

    /**
     * Return a parallel helper, but this should not yet be built, since we will append the item consumer to it first
     */
    protected GT_ParallelHelper createParallelHelper(GT_Recipe recipe, ItemInputInformation info) {
        return new GT_ParallelHelper().setItemInputs(this.inputFakeItems.toArray(new ItemStack[] {}))
            .setAvailableEUt(this.availableEUt)
            .enableBatchMode(0)
            .setRecipe(recipe)
            .setMachine(this, false, false)
            .setMaxParallel(Integer.MAX_VALUE)
            .setOutputCalculation(true)
            .setCalculator(GT_OverclockCalculator.ofNoOverclock(recipe));
    }

    @NotNull
    @Override
    public CheckRecipeResult checkProcessing() {
        // Reset output color
        outputColor = -1;
        currentParallel = 0;
        this.lEUt = 0;

        if (!isConnected) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        // First step in recipe checking is finding all inputs we have to deal with.
        // As a result of this process, we also get the colors of the hatch each item is found in, which
        // we will use for routing the outputs
        ItemInputInformation inputInfo = refreshInputItems();

        // Now find a recipe with the fake inputs
        GT_Recipe recipe = findRecipe(this.inputFakeItems);
        if (recipe == null) return CheckRecipeResultRegistry.NO_RECIPE;

        // Now that we know the recipe, we can figure out the color the output hatch should have
        outputColor = findOutputColor(recipe, inputInfo.colors);
        // Try to find a valid output hatch to see if we have output space available, and error if we don't.
        GT_MetaTileEntity_Hatch_VacuumConveyor_Output outputHatch = this.vacuumConveyorOutputs
            .findAnyColoredHatch(this.outputColor);
        if (outputHatch == null) {
            // TODO: Maybe add a custom result for this
            return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
        }

        // Create parallel helper to calculate parallel and consume inputs
        GT_ParallelHelper parallelHelper = createParallelHelper(recipe, inputInfo);
        // Add item consumer to parallel helper and make it consume the input items while it builds
        parallelHelper.setConsumption(true)
            .setInputConsumer(new CCInputConsumer(this.vacuumConveyorInputs))
            .build();
        CheckRecipeResult result = parallelHelper.getResult();
        if (result.wasSuccessful()) {
            // Set item outputs and parallel count. Note that while these outputs are fake, we override
            // addOutput to convert these back into CCs in the right hatch
            this.currentParallel = parallelHelper.getCurrentParallel();
            this.mOutputItems = parallelHelper.getItemOutputs();

            mEfficiency = 10000;
            mEfficiencyIncrease = 10000;
            mMaxProgresstime = recipe.mDuration;
            // Needs to be negative obviously to display correctly
            this.lEUt = -(long) recipe.mEUt * (long) this.currentParallel;
        }

        return result;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide() && isConnected) {
            super.onPostTick(aBaseMetaTileEntity, aTick);
            if (mEfficiency < 0) mEfficiency = 0;
            if (aBaseMetaTileEntity.getStoredEU() <= 0 && mMaxProgresstime > 0) {
                stopMachine(ShutDownReasonRegistry.POWER_LOSS);
            }
        }
    }

    @Override
    public long maxEUStore() {
        return euBufferSize;
    }

    @Override
    public boolean drainEnergyInput(long aEU) {
        // Drain EU from internal buffer in controller. We will need to charge this buffer from the
        // Assembly Complex
        if (aEU <= this.getEUVar()) {
            this.setEUVar(this.getEUVar() - aEU);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Increase the EU stored in the controller buffer
     *
     * @param maximumIncrease EU that should be added to the buffer
     * @return Actually used amount
     */
    public long increaseStoredEU(long maximumIncrease) {
        if (getBaseMetaTileEntity() == null) {
            return 0;
        }
        connect();
        long increasedEU = Math
            .min(getBaseMetaTileEntity().getEUCapacity() - getBaseMetaTileEntity().getStoredEU(), maximumIncrease);
        return getBaseMetaTileEntity().increaseStoredEnergyUnits(increasedEU, false) ? increasedEU : 0;
    }

    protected GT_MetaTileEntity_Hatch_VacuumConveyor_Output findOutputHatch(byte color) {
        return vacuumConveyorOutputs.findAnyColoredHatch(color);
    }

    @Override
    public boolean addOutput(ItemStack aStack) {
        // We need to override this because outputs are produced in vacuum conveyor outputs, not as real items
        if (GT_Utility.isStackInvalid(aStack)) return false;
        GT_MetaTileEntity_Hatch_VacuumConveyor_Output hatch = findOutputHatch(this.outputColor);
        if (hatch == null) {
            stopMachine(SimpleShutDownReason.ofCritical("Colored output hatch disappeared mid-recipe."));
            return false;
        }
        // Look up component from this output fake stack and unify it with the packet inside the output hatch
        CircuitComponent component = CircuitComponent.getFromFakeStack(aStack);
        CircuitComponentPacket outputPacket = new CircuitComponentPacket(component, aStack.stackSize);
        hatch.unifyPacket(outputPacket);
        return true;
    }

    public void setAvailableEUt(long eut) {
        this.availableEUt = eut;
    }

    public void connect() {
        isConnected = true;
    }

    public void disconnect() {
        isConnected = false;
        this.availableEUt = 0;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    @Override
    public boolean doRandomMaintenanceDamage() {
        // Does not get have maintenance issues
        return true;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }
}