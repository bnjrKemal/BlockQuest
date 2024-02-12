package com.bnjrKemal;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.List;

public class Hologram {

    private ArmorStand[] armorStands;

    public Hologram(Location location, List<String> lines, int height) {
        spawnHologram(location, lines, height);
    }

    public void spawnHologram(Location location, List<String> lines, int height) {
        armorStands = new ArmorStand[lines.size()];

        for (int i = 0; i < lines.size(); i++) {
            armorStands[i] = (ArmorStand) location.getWorld().spawnEntity(location.add(0, (i * 0.25) + height, 0), EntityType.ARMOR_STAND);
            armorStands[i].setCustomNameVisible(true);
            armorStands[i].setGravity(false);
            armorStands[i].setCanPickupItems(false);
            armorStands[i].setCustomName(lines.get(i));
        }
    }

    public void despawnHologram() {
        for (ArmorStand armorStand : armorStands) {
            if (armorStand != null && !armorStand.isDead()) {
                armorStand.remove();
            }
        }
    }

}
