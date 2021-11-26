package net.blay09.mods.cookingforblockheads.block;

import net.blay09.mods.cookingforblockheads.ItemUtils;
import net.blay09.mods.cookingforblockheads.tile.IMutableNameable;
import net.blay09.mods.cookingforblockheads.util.TextUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.INameable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class BlockKitchen extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LOWERED = BooleanProperty.create("lowered");
    public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");
    public static final BooleanProperty HAS_COLOR = BooleanProperty.create("has_color");
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    private static final VoxelShape BOUNDING_BOX_X = Block.makeCuboidShape(0.5, 0, 0, 15.5, 15.0, 16);
    private static final VoxelShape BOUNDING_BOX_Z = Block.makeCuboidShape(0, 0, 0.5, 16, 15.0, 15.5);

    private final ResourceLocation registryName;

    protected BlockKitchen(Block.Properties properties, ResourceLocation registryName) {
        super(properties);
        this.registryName = registryName;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        if (state.get(FACING).getAxis() == Direction.Axis.X) {
            return BOUNDING_BOX_X;
        } else {
            return BOUNDING_BOX_Z;
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    @Nonnull
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        BlockState state = getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite());
        if (state.hasProperty(LOWERED)) {
            state = state.with(LOWERED, shouldBeLoweredUpon(context.getWorld().getBlockState(context.getPos().down())));
        }
        return state.hasProperty(HAS_COLOR) ? state.with(HAS_COLOR, false) : state;
    }

    @Override
    public void addInformation(ItemStack itemStack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(TextUtils.coloredTextComponent("tooltip.cookingforblockheads:multiblock_kitchen", TextFormatting.YELLOW));

        if (hasTooltipDescription()) {
            for (String s : I18n.format("tooltip." + registryName + ".description").split("\\\\n")) {
                tooltip.add(TextUtils.coloredTextComponent(s, TextFormatting.GRAY));
            }
        }

        if (isDyeable()) {
            tooltip.add(TextUtils.coloredTextComponent("tooltip.cookingforblockheads:dyeable", TextFormatting.AQUA));
        }
    }

    protected boolean hasTooltipDescription() {
        return true;
    }

    protected boolean isDyeable() {
        return false;
    }

    public static boolean shouldBlockRenderLowered(IBlockReader world, BlockPos pos) {
        return shouldBeLoweredUpon(world.getBlockState(pos.down()));
    }

    private static boolean shouldBeLoweredUpon(BlockState stateBelow) {
        Block blockBelow = stateBelow.getBlock();
        return blockBelow instanceof KitchenCounterBlock || blockBelow instanceof KitchenCornerBlock;
    }

    public boolean shouldBePlacedFlipped(BlockItemUseContext context, Direction facing) {
        BlockPos pos = context.getPos();
        PlayerEntity placer = context.getPlayer();
        if (placer == null) {
            return Math.random() < 0.5;
        }

        boolean flipped;
        double dir = 0;
        if (facing.getAxis() == Direction.Axis.Z) {
            dir = pos.getX() + 0.5f - placer.getPosX();
            dir *= -1;
        } else if (facing.getAxis() == Direction.Axis.X) {
            dir = pos.getZ() + 0.5f - placer.getPosZ();
        }
        if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            flipped = dir < 0;
        } else {
            flipped = dir > 0;
        }
        return flipped;
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null && state.getBlock() != newState.getBlock()) {
            tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                    .ifPresent(itemHandler -> ItemUtils.dropItemHandlerItems(world, pos, itemHandler));
        }

        super.onReplaced(state, world, pos, newState, isMoving);
    }

    public boolean tryRecolorBlock(BlockState state, ItemStack heldItem, World world, BlockPos pos, PlayerEntity player, BlockRayTraceResult rayTraceResult) {
        if (heldItem.getItem() == Items.BONE_MEAL) {
            if (removeColor(state, world, pos, rayTraceResult.getFace())) {
                if (!player.abilities.isCreativeMode) {
                    heldItem.shrink(1);
                }

                return true;
            }
        }

        DyeColor color = DyeColor.getColor(heldItem);
        if (color != null) {
            if (recolorBlock(state, world, pos, rayTraceResult.getFace(), color)) {
                if (!player.abilities.isCreativeMode) {
                    heldItem.shrink(1);
                }
            }

            return true;
        }

        return false;
    }

    private boolean removeColor(BlockState state, IWorld world, BlockPos pos, Direction facing) {
        if (state.hasProperty(COLOR) && state.hasProperty(HAS_COLOR)) {
            if (state.get(HAS_COLOR)) {
                world.setBlockState(pos, state.with(HAS_COLOR, false), 3);
                return true;
            }
        }

        return false;
    }

    private boolean recolorBlock(BlockState state, IWorld world, BlockPos pos, Direction facing, DyeColor color) {
        if (state.hasProperty(COLOR) && state.hasProperty(HAS_COLOR)) {
            DyeColor current = state.get(COLOR);
            if (current != color || !state.get(HAS_COLOR)) {
                world.setBlockState(pos, state.with(COLOR, color).with(HAS_COLOR, true), 3);
                return true;
            }
        }

        return false;
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (facing == Direction.DOWN && state.hasProperty(LOWERED)) {
            return state.with(LOWERED, shouldBeLoweredUpon(facingState));
        }

        return state;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public boolean eventReceived(BlockState p_189539_1_, World world, BlockPos pos, int id, int param) {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null) {
            return tileEntity.receiveClientEvent(id, param);
        }

        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasDisplayName()) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof IMutableNameable) {
                ((IMutableNameable) tileEntity).setCustomName(itemStack.getDisplayName());
            }
        }
    }
}
