package gregtech.common.tileentities.machines.multi.compressor;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.enums.GT_HatchElement.*;
import static gregtech.api.enums.GT_Values.AuthorFourIsTheNumber;
import static gregtech.api.enums.GT_Values.Ollie;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_MULTI_COMPRESSOR;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_MULTI_COMPRESSOR_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_MULTI_COMPRESSOR_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_MULTI_COMPRESSOR_GLOW;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;
import static gregtech.api.util.GT_StructureUtility.ofFrame;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.MaterialsUEVplus;
import gregtech.api.enums.Textures;
import gregtech.api.gui.modularui.GT_UITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_ExtendedPowerMultiBlockBase;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Input;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.shutdown.SimpleShutDownReason;
import gregtech.common.blocks.GT_Block_Casings10;
import gregtech.common.items.GT_MetaGenerated_Item_01;
import gtPlusPlus.core.util.minecraft.PlayerUtils;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class GT_MetaTileEntity_BlackHoleCompressor
    extends GT_MetaTileEntity_ExtendedPowerMultiBlockBase<GT_MetaTileEntity_BlackHoleCompressor>
    implements ISurvivalConstructable {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final IStructureDefinition<GT_MetaTileEntity_BlackHoleCompressor> STRUCTURE_DEFINITION = StructureDefinition
        .<GT_MetaTileEntity_BlackHoleCompressor>builder()
        .addShape(
            STRUCTURE_PIECE_MAIN,
            // spotless:off
            transpose(new String[][]{
                {"                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","             CCC   CCC             ","            CCC     CCC            ","         CCCCC       CCCCC         ","         CCCC         CCCC         ","                                   ","                                   ","                                   ","         CCCC         CCCC         ","         CCCCC       CCCCC         ","            CCC     CCC            ","             CCC   CCC             ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","             CCC   CCC             ","             CCC   CCC             ","            CCCC   CCCC            ","           CCCCC   CCCCC           ","         CCCCCC     CCCCCC         ","       CCCCCCC       CCCCCCC       ","       CCCCCC         CCCCCC       ","                                   ","                                   ","                                   ","       CCCCCC         CCCCCC       ","       CCCCCCC       CCCCCCC       ","         CCCCCC     CCCCCC         ","           CCCCC   CCCCC           ","            CCCC   CCCC            ","             CCC   CCC             ","             CCC   CCC             ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CCBBBCC              ","            BBCCCCCCCBB            ","           BCCCCCCCCCCCB           ","          BCCCCCCCCCCCCCB          ","         BCCCCCCCCCCCCCCCB         ","        BCCCCCCCCCCCCCCCCCB        ","       BCCCCCCCBBBBBCCCCCCCB       ","       BCCCCCCB     BCCCCCCB       ","      CCCCCCCB       BCCCCCCC      ","      CCCCCCB   BBB   BCCCCCC      ","      BCCCCCB  B   B  BCCCCCB      ","      BCCCCCB  B   B  BCCCCCB      ","      BCCCCCB  B   B  BCCCCCB      ","      CCCCCCB   BBB   BCCCCCC      ","      CCCCCCCB       BCCCCCCC      ","       BCCCCCCB     BCCCCCCB       ","       BCCCCCCCBBBBBCCCCCCCB       ","        BCCCCCCCCCCCCCCCCCB        ","         BCCCCCCCCCCCCCCCB         ","          BCCCCCCCCCCCCCB          ","           BCCCCCCCCCCCB           ","            BBCCCCCCCBB            ","              CCBBBCC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCBBBCC              ","            BBCCCCCCCBB            ","           BCCCCCCCCCCCB           ","          BCCCCCCCCCCCCCB          ","         BCCCCCCCCCCCCCCCB         ","        BCCCCCCCCCCCCCCCCCB        ","       BCCCCCCCBBBBBCCCCCCCB       ","       BCCCCCCBCCCCCBCCCCCCB       ","     CCCCCCCCBCCCCCCCBCCCCCCCC     ","     CCCCCCCBCCCCCCCCCBCCCCCCC     ","      BCCCCCBCCCCCCCCCBCCCCCB      ","      BCCCCCBCCCCCCCCCBCCCCCB      ","      BCCCCCBCCCCCCCCCBCCCCCB      ","     CCCCCCCBCCCCCCCCCBCCCCCCC     ","     CCCCCCCCBCCCCCCCBCCCCCCCC     ","       BCCCCCCBCCCCCBCCCCCCB       ","       BCCCCCCCBBBBBCCCCCCCB       ","        BCCCCCCCCCCCCCCCCCB        ","         BCCCCCCCCCCCCCCCB         ","          BCCCCCCCCCCCCCB          ","           BCCCCCCCCCCCB           ","            BBCCCCCCCBB            ","              CCBBBCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","              CCBBBCC              ","            BBCCCCCCCBB            ","           BCCCCCCCCCCCB           ","          BCCCCCCCCCCCCCB          ","         BCCCCCCCCCCCCCCCB         ","        BCCCCCCCCCCCCCCCCCB        ","       BCCCCCCCBBBBBCCCCCCCB       ","       BCCCCCCB     BCCCCCCB       ","    CCCCCCCCCB       BCCCCCCCCC    ","    CCCCCCCCB         BCCCCCCCC    ","     DBCCCCCB         BCCCCCBD     ","     DBCCCCCB         BCCCCCBD     ","     DBCCCCCB         BCCCCCBD     ","    CCCCCCCCB         BCCCCCCCC    ","    CCCCCCCCCB       BCCCCCCCCC    ","       BCCCCCCB     BCCCCCCB       ","       BCCCCCCCBBBBBCCCCCCCB       ","        BCCCCCCCCCCCCCCCCCB        ","         BCCCCCCCCCCCCCCCB         ","          BCCCCCCCCCCCCCB          ","           BCCCCCCCCCCCB           ","            BBCCCCCCCBB            ","              CCBBBCC              ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","          BB           BB          ","         BBBB  CCCCC  BBBB         ","         BBBBCC     CCBBBB         ","          BBC         CBB          ","           C           C           ","   CCC     C           C     CCC   ","   CCC    C             C    CCC   ","          C             C          ","          C             C          ","          C             C          ","   CCC    C             C    CCC   ","   CCC     C           C     CCC   ","           C           C           ","          BBC         CBB          ","         BBBBCC     CCBBBB         ","         BBBB  CCCCC  BBBB         ","          BB           BB          ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","           BBB CCCCC BBB           ","           BBCC     CCBB           ","           BC         CB           ","   CC       C         C       CC   ","   CC      C           C      CC   ","    D      C           C      D    ","    D      C           C      D    ","    D      C           C      D    ","   CC      C           C      CC   ","   CC       C         C       CC   ","           BC         CB           ","           BBCC     CCBB           ","           BBB CCCCC BBB           ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","            BBBCCCCCBBB            ","            BBC     CBB            ","  CCC       BC       CB       CCC  ","  CCC       C         C       CCC  ","            C         C            ","            C         C            ","            C         C            ","  CCC       C         C       CCC  ","  CCC       BC       CB       CCC  ","            BBC     CBB            ","            BBBCCCCCBBB            ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   "},
                {"                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","             BBCCCCCBB             ","  CC         BC     CB         CC  ","  CC         C       C         CC  ","   D         C       C         D   ","   D         C       C         D   ","   D         C       C         D   ","  CC         C       C         CC  ","  CC         BC     CB         CC  ","             BBCCCCCBB             ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   "},
                {"                                   ","                                   ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              BCCCCCB              ","  CC         BC     CB         CC  ","  CC         C       C         CC  ","             C       C             ","             C       C             ","             C       C             ","  CC         C       C         CC  ","  CC         BC     CB         CC  ","              BCCCCCB              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","                                   ","                                   "},
                {"                                   ","              CC   CC              ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CCC          BBCCCBB          CCC "," CCC          BC   CB          CCC ","  D           C     C           D  ","  D           C     C           D  ","  D           C     C           D  "," CCC          BC   CB          CCC "," CCC          BBCCCBB          CCC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","              CC   CC              ","                                   "},
                {"                                   ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CC            BCCCB            CC "," CC           BC   CB           CC ","              C     C              ","              C     C              ","              C     C              "," CC           BC   CB           CC "," CC            BCCCB            CC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","                                   "},
                {"                                   ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CC             BBB             CC "," CC            BCCCB            CC "," D            BCCCCCB            D "," D            BCCCCCB            D "," D            BCCCCCB            D "," CC            BCCCB            CC "," CC             BBB             CC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","                                   "},
                {"                                   ","              CC   CC              ","              CCBBBCC              ","                CCC                ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CC                             CC "," CC             BBB             CC ","  BC           B   B           CB  ","  BC           B   B           CB  ","  BC           B   B           CB  "," CC             BBB             CC "," CC                             CC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                CCC                ","              CCBBBCC              ","              CC   CC              ","                                   "},
                {"              CCDDDCC              ","              CC   CC              ","              CBBBBBC              ","               C   C               ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","CCC                             CCC","CCBC                           CBCC","D B                             B D","D B                             B D","D B                             B D","CCBC                           CBCC","CCC                             CCC","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","               C   C               ","              CBBBBBC              ","              CC   CC              ","              CCDDDCC              "},
                {"              CC   CC              ","              CCCCCCC              ","              BBBBBBB              ","              C ABA C              ","                ABA                ","                 A                 ","                 A                 ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","CCBC                           CBCC","CCB                             BCC"," CBAA                         AABC "," CBBBAA                     AABBBC "," CBAA                         AABC ","CCB                             BCC","CCBC                           CBCC","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                 A                 ","                 A                 ","                ABA                ","              C ABA C              ","              BBBBBBB              ","              CCCCCCC              ","              CC   CC              "},
                {"              CC   CC              ","              CCCECCC              ","              BBBBBBB              ","              C BBB C              ","                BBB                ","                ABA                ","                ABA                ","                 B                 ","                 B                 ","                                   ","                                   ","                                   ","                                   ","                                   ","CCBC                           CBCC","CCB                             BCC"," CBBBAA                     AABBBC "," EBBBBBBB                 BBBBBBBE "," CBBBAA                     AABBBC ","CCB                             BCC","CCBC                           CBCC","                                   ","                                   ","                                   ","                                   ","                                   ","                 B                 ","                 B                 ","                ABA                ","                ABA                ","                BBB                ","              C BBB C              ","              BBBBBBB              ","              CCCECCC              ","              CC   CC              "},
                {"              CC   CC              ","              CCCCCCC              ","              BBBBBBB              ","              C ABA C              ","                ABA                ","                 A                 ","                 A                 ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","CCBC                           CBCC","CCB                             BCC"," CBAA                         AABC "," CBBBAA                     AABBBC "," CBAA                         AABC ","CCB                             BCC","CCBC                           CBCC","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                 A                 ","                 A                 ","                ABA                ","              C ABA C              ","              BBBBBBB              ","              CCCCCCC              ","              CC   CC              "},
                {"              CCDDDCC              ","              CC   CC              ","              CBBBBBC              ","               C   C               ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","CCC                             CCC","CCBC                           CBCC","D B                             B D","D B                             B D","D B                             B D","CCBC                           CBCC","CCC                             CCC","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","               C   C               ","              CBBBBBC              ","              CC   CC              ","              CCDDDCC              "},
                {"                                   ","              CC   CC              ","              CCBBBCC              ","                CCC                ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CC                             CC "," CC             BBB             CC ","  BC           B   B           CB  ","  BC           B   B           CB  ","  BC           B   B           CB  "," CC             BBB             CC "," CC                             CC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                CCC                ","              CCBBBCC              ","              CC   CC              ","                                   "},
                {"                                   ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CC             BBB             CC "," CC            BCCCB            CC "," D            BCCCCCB            D "," D            BCCCCCB            D "," D            BCCCCCB            D "," CC            BCCCB            CC "," CC             BBB             CC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","                                   "},
                {"                                   ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CC            BCCCB            CC "," CC           BC   CB           CC ","              C     C              ","              C     C              ","              C     C              "," CC           BC   CB           CC "," CC            BCCCB            CC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","                                   "},
                {"                                   ","              CC   CC              ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "," CCC          BBCCCBB          CCC "," CCC          BC   CB          CCC ","  D           C     C           D  ","  D           C     C           D  ","  D           C     C           D  "," CCC          BC   CB          CCC "," CCC          BBCCCBB          CCC ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","              CC   CC              ","                                   "},
                {"                                   ","                                   ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              BCCCCCB              ","  CC         BC     CB         CC  ","  CC         C       C         CC  ","             C       C             ","             C       C             ","             C       C             ","  CC         C       C         CC  ","  CC         BC     CB         CC  ","              BCCCCCB              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","                                   ","                                   "},
                {"                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","             BBCCCCCBB             ","  CC         BC     CB         CC  ","  CC         C       C         CC  ","   D         C       C         D   ","   D         C       C         D   ","   D         C       C         D   ","  CC         C       C         CC  ","  CC         BC     CB         CC  ","             BBCCCCCBB             ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   "},
                {"                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","            BBBCCCCCBBB            ","            BBC     CBB            ","  CCC       BC       CB       CCC  ","  CCC       C         C       CCC  ","            C         C            ","            C         C            ","            C         C            ","  CCC       C         C       CCC  ","  CCC       BC       CB       CCC  ","            BBC     CBB            ","            BBBCCCCCBBB            ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","           BBB CCCCC BBB           ","           BBCC     CCBB           ","           BC         CB           ","   CC       C         C       CC   ","   CC      C           C      CC   ","    D      C           C      D    ","    D      C           C      D    ","    D      C           C      D    ","   CC      C           C      CC   ","   CC       C         C       CC   ","           BC         CB           ","           BBCC     CCBB           ","           BBB CCCCC BBB           ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","          BB           BB          ","         BBBB  CC~CC  BBBB         ","         BBBBCC     CCBBBB         ","          BBC         CBB          ","           C           C           ","   CCC     C           C     CCC   ","   CCC    C             C    CCC   ","          C             C          ","          C             C          ","          C             C          ","   CCC    C             C    CCC   ","   CCC     C           C     CCC   ","           C           C           ","          BBC         CBB          ","         BBBBCC     CCBBBB         ","         BBBB  CCCCC  BBBB         ","          BB           BB          ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCDDDCC              ","              CCBBBCC              ","            BBCCCCCCCBB            ","           BCCCCCCCCCCCB           ","          BCCCCCCCCCCCCCB          ","         BCCCCCCCCCCCCCCCB         ","        BCCCCCCCCCCCCCCCCCB        ","       BCCCCCCCBBBBBCCCCCCCB       ","       BCCCCCCB     BCCCCCCB       ","    CCCCCCCCCB       BCCCCCCCCC    ","    CCCCCCCCB         BCCCCCCCC    ","     DBCCCCCB         BCCCCCBD     ","     DBCCCCCB         BCCCCCBD     ","     DBCCCCCB         BCCCCCBD     ","    CCCCCCCCB         BCCCCCCCC    ","    CCCCCCCCCB       BCCCCCCCCC    ","       BCCCCCCB     BCCCCCCB       ","       BCCCCCCCBBBBBCCCCCCCB       ","        BCCCCCCCCCCCCCCCCCB        ","         BCCCCCCCCCCCCCCCB         ","          BCCCCCCCCCCCCCB          ","           BCCCCCCCCCCCB           ","            BBCCCCCCCBB            ","              CCBBBCC              ","              CCDDDCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CCBBBCC              ","            BBCCCCCCCBB            ","           BCCCCCCCCCCCB           ","          BCCCCCCCCCCCCCB          ","         BCCCCCCCCCCCCCCCB         ","        BCCCCCCCCCCCCCCCCCB        ","       BCCCCCCCBBBBBCCCCCCCB       ","       BCCCCCCBCCCCCBCCCCCCB       ","     CCCCCCCCBCCCCCCCBCCCCCCCC     ","     CCCCCCCBCCCCCCCCCBCCCCCCC     ","      BCCCCCBCCCCCCCCCBCCCCCB      ","      BCCCCCBCCCCCCCCCBCCCCCB      ","      BCCCCCBCCCCCCCCCBCCCCCB      ","     CCCCCCCBCCCCCCCCCBCCCCCCC     ","     CCCCCCCCBCCCCCCCBCCCCCCCC     ","       BCCCCCCBCCCCCBCCCCCCB       ","       BCCCCCCCBBBBBCCCCCCCB       ","        BCCCCCCCCCCCCCCCCCB        ","         BCCCCCCCCCCCCCCCB         ","          BCCCCCCCCCCCCCB          ","           BCCCCCCCCCCCB           ","            BBCCCCCCCBB            ","              CCBBBCC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CCBBBCC              ","            BBCCCCCCCBB            ","           BCCCCCCCCCCCB           ","          BCCCCCCCCCCCCCB          ","         BCCCCCCCCCCCCCCCB         ","        BCCCCCCCCCCCCCCCCCB        ","       BCCCCCCCBBBBBCCCCCCCB       ","       BCCCCCCB     BCCCCCCB       ","      CCCCCCCB       BCCCCCCC      ","      CCCCCCB   BBB   BCCCCCC      ","      BCCCCCB  B   B  BCCCCCB      ","      BCCCCCB  B   B  BCCCCCB      ","      BCCCCCB  B   B  BCCCCCB      ","      CCCCCCB   BBB   BCCCCCC      ","      CCCCCCCB       BCCCCCCC      ","       BCCCCCCB     BCCCCCCB       ","       BCCCCCCCBBBBBCCCCCCCB       ","        BCCCCCCCCCCCCCCCCCB        ","         BCCCCCCCCCCCCCCCB         ","          BCCCCCCCCCCCCCB          ","           BCCCCCCCCCCCB           ","            BBCCCCCCCBB            ","              CCBBBCC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","             CCC   CCC             ","             CCC   CCC             ","            CCCC   CCCC            ","           CCCCC   CCCCC           ","         CCCCCC     CCCCCC         ","       CCCCCCC       CCCCCCC       ","       CCCCCC         CCCCCC       ","                                   ","                                   ","                                   ","       CCCCCC         CCCCCC       ","       CCCCCCC       CCCCCCC       ","         CCCCCC     CCCCCC         ","           CCCCC   CCCCC           ","            CCCC   CCCC            ","             CCC   CCC             ","             CCC   CCC             ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "},
                {"                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","              CC   CC              ","              CC   CC              ","              CC   CC              ","             CCC   CCC             ","            CCC     CCC            ","         CCCCC       CCCCC         ","         CCCC         CCCC         ","                                   ","                                   ","                                   ","         CCCC         CCCC         ","         CCCCC       CCCCC         ","            CCC     CCC            ","             CCC   CCC             ","              CC   CC              ","              CC   CC              ","              CC   CC              ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   ","                                   "}
            }))
            //spotless:on
        .addElement('A', ofBlock(GregTech_API.sBlockGlass1, 4))
        .addElement(
            'B',
            buildHatchAdder(GT_MetaTileEntity_BlackHoleCompressor.class).atLeast(Maintenance, Energy)
                .casingIndex(((GT_Block_Casings10) GregTech_API.sBlockCasings10).getTextureIndex(12))
                .dot(2)
                .buildAndChain(
                    onElementPass(
                        GT_MetaTileEntity_BlackHoleCompressor::onCasingAdded,
                        ofBlock(GregTech_API.sBlockCasings10, 12))))
        .addElement('C', ofBlock(GregTech_API.sBlockCasings10, 11))
        .addElement('D', ofFrame(Materials.NaquadahAlloy))
        .addElement(
            'E',
            buildHatchAdder(GT_MetaTileEntity_BlackHoleCompressor.class).atLeast(InputBus, OutputBus, InputHatch)
                .casingIndex(((GT_Block_Casings10) GregTech_API.sBlockCasings10).getTextureIndex(11))
                .dot(1)
                .buildAndChain(
                    onElementPass(
                        GT_MetaTileEntity_BlackHoleCompressor::onCasingAdded,
                        ofBlock(GregTech_API.sBlockCasings10, 11))))

        .build();

    private boolean blackholeOn = false;
    private int catalyzingCounter = 0;
    private float blackHoleStability = 100;

    private final FluidStack blackholeCatalyzingCost = (MaterialsUEVplus.SpaceTime).getMolten(1);
    private int catalyzingCostModifier = 1;

    public GT_MetaTileEntity_BlackHoleCompressor(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_BlackHoleCompressor(String aName) {
        super(aName);
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_BlackHoleCompressor> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_BlackHoleCompressor(this.mName);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public void setMachineModeIcons() {
        machineModeIcons.add(GT_UITextures.OVERLAY_BUTTON_MACHINEMODE_COMPRESSING);
        machineModeIcons.add(GT_UITextures.OVERLAY_BUTTON_MACHINEMODE_SINGULARITY);
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        setMachineMode(nextMachineMode());
        PlayerUtils.messagePlayer(
            aPlayer,
            String.format(StatCollector.translateToLocal("GT5U.MULTI_MACHINE_CHANGE"), getMachineModeName()));
    }

    @Override
    public String getMachineModeName() {
        return StatCollector.translateToLocal("GT5U.BLACKHOLE.mode." + machineMode);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive) {
                rTexture = new ITexture[] {
                    Textures.BlockIcons
                        .getCasingTextureForId(GT_Utility.getCasingTextureIndex(GregTech_API.sBlockCasings10, 11)),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_MULTI_COMPRESSOR_ACTIVE)
                        .extFacing()
                        .build(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_MULTI_COMPRESSOR_ACTIVE_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            } else {
                rTexture = new ITexture[] {
                    Textures.BlockIcons
                        .getCasingTextureForId(GT_Utility.getCasingTextureIndex(GregTech_API.sBlockCasings10, 11)),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_MULTI_COMPRESSOR)
                        .extFacing()
                        .build(),
                    TextureFactory.builder()
                        .addIcon(OVERLAY_FRONT_MULTI_COMPRESSOR_GLOW)
                        .extFacing()
                        .glow()
                        .build() };
            }
        } else {
            rTexture = new ITexture[] { Textures.BlockIcons
                .getCasingTextureForId(GT_Utility.getCasingTextureIndex(GregTech_API.sBlockCasings10, 11)) };
        }
        return rTexture;
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType("Compressor/Advanced Neutronium Compressor")
            .addInfo("Controller Block for the Semi-Stable Black Hole Containment Field")
            .addInfo(EnumChatFormatting.LIGHT_PURPLE + "Uses the immense power of the event horizon to compress things")
            .addInfo("No longer requires heat management to perform perfect compression")
            .addInfo("Can create advanced singularities!")
            .addSeparator()
            .addInfo(
                "Insert a " + EnumChatFormatting.WHITE
                    + "Black Hole Activation Catalyst"
                    + EnumChatFormatting.GRAY
                    + " to open a black hole")
            .addInfo(
                "The black hole will begin its life at " + EnumChatFormatting.RED
                    + "100%"
                    + EnumChatFormatting.GRAY
                    + " stability and slowly decay")
            .addSeparator()
            .addInfo("Natural decay takes " + EnumChatFormatting.RED + "100" + EnumChatFormatting.GRAY + " seconds")
            .addInfo("Running recipes in the machine will slow the decay by " + EnumChatFormatting.RED + "25%")
            .addInfo(
                "The decay can be " + EnumChatFormatting.BOLD
                    + "halted"
                    + EnumChatFormatting.RESET
                    + EnumChatFormatting.GRAY
                    + " by inserting spacetime")
            .addInfo(
                "Every " + EnumChatFormatting.RED
                    + "30"
                    + EnumChatFormatting.GRAY
                    + " seconds saved by spacetime insertion will "
                    + EnumChatFormatting.RED
                    + "double"
                    + EnumChatFormatting.GRAY
                    + " the cost per second!")
            .addInfo("Once the black hole becomes unstable, it will void all inputs for recipes which require it")
            .addInfo(
                "Insert a " + EnumChatFormatting.WHITE
                    + "Black Hole Deactivation Catalyst"
                    + EnumChatFormatting.GRAY
                    + " to close the black hole")
            .addSeparator()
            .addInfo(
                "Recipes not utilizing the black hole have their lengths " + EnumChatFormatting.RED
                    + "doubled"
                    + EnumChatFormatting.GRAY
                    + " if it becomes unstable")
            .addInfo("400% faster than singleblock machines of the same voltage")
            .addInfo("Only uses 70% of the EU/t normally required")
            .addInfo("Gains 8 parallels per voltage tier")
            .addInfo(
                "Parallels are " + EnumChatFormatting.RED
                    + "doubled"
                    + EnumChatFormatting.GRAY
                    + " when stability is BELOW "
                    + EnumChatFormatting.RED
                    + "50%")
            .addInfo(
                "Parallels are " + EnumChatFormatting.RED
                    + "quadrupled"
                    + EnumChatFormatting.GRAY
                    + " when stability is BELOW "
                    + EnumChatFormatting.RED
                    + "20%")
            .addInfo(AuthorFourIsTheNumber + EnumChatFormatting.RESET + " & " + Ollie)
            .addSeparator()
            .beginStructureBlock(35, 33, 35, false)
            .addCasingInfoMin("Background Radiation Absorbent Casing", 985, false)
            .addCasingInfoExactly("Extreme Density Space-Bending Casing", 3667, false)
            .addCasingInfoExactly("Hawking Radiation Realignment Focus", 64, false)
            .addCasingInfoExactly("Naquadah Alloy Frame Box", 144, false)
            .addInputBus("Behind Laser", 1)
            .addOutputBus("Behind Laser", 1)
            .addInputHatch("Behind Laser", 1)
            .addEnergyHatch("Any Radiation Absorbent Casing", 2)
            .addMaintenanceHatch("Any Radiation Absorbent Casing", 2)
            .toolTipFinisher("GregTech");
        return tt;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 17, 27, 10);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivialBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, 17, 27, 10, elementBudget, env, false, true);
    }

    private int mCasingAmount;

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasingAmount = 0;
        mEnergyHatches.clear();

        if (!checkPiece(STRUCTURE_PIECE_MAIN, 17, 27, 10)) return false;
        if (mCasingAmount < 0) return false;

        return true;
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        aNBT.setInteger("catalyzingCostModifier", catalyzingCostModifier);
        aNBT.setInteger("catalyzingCounter", catalyzingCounter);
        aNBT.setBoolean("blackholeOn", blackholeOn);
        aNBT.setFloat("blackholeStability", blackHoleStability);
        aNBT.setBoolean("doingBlackHole", doingBlackHole);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        if (aNBT.hasKey("catalyzingCounter")) catalyzingCostModifier = aNBT.getInteger("catalyzingCounter");
        if (aNBT.hasKey("catalyzingCostModifier")) catalyzingCostModifier = aNBT.getInteger("catalyzingCostModifier");
        if (aNBT.hasKey("blackholeOn")) blackholeOn = aNBT.getBoolean("blackholeOn");
        if (aNBT.hasKey("blackholeStability")) blackHoleStability = aNBT.getFloat("blackholeStability");
        if (aNBT.hasKey("doingBlackHole")) doingBlackHole = aNBT.getBoolean("doingBlackHole");
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        logic.setAvailableVoltage(GT_Utility.roundUpVoltage(this.getMaxInputVoltage()));
        logic.setAvailableAmperage(1L);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setBoolean("blackholeOn", blackholeOn);
        tag.setFloat("blackHoleStability", blackHoleStability);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        final NBTTagCompound tag = accessor.getNBTData();
        if (tag.getBoolean("blackholeOn")) {
            if (tag.getFloat("blackHoleStability") > 0) {
                currentTip.add(EnumChatFormatting.DARK_PURPLE + "Black Hole Active");
                currentTip.add(
                    EnumChatFormatting.DARK_PURPLE + " Stability: "
                        + EnumChatFormatting.BOLD
                        + Math.round(tag.getFloat("blackHoleStability"))
                        + "%");
            } else {
                currentTip.add(EnumChatFormatting.RED + "BLACK HOLE UNSTABLE");
            }
        } else currentTip.add(EnumChatFormatting.DARK_PURPLE + "Black Hole Offline");
    }

    private boolean doingBlackHole;

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @NotNull
            @Override
            protected Stream<GT_Recipe> findRecipeMatches(@Nullable RecipeMap<?> map) {

                // Loop through all items and look for the Activation and Deactivation Catalysts
                // Deactivation resets stability to 100 and catalyzing cost to 1
                for (ItemStack inputItem : inputItems) {
                    if (inputItem.getItem() instanceof GT_MetaGenerated_Item_01) {
                        if (inputItem.getItemDamage() == 32418 && !blackholeOn) {
                            inputItem.stackSize -= 1;
                            blackholeOn = true;
                            break;
                        } else if (inputItem.getItemDamage() == 32419 && blackholeOn) {
                            inputItem.stackSize -= 1;
                            blackholeOn = false;
                            blackHoleStability = 100;
                            catalyzingCostModifier = 1;
                            break;
                        }
                    }
                }
                return super.findRecipeMatches(map);
            }

            @NotNull
            @Override
            protected CheckRecipeResult validateRecipe(@NotNull GT_Recipe recipe) {

                // Default speed bonus
                setSpeedBonus(1F);
                doingBlackHole = false;

                // If recipe needs a black hole and one is not open, just wait
                // If the recipe doesn't require black hole, incur a 0.5x speed penalty
                // If recipe doesn't require black hole but one is open, give 5x speed bonus
                if (recipe.mSpecialValue > 0) {
                    doingBlackHole = true;
                    if (!blackholeOn) return CheckRecipeResultRegistry.NO_BLACK_HOLE;
                } else {
                    if (blackHoleStability <= 0) setSpeedBonus(2F);
                    else if (blackholeOn) setSpeedBonus(0.2F);
                }
                return super.validateRecipe(recipe);
            }

            @Nonnull
            protected CheckRecipeResult onRecipeStart(@Nonnull GT_Recipe recipe) {
                // If recipe needs a black hole and one is active but unstable, continuously void items
                if (blackHoleStability <= 0 && recipe.mSpecialValue > 0) {
                    return CheckRecipeResultRegistry.UNSTABLE_BLACK_HOLE;
                }
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        }.setMaxParallelSupplier(this::getMaxParallelRecipes)
            .setEuModifier(0.7F);
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (doingBlackHole && !blackholeOn) stopMachine(SimpleShutDownReason.ofCritical("no_black_hole"));
        return super.onRunningTick(aStack);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);

        if (aTick % 20 == 0) {
            if (blackholeOn && blackHoleStability >= 0) {
                float stabilityDecrease = 1F;
                // If the machine is running, reduce stability loss by 25%
                if (this.maxProgresstime() != 0) {
                    stabilityDecrease = 0.75F;
                }
                // Search all hatches for catalyst fluid
                // If found enough, drain it and reduce stability loss to 0
                // Every 30 drains, double the cost
                FluidStack totalCost = new FluidStack(blackholeCatalyzingCost, catalyzingCostModifier);
                for (GT_MetaTileEntity_Hatch_Input hatch : mInputHatches) {
                    if (drain(hatch, totalCost, false)) {
                        drain(hatch, totalCost, true);
                        catalyzingCounter += 1;
                        stabilityDecrease = 0;
                        if (catalyzingCounter >= 30) {
                            catalyzingCostModifier *= 2;
                            catalyzingCounter = 0;
                        }
                    }
                }
                if (blackHoleStability >= 0) blackHoleStability -= stabilityDecrease;
                else blackHoleStability = 0;
            }
        }
    }

    public int getMaxParallelRecipes() {
        int parallels = (8 * GT_Utility.getTier(this.getMaxInputVoltage()));
        if (blackHoleStability < 60) {
            parallels *= 2;
            if (blackHoleStability < 20) parallels *= 2;
        }
        return parallels;
    }

    private static final int MACHINEMODE_COMPRESSOR = 0;
    private static final int MACHINEMODE_BLACKHOLE = 1;

    @Override
    public RecipeMap<?> getRecipeMap() {
        return (machineMode == MACHINEMODE_COMPRESSOR) ? RecipeMaps.compressorRecipes
            : RecipeMaps.neutroniumCompressorRecipes;
    }

    @Nonnull
    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.asList(RecipeMaps.compressorRecipes, RecipeMaps.neutroniumCompressorRecipes);
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
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }
}