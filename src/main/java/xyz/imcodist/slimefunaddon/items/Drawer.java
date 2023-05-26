package xyz.imcodist.slimefunaddon.items;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Drawer extends SlimefunItem implements Listener {
    public Drawer(ItemGroup itemGroup) {
        // Set up the slimefun item.
        super(
                itemGroup,
                new SlimefunItemStack("DRAWER", Material.BARREL, "&eDrawer", " ", "&7Storage drawers lol."),
                RecipeType.ENHANCED_CRAFTING_TABLE,
                new ItemStack[]{
                        new ItemStack(Material.OAK_LOG), new ItemStack(Material.OAK_LOG), new ItemStack(Material.OAK_LOG),
                        new ItemStack(Material.OAK_LOG), new ItemStack(Material.CHEST), new ItemStack(Material.OAK_LOG),
                        new ItemStack(Material.OAK_LOG), new ItemStack(Material.OAK_LOG), new ItemStack(Material.OAK_LOG)
                }
        );

        // Set up the block storage.
        new BlockMenuPreset(getId(), "Drawer Storage") {
            @Override
            public void init() { setSize(9 * 2); }
            @Override
            public boolean canOpen(Block b, Player p) { return false; }
            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) { return new int[0]; }
        };
    }


    @Override
    public void preRegister() {
        // Add item handlers.
        BlockUseHandler blockUseHandler = this::onBlockUse;
        BlockPlaceHandler blockPlaceHandler = getBlockPlaceHandler();
        BlockBreakHandler blockBreakHandler = getBlockBreakHandler();

        addItemHandler(blockUseHandler, blockPlaceHandler, blockBreakHandler);
    }

    @Override
    public void postRegister() {
        // Listen for additional events.
        JavaPlugin plugin = getAddon().getJavaPlugin();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    private void onBlockPlaced(BlockPlaceEvent event) {
        Block block = event.getBlock();
        BlockFace face = event.getPlayer().getFacing().getOppositeFace();

        // Spawn item frame.
        World world = block.getWorld();
        Location spawnLocation = block.getLocation().add(face.getDirection());

        ItemFrame frame = world.spawn(spawnLocation, ItemFrame.class);
        frame.setItemDropChance(0f);
        frame.setFixed(true);

        // Just in case the frame spawns on another face.
        if (frame.getAttachedFace().getOppositeFace() != face) frame.remove();

        // If the block can be rotated, rotate the block towards the new item frame.
        if (block.getBlockData() instanceof Directional) {
            Directional directionalData = (Directional) block.getBlockData();
            directionalData.setFacing(face);

            // Update block data.
            block.setBlockData(directionalData);
        }
    }

    private void onBlockBroken(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
        // Kill the item frame associated with the block.
        Collection<Entity> nearbyEntities = event.getBlock().getWorld().getNearbyEntities(event.getBlock().getBoundingBox().expand(0.01));

        for (Entity entity : nearbyEntities) {
            if (entity instanceof ItemFrame) {
                // Remove every frame connected to this block.
                // Not the ideal way of doing this but, mhm.
                ItemFrame frame = (ItemFrame) entity;
                Block testBlock = getBlockFromFrame(frame);

                if (testBlock.equals(event.getBlock())) {
                    entity.remove();
                }
            }
        }

        // Drop all menu items.
        Block block = event.getBlock();

        BlockMenu blockMenu = BlockStorage.getInventory(block);
        if (blockMenu == null) return;

        loadBlockStorage(block);

        ItemStack mainStack = blockMenu.getItemInSlot(0);
        if (mainStack != null) {
            while (mainStack.getAmount() > 0) {
                int remove = mainStack.getMaxStackSize();
                if (remove > mainStack.getAmount()) remove = mainStack.getAmount();

                ItemStack dropStack = mainStack.clone();
                dropStack.setAmount(remove);

                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), dropStack);

                mainStack.setAmount(mainStack.getAmount() - remove);
            }
        }
    }

    private void onBlockUse(PlayerRightClickEvent event) {
        // If the player is trying to build on the block then ignore.
        if (event.getInteractEvent().isBlockInHand() && event.getPlayer().isSneaking()) return;

        // Prevent the player from opening the barrels inventory.
        event.cancel();

        Block block = event.getClickedBlock().orElse(null);
        if (block == null) return;

        BlockMenu ogMenu = BlockStorage.getInventory(block);
        if (ogMenu == null) return;

        loadBlockStorage(block);

        BlockMenu menu = new BlockMenu(new BlockMenuPreset(getId() + "_MENU", "Drawer") {
            @Override
            public void init() {
                ArrayList<Integer> ignoredSlots = new ArrayList<>();
                ignoredSlots.add(13);

                for (int i = 0; i < (9 * 3); i++) {
                    if (ignoredSlots.contains(i)) continue;

                    int[] slot = {i};
                    drawBackground(slot);
                }

                int[] previewSlot = {13};
                ItemStack mainStack = ogMenu.getItemInSlot(0);
                ItemStack previewStack;

                if (mainStack != null) {
                    previewStack = mainStack.clone();
                    previewStack.setAmount(1);

                    ItemMeta previewMeta = previewStack.getItemMeta();
                    if (previewMeta != null) {
                        previewMeta.setDisplayName(ChatColor.RESET + getItemName(mainStack) + ChatColor.RESET + ChatColor.GRAY + " x" + mainStack.getAmount());
                        previewStack.setItemMeta(previewMeta);
                    }
                } else {
                    previewStack = new ItemStack(Material.BARRIER);
                    ItemMeta previewMeta = previewStack.getItemMeta();

                    if (previewMeta != null) {
                        previewMeta.setDisplayName(ChatColor.RESET.toString() + ChatColor.RED + "NONE");
                        previewStack.setItemMeta(previewMeta);
                    }
                }

                drawBackground(previewStack, previewSlot);
            }
            @Override
            public boolean canOpen(Block b, Player p) { return false; }
            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) { return new int[0]; }
        }, block.getLocation());

        menu.open(event.getPlayer());
    }


    @EventHandler
    private void onEntityInteract(PlayerInteractEntityEvent event) {
        // Make sure the entity is an item frame.
        if (!(event.getRightClicked() instanceof ItemFrame)) return;

        // Get the block from the frame.
        ItemFrame frame = (ItemFrame) event.getRightClicked();
        Block block = getBlockFromFrame(frame);

        // Make sure the block is a drawer.
        SlimefunItem slimefunItem = getSlimefunItem(block);
        if (slimefunItem == null || !slimefunItem.equals(this)) return;

        // Cancel the event.
        event.setCancelled(true);

        // Get inventory's.
        PlayerInventory playerInventory = event.getPlayer().getInventory();

        BlockMenu menu = BlockStorage.getInventory(block);
        if (menu == null) return;

        loadBlockStorage(block);

        // Check if the item should only add once.
        boolean stopAtFirst = !event.getPlayer().isSneaking();

        // Add into the container.
        for (int slot = -1; slot < playerInventory.getSize(); slot++) {
            // Get the stack in the first slot.
            ItemStack mainStack = menu.getItemInSlot(0);

            // Check for the players main slot, then the rest of their inventory.
            ItemStack slotItem;
            boolean insertEmpty = false;

            if (slot == -1) {
                slotItem = playerInventory.getItem(event.getHand());
                insertEmpty = mainStack == null;
            }
            else {
                slotItem = playerInventory.getItem(slot);
            }

            // If the slot has nothing, move on to the next slot.
            if (slotItem == null) continue;

            // If the item being inserted is similar, or the drawer is empty.
            if (slotItem.isSimilar(mainStack) || insertEmpty) {
                int addAmount = slotItem.getAmount();
                int newAmount = (mainStack != null ? mainStack.getAmount() : 0) + addAmount;

                // Make sure the player doesn't add more than the drawer can fit.
                int maxAmount = 64 * 32;
                if (newAmount > maxAmount) {
                    addAmount = slotItem.getAmount() - (newAmount - maxAmount);
                    newAmount = maxAmount;
                }

                // Add the item to the drawer.
                if (!insertEmpty) {
                    // This is redundant but my IDE is telling me it's not and I want it to shut up.
                    if (mainStack != null) mainStack.setAmount(newAmount);
                }
                else {
                    mainStack = slotItem.clone();
                    mainStack.setAmount(newAmount);

                    menu.replaceExistingItem(0, mainStack);
                }

                // Remove the players item.
                slotItem.setAmount(slotItem.getAmount() - addAmount);

                // Don't continue if only one stack should be added.
                //if (stopAtFirst && amountAdded >= slotItem.getMaxStackSize()) break;
                if (stopAtFirst) break;
            }
        }

        // Update the item frame and block storage to reflect new changes.
        updateItemFrame(frame, menu);
        updateBlockStorage(block);
    }

    @EventHandler
    private void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        // Make sure the entity is an Item Frame.
        if (!(event.getEntity() instanceof ItemFrame)) return;

        // Make sure the destroyer is a player.
        if (!(event.getRemover() instanceof Player)) return;

        // Get the block from the frame.
        ItemFrame frame = (ItemFrame) event.getEntity();
        Block block = getBlockFromFrame(frame);

        // Make sure the block is a drawer.
        SlimefunItem slimefunItem = getSlimefunItem(block);
        if (slimefunItem == null || !slimefunItem.equals(this)) return;

        // Cancel the event.
        event.setCancelled(true);

        // Get inventory's.
        Player player = (Player) event.getRemover();
        PlayerInventory playerInventory = player.getInventory();

        BlockMenu menu = BlockStorage.getInventory(block);
        if (menu == null) return;

        loadBlockStorage(block);

        // Remove from main slot.
        ItemStack mainStack = menu.getItemInSlot(0);

        if (mainStack != null) {
            int amountToRemove = 1;

            if (player.isSneaking()) amountToRemove = mainStack.getMaxStackSize();
            if (mainStack.getAmount() < amountToRemove) amountToRemove = mainStack.getAmount();

            ItemStack addStack = mainStack.clone();
            addStack.setAmount(amountToRemove);

            mainStack.setAmount(mainStack.getAmount() - amountToRemove);

            playerInventory.addItem(addStack);
        }

        // Update the item frame and block storage to reflect new changes.
        updateItemFrame(frame, menu);
        updateBlockStorage(block);
    }


    private Block getBlockFromFrame(ItemFrame frame) {
        // Get the attached frame and look for a block behind it.
        BlockFace attachedFace = frame.getAttachedFace();
        return frame.getLocation().add(attachedFace.getDirection()).getBlock();
    }

    private SlimefunItem getSlimefunItem(Block block) {
        return BlockStorage.check(block);
    }

    private void updateItemFrame(ItemFrame frame, BlockMenu menu) {
        ItemStack itemStack = menu.getItemInSlot(0);

        if (itemStack != null) {
            itemStack = itemStack.clone();
            ItemMeta previewMeta = itemStack.getItemMeta();

            if (previewMeta != null) {
                previewMeta.setDisplayName("x" + itemStack.getAmount());
                itemStack.setItemMeta(previewMeta);
            }
        }

        frame.setItem(itemStack);
    }

    private void updateBlockStorage(Block block) {
        BlockMenu menu = BlockStorage.getInventory(block);

        ItemStack mainStack = menu.getItemInSlot(0);
        if (mainStack != null) BlockStorage.addBlockInfo(block, "amount", String.valueOf(mainStack.getAmount()));
        else BlockStorage.addBlockInfo(block, "amount", String.valueOf(0));
    }

    private void loadBlockStorage(Block block) {
        BlockMenu menu = BlockStorage.getInventory(block);

        ItemStack mainStack = menu.getItemInSlot(0);
        if (mainStack != null) {
            int amount = Integer.parseInt(BlockStorage.getLocationInfo(block.getLocation(), "amount"));
            mainStack.setAmount(amount);
        }
    }

    private String getItemName(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) return itemMeta.getDisplayName();

        String name = itemStack.getType().name();
        name = WordUtils.capitalizeFully(name.replace("_", " "));
        return name;
    }


    private BlockPlaceHandler getBlockPlaceHandler() {
        return new BlockPlaceHandler(true) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) { onBlockPlaced(e); }
        };
    }

    private BlockBreakHandler getBlockBreakHandler() {
        return new BlockBreakHandler(true, true) {
            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) { onBlockBroken(e, item, drops);}
        };
    }
}
