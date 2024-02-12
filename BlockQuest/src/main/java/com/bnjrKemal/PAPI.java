package com.bnjrKemal;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PAPI extends PlaceholderExpansion {

    Main main;

    public PAPI(Main instance) {
        this.main = instance;
    }

    @Override
    public String getIdentifier() {
        return "blockquest";
    }

    @Override
    public String getAuthor() {
        return "bnjrKemal";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

        for (SpecificChest chest : Main.chests) {
            if (params.equals(chest.getFileName())) {
                return chest.remainingTime();
            }
        }

        return "";
    }
}
