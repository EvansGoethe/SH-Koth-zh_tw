package dev.smartshub.shkoth.gui;

import dev.smartshub.shkoth.api.koth.Koth;
import dev.smartshub.shkoth.message.MessageParser;
import dev.smartshub.shkoth.registry.KothRegistry;
import dev.smartshub.shkoth.service.gui.GuiService;
import dev.smartshub.shkoth.service.gui.menu.cache.KothToRegisterCache;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EditKothListGui {

    private final KothRegistry kothRegistry;
    private final KothToRegisterCache kothToRegisterCache;
    private final MessageParser parser;
    private GuiService guiService;

    public EditKothListGui(KothRegistry kothRegistry, KothToRegisterCache kothToRegisterCache, MessageParser parser) {
        this.kothRegistry = kothRegistry;
        this.kothToRegisterCache = kothToRegisterCache;
        this.parser = parser;
    }

    public void open(Player player) {
        PaginatedGui gui = Gui.paginated()
                .title(parser.parse("<gold>選擇要編輯的 KOTH"))
                .rows(6)
                .pageSize(45)
                .create();

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        for (Koth koth : kothRegistry.getAll()) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(parser.parse("<gray>名稱: <white>" + koth.getDisplayName()));
            lore.add(parser.parse("<gray>類型: <yellow>" + koth.getType().name()));
            lore.add(parser.parse("<gray>狀態: <green>" + koth.getState().name()));
            lore.add(parser.parse(""));
            lore.add(parser.parse("<yellow>點擊以編輯此 KOTH 設定"));

            gui.addItem(ItemBuilder.from(Material.BEACON)
                    .name(parser.parse("<gold>" + koth.getId()))
                    .lore(lore)
                    .asGuiItem(event -> {
                        player.closeInventory();
                        kothToRegisterCache.addKothToRegister(player.getUniqueId());
                        var tempData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                        if (tempData != null && koth instanceof dev.smartshub.shkoth.koth.Koth impl) {
                            tempData.loadFromKoth(impl);
                        }
                        if (guiService != null) {
                            guiService.openCreateKothGui(player);
                        }
                    }));
        }

        gui.setItem(6, 3, ItemBuilder.from(Material.ARROW)
                .name(parser.parse("<yellow>上一頁"))
                .asGuiItem(event -> gui.previous()));

        gui.setItem(6, 7, ItemBuilder.from(Material.ARROW)
                .name(parser.parse("<yellow>下一頁"))
                .asGuiItem(event -> gui.next()));

        gui.open(player);
    }

    public void setGuiService(GuiService guiService) {
        this.guiService = guiService;
    }
}
