/*
 * Copyright (c) 2018-2020 bartimaeusnek Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions: The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package bartworks.system.material.werkstoff_loaders.recipe;

import static gregtech.api.enums.GTValues.RA;
import static gregtech.api.enums.OrePrefixes.crushed;
import static gregtech.api.enums.OrePrefixes.crushedCentrifuged;
import static gregtech.api.enums.OrePrefixes.crushedPurified;
import static gregtech.api.enums.OrePrefixes.dust;
import static gregtech.api.enums.OrePrefixes.dustImpure;
import static gregtech.api.enums.OrePrefixes.dustPure;
import static gregtech.api.enums.OrePrefixes.dustSmall;
import static gregtech.api.enums.OrePrefixes.gem;
import static gregtech.api.enums.OrePrefixes.ingot;
import static gregtech.api.enums.OrePrefixes.nugget;
import static gregtech.api.enums.OrePrefixes.ore;
import static gregtech.api.recipe.RecipeMaps.autoclaveRecipes;
import static gregtech.api.recipe.RecipeMaps.centrifugeRecipes;
import static gregtech.api.recipe.RecipeMaps.chemicalBathRecipes;
import static gregtech.api.recipe.RecipeMaps.electroMagneticSeparatorRecipes;
import static gregtech.api.recipe.RecipeMaps.hammerRecipes;
import static gregtech.api.recipe.RecipeMaps.maceratorRecipes;
import static gregtech.api.recipe.RecipeMaps.oreWasherRecipes;
import static gregtech.api.recipe.RecipeMaps.thermalCentrifugeRecipes;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeBuilder.TICKS;

import bartworks.system.material.Werkstoff;
import bartworks.system.material.werkstoff_loaders.IWerkstoffRunnable;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;

public class CrushedLoader implements IWerkstoffRunnable {

    @Override
    public void run(Werkstoff werkstoff) {
        if (!werkstoff.hasItemType(ore) || !werkstoff.hasItemType(dust)) return;

        if (werkstoff.hasItemType(ingot) && !werkstoff.getStats()
            .isBlastFurnace()) {
            if (Werkstoff.Types.ELEMENT.equals(werkstoff.getType())) {
                GTModHandler.addSmeltingRecipe(werkstoff.get(crushed), werkstoff.get(nugget, 10));
                GTModHandler.addSmeltingRecipe(werkstoff.get(crushedPurified), werkstoff.get(nugget, 10));
                GTModHandler.addSmeltingRecipe(werkstoff.get(crushedCentrifuged), werkstoff.get(nugget, 10));
            } else {
                GTModHandler.addSmeltingRecipe(werkstoff.get(crushed), werkstoff.get(ingot));
                GTModHandler.addSmeltingRecipe(werkstoff.get(crushedPurified), werkstoff.get(ingot));
                GTModHandler.addSmeltingRecipe(werkstoff.get(crushedCentrifuged), werkstoff.get(ingot));
            }
            GTModHandler.addSmeltingRecipe(werkstoff.get(dustImpure), werkstoff.get(ingot));
            GTModHandler.addSmeltingRecipe(werkstoff.get(dustPure), werkstoff.get(ingot));
            GTModHandler.addSmeltingRecipe(werkstoff.get(dust), werkstoff.get(ingot));
        }

        GTModHandler
            .addCraftingRecipe(werkstoff.get(dustImpure), new Object[] { "h  ", "W  ", 'W', werkstoff.get(crushed) });
        GTModHandler.addCraftingRecipe(
            werkstoff.get(dustPure),
            new Object[] { "h  ", "W  ", 'W', werkstoff.get(crushedPurified) });
        GTModHandler.addCraftingRecipe(
            werkstoff.get(dust),
            new Object[] { "h  ", "W  ", 'W', werkstoff.get(crushedCentrifuged) });

        GTValues.RA.stdBuilder()
            .itemInputs(werkstoff.get(crushed))
            .itemOutputs(werkstoff.get(dustImpure))
            .duration(10 * TICKS)
            .eut(16)
            .addTo(hammerRecipes);

        RA.stdBuilder()
            .itemInputs(werkstoff.get(crushed))
            .itemOutputs(werkstoff.get(dustImpure), werkstoff.getOreByProduct(0, dust))
            .outputChances(100_00, 10_00)
            .eut(2)
            .duration(20 * SECONDS)
            .addTo(maceratorRecipes);

        RA.stdBuilder()
            .itemInputs(werkstoff.get(crushed))
            .itemOutputs(
                werkstoff.get(crushedPurified),
                werkstoff.getOreByProduct(0, dust),
                GTOreDictUnificator.get(dust, Materials.Stone, 1L))
            .outputChances(100_00, 11_11, 100_00)
            .fluidInputs(Materials.Water.getFluid(1_000))
            .duration(25 * SECONDS)
            .eut(16)
            .addTo(oreWasherRecipes);

        RA.stdBuilder()
            .itemInputs(werkstoff.get(crushed))
            .itemOutputs(
                werkstoff.get(crushedPurified),
                werkstoff.getOreByProduct(0, dust),
                GTOreDictUnificator.get(dust, Materials.Stone, 1L))
            .outputChances(100_00, 11_11, 100_00)
            .fluidInputs(GTModHandler.getDistilledWater(200))
            .duration(15 * SECONDS)
            .eut(16)
            .addTo(oreWasherRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(werkstoff.get(crushed))
            .itemOutputs(
                werkstoff.get(crushedCentrifuged),
                werkstoff.getOreByProduct(1, dust),
                GTOreDictUnificator.get(dust, Materials.Stone, 1L))
            .outputChances(100_00, 11_11, 100_00)
            .duration(25 * SECONDS)
            .eut(48)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(werkstoff.get(crushedPurified))
            .itemOutputs(werkstoff.get(dustPure))
            .duration(10 * TICKS)
            .eut(16)
            .addTo(hammerRecipes);

        RA.stdBuilder()
            .itemInputs(werkstoff.get(crushedPurified))
            .itemOutputs(werkstoff.get(dustPure), werkstoff.getOreByProduct(1, dust))
            .outputChances(100_00, 10_00)
            .eut(2)
            .duration(20 * SECONDS)
            .addTo(maceratorRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(werkstoff.get(crushedPurified))
            .itemOutputs(werkstoff.get(crushedCentrifuged), werkstoff.getOreByProduct(1, dust))
            .outputChances(100_00, 11_11)
            .duration(25 * SECONDS)
            .eut(48)
            .addTo(thermalCentrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(werkstoff.get(crushedCentrifuged))
            .itemOutputs(werkstoff.get(dust))
            .duration(10 * TICKS)
            .eut(16)
            .addTo(hammerRecipes);

        RA.stdBuilder()
            .itemInputs(werkstoff.get(crushedCentrifuged))
            .itemOutputs(werkstoff.get(dust), werkstoff.getOreByProduct(2, dust))
            .outputChances(100_00, 10_00)
            .eut(2)
            .duration(20 * SECONDS)
            .addTo(maceratorRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(werkstoff.get(dustImpure))
            .itemOutputs(werkstoff.get(dust), werkstoff.getOreByProduct(0, dust))
            .outputChances(100_00, 11_11)
            .duration(
                Math.max(
                    1L,
                    werkstoff.getStats()
                        .getMass() * 8L))
            .eut(5)
            .addTo(centrifugeRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(werkstoff.get(dustPure))
            .itemOutputs(werkstoff.get(dust), werkstoff.getOreByProduct(1, dust))
            .outputChances(100_00, 11_11)
            .duration(
                Math.max(
                    1L,
                    werkstoff.getStats()
                        .getMass() * 8L))
            .eut(5)
            .addTo(centrifugeRecipes);

        if (werkstoff.contains(SubTag.CRYSTALLISABLE)) {

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(dustPure))
                .itemOutputs(werkstoff.get(gem))
                .outputChances(9000)
                .fluidInputs(Materials.Water.getFluid(200L))
                .duration(1 * MINUTES + 40 * SECONDS)
                .eut(24)
                .addTo(autoclaveRecipes);

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(dustImpure))
                .itemOutputs(werkstoff.get(gem))
                .outputChances(9000)
                .fluidInputs(Materials.Water.getFluid(200L))
                .duration(1 * MINUTES + 40 * SECONDS)
                .eut(24)
                .addTo(autoclaveRecipes);

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(dustPure))
                .itemOutputs(werkstoff.get(gem))
                .outputChances(9500)
                .fluidInputs(GTModHandler.getDistilledWater(200L))
                .duration(1 * MINUTES + 15 * SECONDS)
                .eut(24)
                .addTo(autoclaveRecipes);

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(dustImpure))
                .itemOutputs(werkstoff.get(gem))
                .outputChances(9500)
                .fluidInputs(GTModHandler.getDistilledWater(200L))
                .duration(1 * MINUTES + 15 * SECONDS)
                .eut(24)
                .addTo(autoclaveRecipes);

        }
        if (werkstoff.contains(SubTag.WASHING_MERCURY)) {

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(crushed))
                .itemOutputs(
                    werkstoff.get(crushedPurified),
                    werkstoff.getOreByProduct(1, dust),
                    GTOreDictUnificator.get(dust, Materials.Stone, 1L))
                .outputChances(10000, 7000, 4000)
                .fluidInputs(Materials.Mercury.getFluid(1_000))
                .duration(40 * SECONDS)
                .eut(8)
                .addTo(chemicalBathRecipes);

        }
        if (werkstoff.contains(SubTag.WASHING_SODIUMPERSULFATE)) {

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(crushed))
                .itemOutputs(
                    werkstoff.get(crushedPurified),
                    werkstoff.getOreByProduct(1, dust),
                    GTOreDictUnificator.get(dust, Materials.Stone, 1L))
                .outputChances(10000, 7000, 4000)
                .fluidInputs(Materials.SodiumPersulfate.getFluid(100L))
                .duration(40 * SECONDS)
                .eut(8)
                .addTo(chemicalBathRecipes);

        }
        if (werkstoff.contains(SubTag.ELECTROMAGNETIC_SEPERATION_GOLD)) {

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(dustPure))
                .itemOutputs(
                    werkstoff.get(dust),
                    GTOreDictUnificator.get(dustSmall, Materials.Gold, 1L),
                    GTOreDictUnificator.get(nugget, Materials.Gold, 1L))
                .outputChances(10000, 4000, 2000)
                .duration(20 * SECONDS)
                .eut(24)
                .addTo(electroMagneticSeparatorRecipes);

        } else if (werkstoff.contains(SubTag.ELECTROMAGNETIC_SEPERATION_IRON)) {

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(dustPure))
                .itemOutputs(
                    werkstoff.get(dust),
                    GTOreDictUnificator.get(dustSmall, Materials.Iron, 1L),
                    GTOreDictUnificator.get(nugget, Materials.Iron, 1L))
                .outputChances(10000, 4000, 2000)
                .duration(20 * SECONDS)
                .eut(24)
                .addTo(electroMagneticSeparatorRecipes);

        } else if (werkstoff.contains(SubTag.ELECTROMAGNETIC_SEPERATION_NEODYMIUM)) {

            GTValues.RA.stdBuilder()
                .itemInputs(werkstoff.get(dustPure))
                .itemOutputs(
                    werkstoff.get(dust),
                    GTOreDictUnificator.get(dustSmall, Materials.Neodymium, 1L),
                    GTOreDictUnificator.get(nugget, Materials.Neodymium, 1L))
                .outputChances(10000, 4000, 2000)
                .duration(20 * SECONDS)
                .eut(24)
                .addTo(electroMagneticSeparatorRecipes);

        }
    }
}
