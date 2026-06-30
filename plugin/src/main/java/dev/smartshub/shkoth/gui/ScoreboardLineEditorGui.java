package dev.smartshub.shkoth.gui;

import dev.smartshub.shkoth.message.MessageParser;
import dev.smartshub.shkoth.service.gui.GuiService;
import dev.smartshub.shkoth.service.gui.input.AnvilTextInput;
import dev.smartshub.shkoth.service.gui.menu.cache.KothTempData;
import dev.smartshub.shkoth.service.gui.menu.cache.KothToRegisterCache;
import dev.smartshub.shkoth.service.gui.menu.other.KothLoreBoardPreview;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 計分板每行內容編輯器。
 *
 * 一個 6 列 GUI：
 *   - row 0：返回 / 標題說明 / 新增一行 / 清空
 *   - row 1-4：每列代表一行，含 編輯／上移／下移／刪除 按鈕（4 列最多 36 行；超出走分頁）
 *   - row 5：上一頁 / 頁碼 / 下一頁 / 返回 CreateKothGui
 */
public class ScoreboardLineEditorGui {

    private static final int LINES_PER_PAGE = 4;

    private static final int SLOT_BACK = 0;
    private static final int SLOT_TITLE = 4;
    private static final int SLOT_ADD = 7;
    private static final int SLOT_CLEAR = 8;

    private static final int CONTENT_ROW_START = 1;

    private static final int SLOT_PREV = 45;
    private static final int SLOT_PAGE_INFO = 49;
    private static final int SLOT_NEXT = 53;

    private final MessageParser parser;
    private final KothToRegisterCache cache;
    private final KothLoreBoardPreview boardPreview;

    private GuiService guiService;

    // 每個玩家當前編輯哪一組（capturing/waiting）跟頁碼
    private final Map<UUID, Boolean> editingCapturing = new HashMap<>();
    private final Map<UUID, Integer> currentPage = new HashMap<>();

    public ScoreboardLineEditorGui(MessageParser parser, KothToRegisterCache cache,
                                   KothLoreBoardPreview boardPreview) {
        this.parser = parser;
        this.cache = cache;
        this.boardPreview = boardPreview;
    }

    public void setGuiService(GuiService guiService) {
        this.guiService = guiService;
    }

    public void open(Player player, boolean capturing) {
        editingCapturing.put(player.getUniqueId(), capturing);
        currentPage.putIfAbsent(player.getUniqueId(), 0);
        render(player);
    }

    private void render(Player player) {
        boolean capturing = editingCapturing.getOrDefault(player.getUniqueId(), false);
        KothTempData data = cache.getKothToRegister(player.getUniqueId());
        if (data == null) {
            player.closeInventory();
            return;
        }
        List<String> lines = data.getLines(capturing);
        int page = clampPage(player, lines.size());

        Gui gui = Gui.gui()
                .title(parser.parse("<gold>" + (capturing ? "佔領時" : "等待時") + "計分板內容"))
                .rows(6)
                .create();

        // === Header ===
        gui.setItem(SLOT_BACK, ItemBuilder.from(Material.ARROW)
                .name(parser.parse("<yellow>返回"))
                .lore(parser.parse("<gray>回到 KOTH 編輯介面"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    boardPreview.invalidate(player.getUniqueId());
                    if (guiService != null) guiService.openCreateKothGui(player);
                }));

        gui.setItem(SLOT_TITLE, ItemBuilder.from(Material.PAPER)
                .name(parser.parse("<aqua>" + (capturing ? "佔領時" : "等待時") + "計分板"))
                .lore(parser.parseList(List.of(
                        "<gray>共 <yellow>" + lines.size() + " <gray>行",
                        "<dark_gray>左鍵編輯 / 右鍵刪除",
                        "<dark_gray>Shift+左鍵 上移 / Shift+右鍵 下移")))
                .asGuiItem(e -> e.setCancelled(true)));

        gui.setItem(SLOT_ADD, ItemBuilder.from(Material.EMERALD)
                .name(parser.parse("<green>新增一行"))
                .lore(parser.parse("<gray>用 Anvil 輸入文字"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    openAnvilForNewLine(player, capturing);
                }));

        gui.setItem(SLOT_CLEAR, ItemBuilder.from(Material.BARRIER)
                .name(parser.parse("<red>清空所有行"))
                .lore(parser.parse("<dark_red>不可復原"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    if (capturing) data.clearCapturingLines();
                    else data.clearWaitingLines();
                    boardPreview.invalidate(player.getUniqueId());
                    render(player);
                }));

        // === Content rows ===
        int firstIdx = page * LINES_PER_PAGE;
        for (int row = 0; row < LINES_PER_PAGE; row++) {
            int idx = firstIdx + row;
            int baseSlot = (CONTENT_ROW_START + row) * 9;
            if (idx >= lines.size()) {
                fillEmpty(gui, baseSlot);
            } else {
                placeLineRow(gui, player, capturing, lines.get(idx), idx, baseSlot);
            }
        }

        // === Footer pagination ===
        int totalPages = Math.max(1, (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        gui.setItem(SLOT_PREV, ItemBuilder.from(page > 0 ? Material.SPECTRAL_ARROW : Material.GRAY_DYE)
                .name(parser.parse(page > 0 ? "<yellow>上一頁" : "<dark_gray>上一頁"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    if (page > 0) {
                        currentPage.put(player.getUniqueId(), page - 1);
                        render(player);
                    }
                }));

        gui.setItem(SLOT_PAGE_INFO, ItemBuilder.from(Material.BOOK)
                .name(parser.parse("<aqua>第 " + (page + 1) + " / " + totalPages + " 頁"))
                .asGuiItem(e -> e.setCancelled(true)));

        gui.setItem(SLOT_NEXT, ItemBuilder.from(page < totalPages - 1 ? Material.SPECTRAL_ARROW : Material.GRAY_DYE)
                .name(parser.parse(page < totalPages - 1 ? "<yellow>下一頁" : "<dark_gray>下一頁"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    if (page < totalPages - 1) {
                        currentPage.put(player.getUniqueId(), page + 1);
                        render(player);
                    }
                }));

        // Fill remaining
        GuiItem filler = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(parser.parse(" "))
                .asGuiItem(e -> e.setCancelled(true));
        for (int s = 45; s < 54; s++) {
            if (gui.getGuiItem(s) == null) gui.setItem(s, filler);
        }
        for (int s = 0; s < 9; s++) {
            if (gui.getGuiItem(s) == null) gui.setItem(s, filler);
        }

        gui.open(player);
    }

    private void placeLineRow(Gui gui, Player player, boolean capturing, String text, int idx, int baseSlot) {
        // baseSlot = 該列的第 0 格 (0/9/18/27/36/45)
        // 排版：[編號] [文字 ×5] [上移] [下移] [刪除]
        gui.setItem(baseSlot, ItemBuilder.from(Material.NAME_TAG)
                .name(parser.parse("<gold>#" + (idx + 1)))
                .asGuiItem(e -> e.setCancelled(true)));

        GuiItem textItem = ItemBuilder.from(Material.PAPER)
                .name(parser.parse("<white>" + (text == null || text.isEmpty() ? "(空)" : text)))
                .lore(parser.parseList(List.of(
                        "<dark_gray>左鍵 <gray>編輯",
                        "<dark_gray>右鍵 <gray>刪除此行")))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    if (e.getClick().isShiftClick()) {
                        if (e.getClick().isLeftClick()) moveUp(player, capturing, idx);
                        else if (e.getClick().isRightClick()) moveDown(player, capturing, idx);
                    } else if (e.getClick().isLeftClick()) {
                        openAnvilForEdit(player, capturing, idx, text);
                    } else if (e.getClick().isRightClick()) {
                        deleteLine(player, capturing, idx);
                    }
                });
        for (int i = 1; i <= 5; i++) gui.setItem(baseSlot + i, textItem);

        gui.setItem(baseSlot + 6, ItemBuilder.from(Material.ARROW)
                .name(parser.parse("<yellow>↑ 上移"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    moveUp(player, capturing, idx);
                }));

        gui.setItem(baseSlot + 7, ItemBuilder.from(Material.ARROW)
                .name(parser.parse("<yellow>↓ 下移"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    moveDown(player, capturing, idx);
                }));

        gui.setItem(baseSlot + 8, ItemBuilder.from(Material.LAVA_BUCKET)
                .name(parser.parse("<red>刪除"))
                .asGuiItem(e -> {
                    e.setCancelled(true);
                    deleteLine(player, capturing, idx);
                }));
    }

    private void fillEmpty(Gui gui, int baseSlot) {
        GuiItem filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(parser.parse(" "))
                .asGuiItem(e -> e.setCancelled(true));
        for (int i = 0; i < 9; i++) gui.setItem(baseSlot + i, filler);
    }

    private int clampPage(Player player, int totalLines) {
        int totalPages = Math.max(1, (totalLines + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        int p = currentPage.getOrDefault(player.getUniqueId(), 0);
        if (p >= totalPages) p = totalPages - 1;
        if (p < 0) p = 0;
        currentPage.put(player.getUniqueId(), p);
        return p;
    }

    private void openAnvilForNewLine(Player player, boolean capturing) {
        AnvilTextInput anvil = AnvilTextInput.get();
        if (anvil == null) return;
        anvil.prompt(player, "", text -> {
            if (text == null || text.isEmpty()) {
                open(player, capturing);
                return;
            }
            KothTempData data = cache.getKothToRegister(player.getUniqueId());
            if (data == null) return;
            if (capturing) data.addCapturingLine(text);
            else data.addWaitingLine(text);
            boardPreview.invalidate(player.getUniqueId());
            open(player, capturing);
        }, () -> open(player, capturing));
    }

    private void openAnvilForEdit(Player player, boolean capturing, int idx, String current) {
        AnvilTextInput anvil = AnvilTextInput.get();
        if (anvil == null) return;
        anvil.prompt(player, current == null ? "" : current, text -> {
            KothTempData data = cache.getKothToRegister(player.getUniqueId());
            if (data == null) return;
            data.setLineAt(capturing, idx, text);
            boardPreview.invalidate(player.getUniqueId());
            open(player, capturing);
        }, () -> open(player, capturing));
    }

    private void deleteLine(Player player, boolean capturing, int idx) {
        KothTempData data = cache.getKothToRegister(player.getUniqueId());
        if (data == null) return;
        data.removeLineAt(capturing, idx);
        boardPreview.invalidate(player.getUniqueId());
        render(player);
    }

    private void moveUp(Player player, boolean capturing, int idx) {
        if (idx <= 0) return;
        KothTempData data = cache.getKothToRegister(player.getUniqueId());
        if (data == null) return;
        data.moveLine(capturing, idx, idx - 1);
        boardPreview.invalidate(player.getUniqueId());
        render(player);
    }

    private void moveDown(Player player, boolean capturing, int idx) {
        KothTempData data = cache.getKothToRegister(player.getUniqueId());
        if (data == null) return;
        List<String> lines = data.getLines(capturing);
        if (idx >= lines.size() - 1) return;
        data.moveLine(capturing, idx, idx + 1);
        boardPreview.invalidate(player.getUniqueId());
        render(player);
    }
}
