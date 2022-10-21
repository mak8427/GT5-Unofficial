package gregtech.common.tileentities.boilers;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.UITexture;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.ProgressBar;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import gregtech.GT_Mod;
import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.enums.Materials;
import gregtech.api.enums.ParticleFX;
import gregtech.api.enums.SoundResource;
import gregtech.api.gui.modularui.GT_UIInfos;
import gregtech.api.gui.modularui.GT_UITextures;
import gregtech.api.gui.modularui.SteamTexture;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_BasicTank;
import gregtech.api.objects.GT_ItemStack;
import gregtech.api.util.GT_Log;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.WorldSpawnedEventBuilder.ParticleEventBuilder;
import gregtech.common.GT_Pollution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

public abstract class GT_MetaTileEntity_Boiler extends GT_MetaTileEntity_BasicTank {
    public static final byte SOUND_EVENT_LET_OFF_EXCESS_STEAM = 1;
    public int mTemperature = 20;
    public int mProcessingEnergy = 0;
    public int mLossTimer = 0;
    public FluidStack mSteam = null;
    public boolean mHadNoWater = false;
    private int mExcessWater = 0;

    public GT_MetaTileEntity_Boiler(
            int aID, String aName, String aNameRegional, String aDescription, ITexture... aTextures) {
        super(aID, aName, aNameRegional, 0, 4, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Boiler(
            int aID, String aName, String aNameRegional, String[] aDescription, ITexture... aTextures) {
        super(aID, aName, aNameRegional, 0, 4, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Boiler(String aName, int aTier, String aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 4, aDescription, aTextures);
    }

    public GT_MetaTileEntity_Boiler(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 4, aDescription, aTextures);
    }

    @Override
    public ITexture[] getTexture(
            IGregTechTileEntity aBaseMetaTileEntity,
            byte aSide,
            byte aFacing,
            byte aColorIndex,
            boolean aActive,
            boolean aRedstone) {
        ITexture[] tmp =
                mTextures[aSide >= 2 ? aSide != aFacing ? 2 : ((byte) (aActive ? 4 : 3)) : aSide][aColorIndex + 1];
        if (aSide != aFacing && tmp.length == 2) {
            tmp = new ITexture[] {tmp[0]};
        }
        return tmp;
    }

    @Override
    public boolean isElectric() {
        return false;
    }

    @Override
    public boolean isPneumatic() {
        return false;
    }

    @Override
    public boolean isSteampowered() {
        return false;
    }

    @Override
    public boolean isSimpleMachine() {
        return false;
    }

    @Override
    public boolean isFacingValid(byte aFacing) {
        return aFacing > 1;
    }

    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    @Override
    public boolean isValidSlot(int aIndex) {
        return true;
    }

    @Override
    public int getProgresstime() {
        return this.mTemperature;
    }

    @Override
    public int maxProgresstime() {
        return 500;
    }

    @Override
    public boolean onRightclick(IGregTechTileEntity aBaseMetaTileEntity, EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        }
        if (aPlayer != null) {
            if (GT_Utility.areStacksEqual(aPlayer.getCurrentEquippedItem(), new ItemStack(Items.water_bucket, 1))) {
                fill(Materials.Water.getFluid(1000L * (long) aPlayer.getCurrentEquippedItem().stackSize), true);

                if (!aPlayer.capabilities.isCreativeMode) {
                    aPlayer.getCurrentEquippedItem().func_150996_a(Items.bucket);
                }
            } else {
                GT_UIInfos.openGTTileEntityUI(aBaseMetaTileEntity, aPlayer);
            }
        }
        return true;
    }

    @Override
    public boolean doesFillContainers() {
        return true;
    }

    @Override
    public boolean doesEmptyContainers() {
        return true;
    }

    @Override
    public boolean canTankBeFilled() {
        return true;
    }

    @Override
    public boolean canTankBeEmptied() {
        return true;
    }

    @Override
    public boolean displaysItemStack() {
        return false;
    }

    @Override
    public boolean displaysStackSize() {
        return false;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return GT_ModHandler.isWater(aFluid);
    }

    @Override
    public FluidStack getDrainableStack() {
        return this.mSteam;
    }

    @Override
    public FluidStack setDrainableStack(FluidStack aFluid) {
        this.mSteam = aFluid;
        return this.mSteam;
    }

    @Override
    public boolean isDrainableStackSeparate() {
        return true;
    }

    @Override
    public boolean allowCoverOnSide(byte aSide, GT_ItemStack aCover) {
        return GregTech_API.getCoverBehaviorNew(aCover.toStack()).isSimpleCover();
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("mLossTimer", this.mLossTimer);
        aNBT.setInteger("mTemperature", this.mTemperature);
        aNBT.setInteger("mProcessingEnergy", this.mProcessingEnergy);
        aNBT.setInteger("mExcessWater", this.mExcessWater);
        if (this.mSteam == null) {
            return;
        }
        try {
            aNBT.setTag("mSteam", this.mSteam.writeToNBT(new NBTTagCompound()));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        this.mLossTimer = aNBT.getInteger("mLossTimer");
        this.mTemperature = aNBT.getInteger("mTemperature");
        this.mProcessingEnergy = aNBT.getInteger("mProcessingEnergy");
        this.mExcessWater = aNBT.getInteger("mExcessWater");
        this.mSteam = FluidStack.loadFluidStackFromNBT(aNBT.getCompoundTag("mSteam"));
    }

    /**
     * Produce some steam. Assume water is present.
     */
    protected void produceSteam(int aAmount) {
        mExcessWater -= aAmount;
        if (mExcessWater < 0) {
            int tWaterToConsume = -mExcessWater / GT_Values.STEAM_PER_WATER;
            mFluid.amount -= tWaterToConsume;
            mExcessWater += GT_Values.STEAM_PER_WATER * tWaterToConsume;
        }
        if (GT_ModHandler.isSteam(this.mSteam)) {
            this.mSteam.amount += aAmount;
        } else {
            this.mSteam = GT_ModHandler.getSteam(aAmount);
        }
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        pollute(aTick);

        if (isNotAllowedToWork(aBaseMetaTileEntity, aTick)) return;

        calculateCooldown();
        pushSteamToInventories(aBaseMetaTileEntity);

        if (canNotCreateSteam(aBaseMetaTileEntity, aTick)) {
            pollute(aTick);
            return;
        }

        ventSteamIfTankIsFull();
        updateFuelTimed(aBaseMetaTileEntity, aTick);
        calculateHeatUp(aBaseMetaTileEntity, aTick);
    }

    private boolean isNotAllowedToWork(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        return (!aBaseMetaTileEntity.isServerSide()) || (aTick <= 20L);
    }

    private void pollute(long aTick) {
        if (this.mProcessingEnergy > 0 && (aTick % 20L == 0L)) {
            GT_Pollution.addPollution(getBaseMetaTileEntity(), getPollution());
        }
    }

    private void calculateHeatUp(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if ((this.mTemperature < getMaxTemperature())
                && (this.mProcessingEnergy > 0)
                && (aTick % getHeatUpRate() == 0L)) {
            this.mProcessingEnergy -= getEnergyConsumption();
            this.mTemperature += getHeatUpAmount();
        }
        aBaseMetaTileEntity.setActive(this.mProcessingEnergy > 0);
    }

    private void updateFuelTimed(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if ((this.mProcessingEnergy <= 0) && (aBaseMetaTileEntity.isAllowedToWork()))
            updateFuel(aBaseMetaTileEntity, aTick);
    }

    private void ventSteamIfTankIsFull() {
        if ((this.mSteam != null) && (this.mSteam.amount > getCapacity())) {
            sendSound(SOUND_EVENT_LET_OFF_EXCESS_STEAM);
            this.mSteam.amount = getCapacity() * 3 / 4;
        }
    }

    private boolean canNotCreateSteam(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aTick % 10L != 0L) {
            return false;
        }

        if (this.mTemperature > 100) {
            if ((!GT_ModHandler.isWater(this.mFluid)) || (this.mFluid.amount <= 0)) {
                this.mHadNoWater = true;
            } else {
                if (this.mHadNoWater) {
                    GT_Log.exp.println("Boiler " + this.mName + " had no Water!");
                    onDangerousWaterLack(aBaseMetaTileEntity, aTick);
                    return true;
                }
                produceSteam(getProductionPerSecond() / 2);
            }
        } else {
            this.mHadNoWater = false;
        }
        return false;
    }

    protected void onDangerousWaterLack(IGregTechTileEntity tile, long ignoredTicks) {
        tile.doExplosion(2048L);
    }

    protected final void pushSteamToSide(IGregTechTileEntity aBaseMetaTileEntity, int aSide) {
        IFluidHandler tTileEntity = aBaseMetaTileEntity.getITankContainerAtSide((byte) aSide);
        if (tTileEntity == null) return;
        FluidStack tDrained = aBaseMetaTileEntity.drain(
                ForgeDirection.getOrientation(aSide), Math.max(1, this.mSteam.amount / 2), false);
        if (tDrained == null) return;
        int tFilledAmount =
                tTileEntity.fill(ForgeDirection.getOrientation(aSide).getOpposite(), tDrained, false);
        if (tFilledAmount <= 0) return;
        tTileEntity.fill(
                ForgeDirection.getOrientation(aSide).getOpposite(),
                aBaseMetaTileEntity.drain(ForgeDirection.getOrientation(aSide), tFilledAmount, true),
                true);
    }

    protected void pushSteamToInventories(IGregTechTileEntity aBaseMetaTileEntity) {
        for (int i = 1; (this.mSteam != null) && (i < 6); i++) {
            if (i == aBaseMetaTileEntity.getFrontFacing()) continue;
            pushSteamToSide(aBaseMetaTileEntity, i);
        }
    }

    private void calculateCooldown() {
        if (this.mTemperature <= 20) {
            this.mTemperature = 20;
            this.mLossTimer = 0;
        } else if (++this.mLossTimer > getCooldownInterval()) {
            // only loss temperature if hot
            this.mTemperature -= 1;
            this.mLossTimer = 0;
        }
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return GT_Mod.gregtechproxy.mAllowSmallBoilerAutomation;
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, byte aSide, ItemStack aStack) {
        return GT_Mod.gregtechproxy.mAllowSmallBoilerAutomation;
    }

    @Override
    public void doSound(byte aIndex, double aX, double aY, double aZ) {
        if (aIndex == GT_MetaTileEntity_Boiler.SOUND_EVENT_LET_OFF_EXCESS_STEAM) {
            GT_Utility.doSoundAtClient(SoundResource.RANDOM_FIZZ, 2, 1.0F, aX, aY, aZ);

            new ParticleEventBuilder()
                    .setIdentifier(ParticleFX.CLOUD)
                    .setWorld(getBaseMetaTileEntity().getWorld())
                    .setMotion(0D, 0D, 0D)
                    .<ParticleEventBuilder>times(8, x -> x.setPosition(
                                    aX - 0.5D + XSTR_INSTANCE.nextFloat(), aY, aZ - 0.5D + XSTR_INSTANCE.nextFloat())
                            .run());
        }
    }

    @Override
    public int getTankPressure() {
        return 100;
    }

    protected boolean isOutputToFront() {
        return false;
    }

    protected abstract int getPollution();

    @Override
    public int getCapacity() {
        return 16000;
    }

    protected abstract int getProductionPerSecond();

    protected abstract int getMaxTemperature();

    protected abstract int getEnergyConsumption();

    protected abstract int getCooldownInterval();

    protected int getHeatUpRate() {
        return 12;
    }

    protected int getHeatUpAmount() {
        return 1;
    }

    protected abstract void updateFuel(IGregTechTileEntity aBaseMetaTileEntity, long aTick);

    protected SteamTexture.Variant getVariant() {
        return SteamTexture.Variant.BRONZE;
    }

    protected IDrawable[] getFuelSlotBackground() {
        return new IDrawable[] {getSlotBackground(), GT_UITextures.OVERLAY_SLOT_COAL_STEAM.get(getVariant())};
    }

    protected IDrawable[] getAshSlotBackground() {
        return new IDrawable[] {getSlotBackground(), GT_UITextures.OVERLAY_SLOT_DUST_STEAM.get(getVariant())};
    }

    @Override
    public boolean useModularUI() {
        return true;
    }

    @Override
    protected void addUIWidgets(ModularWindow.Builder builder) {
        builder.widget(new SlotWidget(inventoryHandler, 0)
                        .setPos(43, 25)
                        .setBackground(getSlotBackground(), GT_UITextures.OVERLAY_SLOT_IN_STEAM.get(getVariant())))
                .widget(new SlotWidget(inventoryHandler, 1)
                        .setPos(43, 61)
                        .setBackground(getSlotBackground(), GT_UITextures.OVERLAY_SLOT_OUT_STEAM.get(getVariant())))
                .widget(new SlotWidget(inventoryHandler, 2).setPos(115, 61).setBackground(getFuelSlotBackground()))
                .widget(new SlotWidget(inventoryHandler, 3).setPos(115, 25).setBackground(getAshSlotBackground()))
                .widget(new ProgressBar()
                        .setProgress(() -> mSteam == null ? 0 : (float) mSteam.amount / getCapacity())
                        .setTexture(
                                GT_UITextures.PROGRESSBAR_BOILER_EMPTY_STEAM.get(getVariant()),
                                GT_UITextures.PROGRESSBAR_BOILER_STEAM,
                                10)
                        .setDirection(ProgressBar.Direction.UP)
                        .setPos(70, 25)
                        .setSize(10, 54))
                .widget(new ProgressBar()
                        .setProgress(() -> mFluid == null ? 0 : (float) mFluid.amount / getCapacity())
                        .setTexture(
                                GT_UITextures.PROGRESSBAR_BOILER_EMPTY_STEAM.get(getVariant()),
                                GT_UITextures.PROGRESSBAR_BOILER_WATER,
                                10)
                        .setDirection(ProgressBar.Direction.UP)
                        .setPos(83, 25)
                        .setSize(10, 54))
                .widget(new ProgressBar()
                        .setProgress(() -> (float) mTemperature / maxProgresstime())
                        .setTexture(
                                GT_UITextures.PROGRESSBAR_BOILER_EMPTY_STEAM.get(getVariant()),
                                GT_UITextures.PROGRESSBAR_BOILER_HEAT,
                                10)
                        .setDirection(ProgressBar.Direction.UP)
                        .setPos(96, 25)
                        .setSize(10, 54))
                .widget(new ProgressBar()
                        // cap minimum so that one can easily see there's fuel remaining
                        .setProgress(
                                () -> mProcessingEnergy > 0 ? Math.max((float) mProcessingEnergy / 1000, 1f / 5) : 0)
                        .setTexture(GT_UITextures.PROGRESSBAR_FUEL_STEAM.get(getVariant()), 14)
                        .setDirection(ProgressBar.Direction.UP)
                        .setPos(116, 45)
                        .setSize(14, 14))
                .widget(new DrawableWidget()
                        .setDrawable(GT_UITextures.OVERLAY_SLOT_CANISTER_STEAM.get(getVariant()))
                        .setPos(43, 43)
                        .setSize(18, 18));
    }

    @Override
    protected void addTitleToUI(ModularWindow.Builder builder) {
        addTitleToUI(builder, "Boiler");
    }

    @Override
    protected IDrawable getGregTechLogo() {
        return GT_UITextures.PICTURE_GT_LOGO_17x17_TRANSPARENT_STEAM.get(getVariant());
    }

    @Override
    protected UITexture getBackground() {
        return GT_UITextures.BACKGROUND_STEAM.get(getVariant());
    }

    @Override
    protected IDrawable getSlotBackground() {
        return GT_UITextures.SLOT_ITEM_STEAM.get(getVariant());
    }
}
