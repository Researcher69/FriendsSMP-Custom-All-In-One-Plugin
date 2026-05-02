package com.friendssmp.gui;

import com.friendssmp.manager.ClaimManager;
import com.friendssmp.manager.PlayerRegistryManager;
import com.friendssmp.model.Claim;
import com.friendssmp.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public final class TrustGUIManager {
    private static final int[] HEAD_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 23, 24, 25, 26,
            27, 28, 29, 30, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int[] CLAIM_SLOTS = {22, 31};

    private final ClaimManager claimManager;
    private final PlayerRegistryManager playerRegistryManager;

    public TrustGUIManager(ClaimManager claimManager, PlayerRegistryManager playerRegistryManager) {
        this.claimManager = claimManager;
        this.playerRegistryManager = playerRegistryManager;
    }

    public void open(Player player, Claim claim, int page) {
        new TrustGUI(player, claim.id(), page).open(player);
    }

    private final class TrustGUI extends PaginatedGUI<PlayerRegistryManager.KnownPlayer> {
        private final Player owner;
        private int selectedClaimId;

        private TrustGUI(Player owner, int selectedClaimId, int page) {
            super(54, "Trust Manager", playerRegistryManager.allPlayers().stream()
                    .filter(knownPlayer -> !knownPlayer.uuid().equals(owner.getUniqueId()))
                    .toList(), HEAD_SLOTS, page);
            this.owner = owner;
            this.selectedClaimId = selectedClaimId;
        }

        @Override
        protected void renderFrame() {
            fill(item(Material.BLACK_STAINED_GLASS_PANE, " "));
            List<Claim> claims = claimManager.claimsOf(owner.getUniqueId());
            if (claims.stream().noneMatch(claim -> claim.id() == selectedClaimId) && !claims.isEmpty()) {
                selectedClaimId = claims.get(0).id();
            }
            for (int i = 0; i < Math.min(claims.size(), CLAIM_SLOTS.length); i++) {
                Claim claim = claims.get(i);
                setItem(CLAIM_SLOTS[i], claimItem(claim, claim.id() == selectedClaimId), event -> {
                    selectedClaimId = claim.id();
                    ((Player) event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
                    redraw();
                });
            }
            setItem(49, item(Material.BARRIER, "§cClose"), event -> event.getWhoClicked().closeInventory());
        }

        @Override
        protected void renderEntry(int slot, PlayerRegistryManager.KnownPlayer knownPlayer) {
            Claim selectedClaim = claimManager.byId(selectedClaimId).orElse(null);
            boolean trusted = selectedClaim != null && selectedClaim.trusted().contains(knownPlayer.uuid());
            setItem(slot, playerHead(knownPlayer.uuid(), knownPlayer.name(), trusted), event -> {
                if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                Claim claim = claimManager.byId(selectedClaimId).orElse(null);
                if (claim == null || !claim.ownerUuid().equals(player.getUniqueId())) {
                    player.closeInventory();
                    Message.send(player, "&cThat claim is no longer available.");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(knownPlayer.uuid());
                if (event.getClick() == ClickType.RIGHT) {
                    claimManager.untrust(claim, player, target);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.7f, 1.2f);
                    Message.send(player, "&aUntrusted &f" + knownPlayer.name() + "&a.");
                } else {
                    claimManager.trust(claim, player, target);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
                    Message.send(player, "&aTrusted &f" + knownPlayer.name() + "&a.");
                }
                redraw();
            });
        }

        private ItemStack claimItem(Claim claim, boolean selected) {
            return item(selected ? Material.LIME_BANNER : Material.FILLED_MAP, (selected ? "§eSelected: " : "§a") + claim.plotName(), List.of(
                    "§7World: §f" + claim.world(),
                    "§7Coords: §f" + claim.coordinateSummary(),
                    "§7Size: §f" + claim.widthX() + " x " + claim.widthZ(),
                    selected ? "§aPlayer clicks affect this claim." : "§eClick to select"));
        }

        private ItemStack playerHead(UUID uuid, String name, boolean trusted) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.setDisplayName((trusted ? "§a" : "§c") + name);
            meta.setLore(List.of(
                    trusted ? "§7Status: §aTrusted" : "§7Status: §cNot trusted",
                    "§eLeft click: trust",
                    "§eRight click: untrust"));
            item.setItemMeta(meta);
            return item;
        }
    }
}
