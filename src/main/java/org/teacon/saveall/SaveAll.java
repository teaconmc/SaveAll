package org.teacon.saveall;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.zip.Deflater.DEFAULT_COMPRESSION;

@Mod(SaveAll.MOD_ID)
@Mod.EventBusSubscriber
public class SaveAll {
    public static final String MOD_ID = "saveall";
    public static int tick;
    public static MinecraftServer server;

    @SubscribeEvent
    public static void onServerTickEvent(TickEvent.ServerTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START) || server == null || config == null) return;
        tick++;
        if (tick > config.saveTime.get()) {
            tick = 0;
            for (ServerWorld world : server.getWorlds()) {
                if (world != null) {
                    world.disableLevelSaving = true;
                }
            }
            server.getPlayerList().saveAllPlayerData();
            new Thread(() -> {
                try {
                    File src = server.getWorld(DimensionType.OVERWORLD).getSaveHandler().getWorldDirectory();
                    LinkedHashMap<File, String> fileMap = new LinkedHashMap<>();
                    for (File file : listFile(src)) {
                        String filePath = file.getAbsolutePath();
                        fileMap.put(file, src.getName() + File.separator + filePath.substring(src.getAbsolutePath().length() + 1));
                    }
                    StringBuilder out = new StringBuilder();
                    Calendar time = Calendar.getInstance();
                    addNum(out, time.get(Calendar.YEAR), '-');
                    addNum(out, time.get(Calendar.MONTH) + 1, '-');
                    addNum(out, time.get(Calendar.DAY_OF_MONTH), '-');
                    addNum(out, time.get(Calendar.HOUR_OF_DAY), '-');
                    addNum(out, time.get(Calendar.MINUTE), '-');
                    addNum(out, time.get(Calendar.SECOND), '\0');
                    out.append(".zip");
                    File dstFile = newFile(new File(config.folder.get(), out.toString()));
                    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
                    zos.setLevel(DEFAULT_COMPRESSION);

                    byte[] buffer = new byte[4096];

                    for (Map.Entry<File, String> entry : fileMap.entrySet()) {
                        try {
                            ZipEntry ze = new ZipEntry(entry.getValue());

                            zos.putNextEntry(ze);
                            FileInputStream fis = new FileInputStream(entry.getKey());
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                            zos.closeEntry();
                            fis.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    zos.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }).start();
        }
    }

    @SubscribeEvent
    public static void onSeverStart(FMLServerStartedEvent event) {
        server = event.getServer();
    }

    public static List<File> listFile(File file) {
        List<File> l = new ArrayList<>();
        listFile0(l, file);
        return l;
    }

    public static void listFile0(List<File> list, File file) {
        if (file.isDirectory()) {
            File[] fl = file.listFiles();

            if (fl != null && fl.length > 0) {
                for (File aFl : fl) {
                    listFile0(list, aFl);
                }
            }
        } else if (file.isFile()) {
            list.add(file);
        }
    }

    private static void addNum(StringBuilder sb, int num, char c) {
        if (num < 10) {
            sb.append('0');
        }
        sb.append(num);
        if (c != '\0') {
            sb.append(c);
        }
    }

    public static File newFile(File file) {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public static Config config;

    public SaveAll() {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        config = new Config(configBuilder);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, configBuilder.build(), MOD_ID + ".toml");
    }

}
