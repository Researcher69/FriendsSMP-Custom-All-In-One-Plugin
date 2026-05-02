package com.friendssmp.gui;

import com.friendssmp.manager.ClaimManager;
import com.friendssmp.model.Claim;
import com.friendssmp.util.Message;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public final class ClaimGUIManager {
    private final ClaimManager claimManager;
    private final TrustGUIManager trustGUIManager;

    public ClaimGUIManager(ClaimManager claimManager, TrustGUIManager trustGUIManager) {
        this.claimManager = claimManager;
        this.trustGUIManager = trustGUIManager;
    }

    public void openClaims(Player player) {
        new ClaimsGUI(player).open(player);
    }

    private final class ClaimsGUI extends BaseGUI {
        private final Player owner;

        private ClaimsGUI(Player owner) {
            super(27, "Your Claims");
            this.owner = owner;
        }

        @Override
        protected void render() {
            fill(item(Material.GRAY_STAINED_GLASS_PANE, " "));
            List<Claim> claims = claimManager.claimsOf(owner.getUniqueId());
            if (claims.isEmpty()) {
                setItem(13, item(Material.BARRIER, "§cNo claims", List.of("§7Use /claim to get a Claim Wand.")));
                return;
            }
            int[] slots = {12, 14};
            for (int i = 0; i < Math.min(claims.size(), slots.length); i++) {
                Claim claim = claims.get(i);
                setItem(slots[i], claimItem(claim, false), event -> {
                    Player player = (Player) event.getWhoClicked();
                    claimManager.byId(claim.id()).ifPresentOrElse(selected -> trustGUIManager.open(player, selected, 0),
                            () -> Message.send(player, "&cThat claim no longer exists."));
                });
            }
        }

        private org.bukkit.inventory.ItemStack claimItem(Claim claim, boolean selected) {
            return item(selected ? Material.MAP : Material.FILLED_MAP, (selected ? "§e" : "§a") + claim.plotName(), List.of(
                    "§7World: §f" + claim.world(),
                    "§7Coords: §f" + claim.coordinateSummary(),
                    "§7Size: §f" + claim.widthX() + " x " + claim.widthZ(),
                    "§eClick to manage trust"));
        }
    }
}
