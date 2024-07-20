package gtPlusPlus.xmod.gregtech.registration.gregtech;

import static gtPlusPlus.xmod.gregtech.registration.gregtech.MetaTileEntityIDs.Super_Chest_EV;
import static gtPlusPlus.xmod.gregtech.registration.gregtech.MetaTileEntityIDs.Super_Chest_HV;
import static gtPlusPlus.xmod.gregtech.registration.gregtech.MetaTileEntityIDs.Super_Chest_IV;
import static gtPlusPlus.xmod.gregtech.registration.gregtech.MetaTileEntityIDs.Super_Chest_LV;
import static gtPlusPlus.xmod.gregtech.registration.gregtech.MetaTileEntityIDs.Super_Chest_MV;

import gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList;
import gtPlusPlus.xmod.gregtech.common.tileentities.storage.GT_MetaTileEntity_TieredChest;

public class GregtechSuperChests {

    public static void run() {
        String aSuffix = " [Disabled]";

        GregtechItemList.Super_Chest_LV.set(
            (new GT_MetaTileEntity_TieredChest(
                Super_Chest_LV.ID,
                "super.chest.gtpp.tier.01",
                "Super Chest I" + aSuffix,
                1)).getStackForm(1L));
        GregtechItemList.Super_Chest_MV.set(
            (new GT_MetaTileEntity_TieredChest(
                Super_Chest_MV.ID,
                "super.chest.gtpp.tier.02",
                "Super Chest II" + aSuffix,
                2)).getStackForm(1L));
        GregtechItemList.Super_Chest_HV.set(
            (new GT_MetaTileEntity_TieredChest(
                Super_Chest_HV.ID,
                "super.chest.gtpp.tier.03",
                "Super Chest III" + aSuffix,
                3)).getStackForm(1L));
        GregtechItemList.Super_Chest_EV.set(
            (new GT_MetaTileEntity_TieredChest(
                Super_Chest_EV.ID,
                "super.chest.gtpp.tier.04",
                "Super Chest IV" + aSuffix,
                4)).getStackForm(1L));
        GregtechItemList.Super_Chest_IV.set(
            (new GT_MetaTileEntity_TieredChest(
                Super_Chest_IV.ID,
                "super.chest.gtpp.tier.05",
                "Super Chest V" + aSuffix,
                5)).getStackForm(1L));

    }
}
