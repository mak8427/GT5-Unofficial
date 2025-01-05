package goodgenerator.loader;

import static bartworks.system.material.WerkstoffLoader.*;
import static goodgenerator.api.recipe.GoodGeneratorRecipeMaps.componentAssemblyLineRecipes;
import static gregtech.api.enums.ItemList.*;
import static gregtech.api.enums.Materials.*;
import static gregtech.api.enums.OrePrefixes.*;
import static gregtech.api.enums.TierEU.RECIPE_IV;
import static gregtech.api.enums.TierEU.RECIPE_LV;
import static gregtech.api.enums.TierEU.RECIPE_MV;
import static gregtech.api.enums.TierEU.RECIPE_ULV;
import static gregtech.api.util.GTOreDictUnificator.get;
import static gregtech.api.util.GTRecipeBuilder.*;
import static gregtech.api.util.GTRecipeConstants.COAL_CASING_TIER;
import static gregtech.api.util.GTUtility.getIntegratedCircuit;
import static gtPlusPlus.core.material.MaterialsAlloy.*;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;

// spotless:off
/**
 * <h3>Guide to making Component Assembly Line (CoAL) recipes</h3>
 * <pre>
 * <ul>
 *     <li>Match the order of the Assembly Line/Assembler recipe as best as possible.
 *     <li>Duplicated recipes for multiple inputs (i.e., SBR vs Silicone) should be respected.
 *     <li>Multiply all inputs by 48x, but output 64 at a time.
 * </ul>
 * Item conversion rules (in case of odd numbers, round down):
 * <ul>
 *     <li>All wires/cables should convert to 16x sizes (excluding fine wires).
 *     <li>All plates should convert to dense plates.
 *     <li>All circuits should convert to circuit wraps.
 *     <li>All rods should convert to long rods.
 *     <li>All small gears should convert into gears.
 *     <li>16 Gravi Stars -> 1 Nuclear Star for UHV+.
 *     <li>16 Tier N Nanites -> 1 Tier N+1 Nanite (i.e., 16 Neutronium -> 1 Gold).
 * </ul>
 * Fluid conversion rules:
 * <ul>
 *     <li>Convert metal items to fluid IF the stack size exceeds 64.
 *     <li>Convert fluids to their "basic" form (i.e., Magnetic Samarium -> Samarium).
 * </ul>
 * Circuit Numbers (LuV+ only):
 * <ul>
 *     <li>None: Field Generator
 *     <li>1: Motor
 *     <li>2: Piston
 *     <li>3: Pump
 *     <li>4: Robot Arm
 *     <li>5: Conveyor
 *     <li>6: Emitter
 *     <li>7: Sensor
 * </ul>
 * </pre>
 */
// todo rename after done
public class ComponentAssemblyLineRecipeLoader2 {

    private static final int L = (int) GTValues.L;
    private static final int // todo double check tiers
        COAL_LV  = 0,
        COAL_MV  = 1,
        COAL_HV  = 2,
        COAL_EV  = 3,
        COAL_IV  = 4,
        COAL_LuV = 5,
        COAL_ZPM = 6,
        COAL_UV  = 7,
        COAL_UHV = 8,
        COAL_UEV = 9,
        COAL_UIV = 10,
        COAL_UMV = 11,
        COAL_UXV = 12;

    private static final int
        MOTOR_CIRCUIT     = 1,
        PISTON_CIRCUIT    = 2,
        PUMP_CIRCUIT      = 3,
        ROBOT_ARM_CIRCUIT = 4,
        CONVEYOR_CIRCUIT  = 5,
        EMITTER_CIRCUIT   = 6,
        SENSOR_CIRCUIT    = 7;

    public static void run() {
        ComponentAssemblyLineMiscRecipes.run();

        lvRecipes();
        mvRecipes();
        hvRecipes();
        evRecipes();
        ivRecipes();
        luvRecipes();
        zpmRecipes();
        uvRecipes();
        uhvRecipes();
        uevRecipes();
        uivRecipes();
        umvRecipes();
        uxvRecipes();
    }

    private static void lvRecipes() {
        // Motor
        for (var copper : new Materials[] { Copper, AnnealedCopper }) {
            GTValues.RA.stdBuilder()
                .itemOutputs(Electric_Motor_LV.get(64))
                .itemInputs(
                    get(stickLong, IronMagnetic, 24),
                    get(stickLong, Iron, 48),
                    get(wireGt16, copper, 12),
                    get(cableGt16, Tin, 6))
                .duration(48 * SECONDS)
                .eut(RECIPE_ULV)
                .metadata(COAL_CASING_TIER, COAL_LV)
                .addTo(componentAssemblyLineRecipes);

            GTValues.RA.stdBuilder()
                .itemOutputs(Electric_Motor_LV.get(64))
                .itemInputs(
                    get(stickLong, SteelMagnetic, 24),
                    get(stickLong, Steel, 48),
                    get(wireGt16, copper, 12),
                    get(cableGt16, Tin, 6))
                .duration(48 * SECONDS)
                .eut(RECIPE_ULV)
                .metadata(COAL_CASING_TIER, COAL_LV)
                .addTo(componentAssemblyLineRecipes);
        }

        // Piston
        GTValues.RA.stdBuilder()
            .itemOutputs(Electric_Piston_LV.get(64))
            .itemInputs(
                Electric_Motor_LV.get(48),
                get(plateDense, Steel, 16),
                get(stickLong, Steel, 48),
                get(cableGt16, Tin, 6),
                get(gearGt, Steel, 12))
            .duration(48 * SECONDS)
            .eut(RECIPE_ULV)
            .metadata(COAL_CASING_TIER, COAL_LV)
            .addTo(componentAssemblyLineRecipes);

        // Robot Arm
        GTValues.RA.stdBuilder()
            .itemOutputs(Robot_Arm_LV.get(64))
            .itemInputs(
                Electric_Motor_LV.get(96),
                Electric_Piston_LV.get(48),
                get(stickLong, Steel, 48),
                get(wrapCircuit, LV, 3),
                get(cableGt16, Tin, 9))
            .duration(48 * SECONDS)
            .eut(RECIPE_ULV)
            .metadata(COAL_CASING_TIER, COAL_LV)
            .addTo(componentAssemblyLineRecipes);

        for (var rubber : new Materials[] { Rubber, Silicone, StyreneButadieneRubber }) {
            // Pump
            GTValues.RA.stdBuilder()
                .itemOutputs(Electric_Pump_LV.get(64))
                .itemInputs(
                    Electric_Motor_LV.get(48),
                    get(rotor, Tin, 48),
                    get(screw, Tin, 48),
                    get(cableGt16, Tin, 3),
                    get(pipe, Bronze, 48))
                .fluidInputs(
                    rubber.getMolten(24 * L))
                .duration(48 * SECONDS)
                .eut(RECIPE_ULV)
                .metadata(COAL_CASING_TIER, COAL_LV)
                .addTo(componentAssemblyLineRecipes);

            // Conveyor
            GTValues.RA.stdBuilder()
                .itemOutputs(Conveyor_Module_LV.get(64))
                .itemInputs(
                    Electric_Motor_LV.get(96),
                    get(plateDense, rubber, 32),
                    get(cableGt16, Tin, 3))
                .duration(48 * SECONDS)
                .eut(RECIPE_ULV)
                .metadata(COAL_CASING_TIER, COAL_LV)
                .addTo(componentAssemblyLineRecipes);
        }

        // Sensor
        GTValues.RA.stdBuilder()
            .itemOutputs(Sensor_LV.get(64))
            .itemInputs(
                get(gem, CertusQuartz, 48),
                get(plateDense, Steel, 21),
                get(stickLong, Brass, 24),
                get(wrapCircuit, LV, 3))
            .duration(48 * SECONDS)
            .eut(RECIPE_ULV)
            .metadata(COAL_CASING_TIER, COAL_LV)
            .addTo(componentAssemblyLineRecipes);

        // Emitter
        GTValues.RA.stdBuilder()
            .itemOutputs(Emitter_LV.get(64))
            .itemInputs(
                get(gem, CertusQuartz, 48),
                get(wrapCircuit, LV, 6),
                get(cableGt16, Tin, 6))
            .fluidInputs(
                Brass.getMolten(96 * L))
            .duration(48 * SECONDS)
            .eut(RECIPE_ULV)
            .metadata(COAL_CASING_TIER, COAL_LV)
            .addTo(componentAssemblyLineRecipes);

        // Field Generator
        GTValues.RA.stdBuilder()
            .itemOutputs(Field_Generator_LV.get(64))
            .itemInputs(
                get(plate, EnderPearl, 48),
                get(wrapCircuit, HV, 12))
            .fluidInputs(
                RedSteel.getMolten(96 * L))
            .duration(24 * MINUTES)
            .eut(RECIPE_ULV)
            .metadata(COAL_CASING_TIER, COAL_LV)
            .addTo(componentAssemblyLineRecipes);
    }

    private static void mvRecipes() {
        // Motor
        for (var copper : new Materials[] { Copper, AnnealedCopper }) {
            GTValues.RA.stdBuilder()
                .itemOutputs(Electric_Motor_MV.get(64))
                .itemInputs(
                    get(stickLong, SteelMagnetic, 24),
                    get(stickLong, Aluminium, 48),
                    get(wireGt16, Cupronickel, 24),
                    get(cableGt16, copper, 6))
                .duration(48 * SECONDS)
                .eut(RECIPE_LV)
                .metadata(COAL_CASING_TIER, COAL_MV)
                .addTo(componentAssemblyLineRecipes);
        }

        // Piston
        GTValues.RA.stdBuilder()
            .itemOutputs(Electric_Piston_MV.get(64))
            .itemInputs(
                Electric_Motor_MV.get(48),
                get(plateDense, Aluminium, 16),
                get(stickLong, Aluminium, 48),
                get(cableGt16, Copper, 6),
                get(gearGt, Aluminium, 12))
            .duration(48 * SECONDS)
            .eut(RECIPE_LV)
            .metadata(COAL_CASING_TIER, COAL_MV)
            .addTo(componentAssemblyLineRecipes);

        // Robot Arm
        GTValues.RA.stdBuilder()
            .itemOutputs(Robot_Arm_MV.get(64))
            .itemInputs(
                Electric_Motor_MV.get(96),
                Electric_Piston_MV.get(48),
                get(stickLong, Aluminium, 48),
                get(wrapCircuit, MV, 3),
                get(cableGt16, Copper, 9))
            .duration(48 * SECONDS)
            .eut(RECIPE_LV)
            .metadata(COAL_CASING_TIER, COAL_MV)
            .addTo(componentAssemblyLineRecipes);

        for (var rubber : new Materials[] { Rubber, Silicone, StyreneButadieneRubber }) {
            // Pump
            GTValues.RA.stdBuilder()
                .itemOutputs(Electric_Pump_MV.get(64))
                .itemInputs(
                    Electric_Motor_MV.get(48),
                    get(rotor, Bronze, 48),
                    get(screw, Bronze, 48),
                    get(cableGt16, Copper, 3),
                    get(pipe, Steel, 48))
                .fluidInputs(
                    rubber.getMolten(24 * L))
                .duration(48 * SECONDS)
                .eut(RECIPE_LV)
                .metadata(COAL_CASING_TIER, COAL_MV)
                .addTo(componentAssemblyLineRecipes);

            // Conveyor
            GTValues.RA.stdBuilder()
                .itemOutputs(Conveyor_Module_MV.get(64))
                .itemInputs(
                    Electric_Motor_MV.get(96),
                    get(plateDense, rubber, 32),
                    get(cableGt16, Copper, 3))
                .duration(48 * SECONDS)
                .eut(RECIPE_LV)
                .metadata(COAL_CASING_TIER, COAL_MV)
                .addTo(componentAssemblyLineRecipes);
        }

        // Sensor
        GTValues.RA.stdBuilder()
            .itemOutputs(Sensor_MV.get(64))
            .itemInputs(
                get(gemFlawless, Emerald, 48), // todo exquisite?
                get(plateDense, Aluminium, 21),
                get(stickLong, Electrum, 24),
                get(wrapCircuit, MV, 3))
            .duration(48 * SECONDS)
            .eut(RECIPE_LV)
            .metadata(COAL_CASING_TIER, COAL_MV)
            .addTo(componentAssemblyLineRecipes);

        // Emitter
        GTValues.RA.stdBuilder()
            .itemOutputs(Emitter_MV.get(64))
            .itemInputs(
                get(gem, EnderPearl, 48),
                get(wrapCircuit, MV, 6),
                get(cableGt16, Copper, 6))
            .fluidInputs(
                Electrum.getMolten(96 * L))
            .duration(48 * SECONDS)
            .eut(RECIPE_LV)
            .metadata(COAL_CASING_TIER, COAL_MV)
            .addTo(componentAssemblyLineRecipes);

        // Field Generator
        GTValues.RA.stdBuilder()
            .itemOutputs(Field_Generator_MV.get(64))
            .itemInputs(
                get(plate, EnderEye, 48),
                get(wrapCircuit, EV, 12))
            .fluidInputs(
                TungstenSteel.getMolten(96 * L))
            .duration(24 * MINUTES)
            .eut(RECIPE_LV)
            .metadata(COAL_CASING_TIER, COAL_MV)
            .addTo(componentAssemblyLineRecipes);
    }

    private static void hvRecipes() {
        // Motor
        GTValues.RA.stdBuilder()
            .itemOutputs(Electric_Motor_HV.get(64))
            .itemInputs(
                get(stickLong, SteelMagnetic, 24),
                get(stickLong, StainlessSteel, 48),
                get(wireGt16, Electrum, 48),
                get(cableGt16, Silver, 12))
            .duration(48 * SECONDS)
            .eut(RECIPE_MV)
            .metadata(COAL_CASING_TIER, COAL_HV)
            .addTo(componentAssemblyLineRecipes);

        // Piston
        GTValues.RA.stdBuilder()
            .itemOutputs(Electric_Piston_HV.get(64))
            .itemInputs(
                Electric_Motor_HV.get(48),
                get(plateDense, StainlessSteel, 16),
                get(stickLong, StainlessSteel, 48),
                get(cableGt16, Gold, 6),
                get(gearGt, StainlessSteel, 12))
            .duration(48 * SECONDS)
            .eut(RECIPE_MV)
            .metadata(COAL_CASING_TIER, COAL_HV)
            .addTo(componentAssemblyLineRecipes);

        // Robot Arm
        GTValues.RA.stdBuilder()
            .itemOutputs(Robot_Arm_HV.get(64))
            .itemInputs(
                Electric_Motor_HV.get(96),
                Electric_Piston_HV.get(48),
                get(stickLong, StainlessSteel, 48),
                get(wrapCircuit, HV, 3),
                get(cableGt16, Gold, 9))
            .duration(48 * SECONDS)
            .eut(RECIPE_MV)
            .metadata(COAL_CASING_TIER, COAL_HV)
            .addTo(componentAssemblyLineRecipes);

        for (var rubber : new Materials[] { Rubber, Silicone, StyreneButadieneRubber }) {
            // Pump
            GTValues.RA.stdBuilder()
                .itemOutputs(Electric_Pump_HV.get(64))
                .itemInputs(
                    Electric_Motor_HV.get(48),
                    get(rotor, Steel, 48),
                    get(screw, Steel, 48),
                    get(cableGt16, Gold, 3),
                    get(pipe, StainlessSteel, 48))
                .fluidInputs(
                    rubber.getMolten(24 * L))
                .duration(48 * SECONDS)
                .eut(RECIPE_MV)
                .metadata(COAL_CASING_TIER, COAL_HV)
                .addTo(componentAssemblyLineRecipes);

            // Conveyor
            GTValues.RA.stdBuilder()
                .itemOutputs(Conveyor_Module_HV.get(64))
                .itemInputs(
                    Electric_Motor_HV.get(96),
                    get(plateDense, rubber, 32),
                    get(cableGt16, Gold, 3))
                .duration(48 * SECONDS)
                .eut(RECIPE_MV)
                .metadata(COAL_CASING_TIER, COAL_HV)
                .addTo(componentAssemblyLineRecipes);
        }

        // Sensor
        GTValues.RA.stdBuilder()
            .itemOutputs(Sensor_HV.get(64))
            .itemInputs(
                get(gem, EnderEye, 48),
                get(plateDense, StainlessSteel, 21),
                get(stickLong, Chrome, 24),
                get(wrapCircuit, HV, 3))
            .duration(48 * SECONDS)
            .eut(RECIPE_MV)
            .metadata(COAL_CASING_TIER, COAL_HV)
            .addTo(componentAssemblyLineRecipes);

        // Emitter
        GTValues.RA.stdBuilder()
            .itemOutputs(Emitter_HV.get(64))
            .itemInputs(
                get(gem, EnderEye, 48),
                get(wrapCircuit, HV, 6),
                get(cableGt16, Gold, 6))
            .fluidInputs(
                Chrome.getMolten(96 * L))
            .duration(48 * SECONDS)
            .eut(RECIPE_MV)
            .metadata(COAL_CASING_TIER, COAL_HV)
            .addTo(componentAssemblyLineRecipes);

        // Field Generator
        GTValues.RA.stdBuilder()
            .itemOutputs(Field_Generator_HV.get(64))
            .itemInputs(
                QuantumEye.get(48),
                get(wrapCircuit, IV, 12))
            .fluidInputs(
                NiobiumTitanium.getMolten(192 * L))
            .duration(24 * MINUTES)
            .eut(RECIPE_MV)
            .metadata(COAL_CASING_TIER, COAL_HV)
            .addTo(componentAssemblyLineRecipes);
    }

    private static void evRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void ivRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void luvRecipes() {
        // Motor
        GTValues.RA.stdBuilder()
            .itemOutputs(Electric_Motor_LuV.get(64))
            .itemInputs(
                get(cableGt16, YttriumBariumCuprate, 6),
                get(stickLong, SamariumMagnetic, 24),
                getIntegratedCircuit(1))
            .fluidInputs(
                INDALLOY_140.getFluidStack(48 * L),
                Lubricant.getFluid(12000),
                Ruridit.getMolten(768 * L),
                HSSS.getMolten(96 * L))
            .duration(24 * MINUTES)
            .eut(RECIPE_IV)
            .metadata(COAL_CASING_TIER, COAL_LuV)
            .addTo(componentAssemblyLineRecipes);

        // Piston
        GTValues.RA.stdBuilder()
            .itemOutputs(Electric_Piston_LuV.get(64))
            .itemInputs(
                get(cableGt16, YttriumBariumCuprate, 12),
                Electric_Motor_LuV.get(48),
                get(plateDense, HSSS, 32),
                getIntegratedCircuit(2))
            .fluidInputs(
                INDALLOY_140.getFluidStack(48 * L),
                Lubricant.getFluid(12000),
                HSSS.getMolten(602 * L + 96))
            .duration(24 * MINUTES)
            .eut(RECIPE_IV)
            .metadata(COAL_CASING_TIER, COAL_LuV)
            .addTo(componentAssemblyLineRecipes);

        // Pump
        // Conveyor
        GTValues.RA.stdBuilder()
            .itemOutputs(Conveyor_Module_LuV.get(64))
            .itemInputs(
                Electric_Motor_LuV.get(96),
                get(plateDense, HSSS, 10),
                get(cableGt16, YttriumBariumCuprate, 6),
                get(plateDense, Silicone, 53)) // todo sbr?
            .fluidInputs(
                INDALLOY_140.getFluidStack(48 * L),
                Lubricant.getFluid(12000),
                HSSS.getMolten(218 * L + 96))
            .duration(24 * MINUTES)
            .eut(RECIPE_IV)
            .metadata(COAL_CASING_TIER, COAL_LuV)
            .addTo(componentAssemblyLineRecipes);

        // Robot Arm
        // Sensor
        // Emitter
        GTValues.RA.stdBuilder()
            .itemOutputs(Emitter_LuV.get(64))
            .itemInputs(
                get(cableGt16, YttriumBariumCuprate, 21),
                Electric_Motor_LuV.get(48),
                get(frameGt, HSSS, 48),
                get(wrapCircuit, LuV, 12),
                QuantumStar.get(48),
                getIntegratedCircuit(6))
            .fluidInputs(
                INDALLOY_140.getFluidStack(192 * L),
                Gallium.getMolten(2304 * L),
                Ruridit.getMolten(192 * L))
            .duration(24 * MINUTES)
            .eut(RECIPE_IV)
            .metadata(COAL_CASING_TIER, COAL_LuV)
            .addTo(componentAssemblyLineRecipes);

        // Field Generator
        GTValues.RA.stdBuilder()
            .itemOutputs(Field_Generator_LuV.get(64))
            .itemInputs(
                get(frameGt, HSSS, 48),
                get(plateDense, HSSS, 32),
                QuantumStar.get(96),
                Emitter_LuV.get(192),
                get(wrapCircuit, Materials.ZPM, 12),
                get(cableGt16, YttriumBariumCuprate, 24))
            .fluidInputs(
                INDALLOY_140.getFluidStack(192 * L),
                Ruridit.getMolten(1536 * L))
            .duration(24 * MINUTES)
            .eut(RECIPE_IV)
            .metadata(COAL_CASING_TIER, COAL_LuV)
            .addTo(componentAssemblyLineRecipes);
    }

    private static void zpmRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void uvRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void uhvRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void uevRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void uivRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void umvRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }

    private static void uxvRecipes() {
        // Motor
        // Piston
        // Robot Arm
        // Pump
        // Conveyor
        // Sensor
        // Emitter
        // Field Generator
    }
}
// spotless:on
