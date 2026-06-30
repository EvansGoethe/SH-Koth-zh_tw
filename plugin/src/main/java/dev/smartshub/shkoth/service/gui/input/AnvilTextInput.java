package dev.smartshub.shkoth.service.gui.input;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * 在 Bukkit 原生 anvil 介面內收集玩家輸入，取代「關閉視窗→聊天欄輸入」流程。
 *
 * 流程：
 *   1. {@link #prompt(Player, String, Consumer, Runnable)} 開啟一個 anvil view，slot 0 放 displayName = initial 的紙
 *   2. 玩家在 anvil 名稱欄編輯 → 我們在 PrepareAnvilEvent 同步把 slot 2 設為帶最新文字的結果
 *   3. 玩家點 slot 2 → 呼叫 onConfirm(text) 並關閉 anvil
 *   4. 直接關視窗 → 呼叫 onCancel
 */
public final class AnvilTextInput implements Listener {

    private static volatile AnvilTextInput instance;

    private final Plugin plugin;
    private final Map<UUID, PendingPrompt> pending = new ConcurrentHashMap<>();

    public AnvilTextInput(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        instance = this;
    }

    public static AnvilTextInput get() {
        return instance;
    }

    public void prompt(Player player, String initial,
                       Consumer<String> onConfirm, Runnable onCancel) {
        Inventory inv = Bukkit.createInventory(player, InventoryType.ANVIL);
        ItemStack placeholder = new ItemStack(Material.PAPER);
        ItemMeta meta = placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(initial == null ? "" : initial));
            placeholder.setItemMeta(meta);
        }
        inv.setItem(0, placeholder);

        PendingPrompt pp = new PendingPrompt(inv, onConfirm, onCancel);
        pending.put(player.getUniqueId(), pp);

        Bukkit.getScheduler().runTask(plugin, () -> {
            var view = player.openInventory(inv);
            if (view instanceof AnvilView anvilView) {
                anvilView.setRepairCost(0);
                anvilView.setMaximumRepairCost(0);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        UUID uuid = event.getView().getPlayer().getUniqueId();
        PendingPrompt pp = pending.get(uuid);
        if (pp == null) return;
        if (event.getInventory() != pp.anvilInv) return;

        AnvilInventory anvil = event.getInventory();
        ItemStack input = anvil.getItem(0);
        if (input == null) return;

        String renameText = event.getView() instanceof AnvilView av ? av.getRenameText() : "";
        ItemStack result = input.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(renameText == null ? "" : renameText));
            result.setItemMeta(meta);
        }
        event.setResult(result);
        if (event.getView() instanceof AnvilView anvilView) {
            anvilView.setRepairCost(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        UUID uuid = event.getWhoClicked().getUniqueId();
        PendingPrompt pp = pending.get(uuid);
        if (pp == null) return;
        if (event.getInventory() != pp.anvilInv) return;

        event.setCancelled(true);

        if (event.getRawSlot() == 2) {
            String text = extractText(event.getInventory(),
                    event.getView() instanceof AnvilView av ? av : null);
            pp.confirmed = true;
            pending.remove(uuid);
            Player player = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                try {
                    pp.onConfirm.accept(text);
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "AnvilTextInput onConfirm 拋出例外", t);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PendingPrompt pp = pending.remove(uuid);
        if (pp == null) return;
        if (event.getInventory() != pp.anvilInv) return;
        if (pp.confirmed) return;

        if (pp.onCancel != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    pp.onCancel.run();
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.SEVERE, "AnvilTextInput onCancel 拋出例外", t);
                }
            });
        }
    }

    private String extractText(Inventory anvilInv, AnvilView view) {
        if (!(anvilInv instanceof AnvilInventory anvil)) return "";
        String text = view != null ? view.getRenameText() : null;
        if (text != null && !text.isEmpty()) return text;
        ItemStack result = anvil.getItem(2);
        if (result != null && result.hasItemMeta()) {
            Component name = result.getItemMeta().displayName();
            if (name != null) return PlainTextComponentSerializer.plainText().serialize(name);
        }
        ItemStack input = anvil.getItem(0);
        if (input != null && input.hasItemMeta()) {
            Component name = input.getItemMeta().displayName();
            if (name != null) return PlainTextComponentSerializer.plainText().serialize(name);
        }
        return "";
    }

    private static final class PendingPrompt {
        final Inventory anvilInv;
        final Consumer<String> onConfirm;
        final Runnable onCancel;
        boolean confirmed = false;

        PendingPrompt(Inventory anvilInv, Consumer<String> onConfirm, Runnable onCancel) {
            this.anvilInv = anvilInv;
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
        }
    }
}
