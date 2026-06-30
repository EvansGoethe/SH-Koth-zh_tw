package dev.smartshub.shkoth.service.reward;

import com.saicone.rtag.item.ItemTagStream;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 為離線贏家暫存獎勵，玩家上線時補發。
 * 寫檔走 async；讀取在 onEnable 時 sync 載入一次以確保第一個 join 事件能取到。
 */
public class OfflineRewardStorage {

    private static volatile OfflineRewardStorage instance;

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, List<ItemStack>> pending = new ConcurrentHashMap<>();

    public OfflineRewardStorage(Plugin plugin) {
        this.plugin = plugin;
        File folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();
        this.file = new File(folder, "offline-rewards.yml");
        loadSync();
        instance = this;
    }

    public static OfflineRewardStorage get() {
        return instance;
    }

    public void add(UUID winner, ItemStack item) {
        if (item == null) return;
        pending.computeIfAbsent(winner, u -> Collections.synchronizedList(new ArrayList<>())).add(item.clone());
        scheduleSave();
    }

    /**
     * 玩家上線時呼叫。回傳要發給玩家的 ItemStack 清單，並從暫存清除。
     */
    public List<ItemStack> drain(Player player) {
        List<ItemStack> items = pending.remove(player.getUniqueId());
        if (items == null || items.isEmpty()) return Collections.emptyList();
        scheduleSave();
        return new ArrayList<>(items);
    }

    public boolean hasPending(UUID id) {
        List<ItemStack> list = pending.get(id);
        return list != null && !list.isEmpty();
    }

    private void loadSync() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                List<String> base64List = root.getStringList(key);
                List<ItemStack> items = new ArrayList<>(base64List.size());
                for (String base64 : base64List) {
                    ItemStack[] arr = ItemTagStream.INSTANCE.fromBase64(base64);
                    if (arr != null) {
                        for (ItemStack it : arr) {
                            if (it != null) items.add(it);
                        }
                    }
                }
                if (!items.isEmpty()) {
                    pending.put(id, Collections.synchronizedList(items));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "讀取離線獎勵失敗 (key=" + key + ")", e);
            }
        }
    }

    private void scheduleSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveNow();
            }
        }.runTaskAsynchronously(plugin);
    }

    private synchronized void saveNow() {
        YamlConfiguration yaml = new YamlConfiguration();
        Map<String, List<String>> serialized = new HashMap<>();
        for (Map.Entry<UUID, List<ItemStack>> e : pending.entrySet()) {
            List<ItemStack> items;
            synchronized (e.getValue()) {
                items = new ArrayList<>(e.getValue());
            }
            List<String> base64 = new ArrayList<>(items.size());
            for (ItemStack item : items) {
                base64.add(ItemTagStream.INSTANCE.toBase64(new ItemStack[]{item}));
            }
            serialized.put(e.getKey().toString(), base64);
        }
        yaml.set("players", serialized);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "儲存離線獎勵失敗", ex);
        }
    }

    public void flush() {
        // 主執行緒同步存（用於 onDisable）
        saveNow();
    }

    /**
     * 玩家上線時呼叫：發放暫存獎勵，背包滿了就在腳邊掉落並設為該玩家可拾取。
     */
    public void giveTo(Player player) {
        List<ItemStack> items = drain(player);
        if (items.isEmpty()) return;
        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                org.bukkit.entity.Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), drop);
                try {
                    dropped.setOwner(player.getUniqueId());
                } catch (Throwable ignored) {
                    // 老版本 API 沒有 setOwner，忽略
                }
            }
        }
        player.sendMessage("§a你領取了 " + items.size() + " 件 KOTH 離線獎勵！");
    }
}
