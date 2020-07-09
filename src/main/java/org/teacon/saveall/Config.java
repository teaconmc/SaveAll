package org.teacon.saveall;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public class Config {
    public final ForgeConfigSpec.IntValue saveTime;
    public final ForgeConfigSpec.ConfigValue<String> folder;


    public Config(ForgeConfigSpec.Builder builder) {
        saveTime = builder
                .defineInRange("save_time", 60*20*60, 0, 60*20*60*24);
        folder = builder
                .define("save_folder", "backups");
    }


}
