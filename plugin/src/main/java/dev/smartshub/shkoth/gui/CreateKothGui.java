package dev.smartshub.shkoth.gui;

import dev.smartshub.shkoth.api.gui.BaseUpdatableGui;
import dev.smartshub.shkoth.api.koth.guideline.KothType;
import dev.smartshub.shkoth.api.koth.guideline.NotifyType;
import dev.smartshub.shkoth.message.MessageParser;
import dev.smartshub.shkoth.registry.KothRegistry;
import dev.smartshub.shkoth.service.gui.GuiService;
import dev.smartshub.shkoth.service.gui.input.AnvilTextInput;
import dev.smartshub.shkoth.service.gui.menu.cache.KothTempData;
import dev.smartshub.shkoth.service.gui.menu.cache.KothToRegisterCache;
import dev.smartshub.shkoth.service.gui.menu.cache.KothValidation;
import dev.smartshub.shkoth.service.gui.menu.other.KothLoreBoardPreview;
import dev.smartshub.shkoth.service.wand.WandService;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 分頁式 KOTH 建立/編輯 GUI。
 * 共 6 列 54 格：
 *   row 0  (0-8)   ：分頁切換 + 取消
 *   row 1-4 (9-44) ：當前分頁的內容（每頁配置不同）
 *   row 5  (45-53) ：底排動作鈕（測試／複製／驗證資訊／儲存）
 *
 * 編輯模式偵測：若 tempData 的 id 已存在於 KothRegistry，視為編輯既有 KOTH。
 */
public class CreateKothGui extends BaseUpdatableGui {

    // ===== Slot 常數（取代魔數）=====
    private static final int SLOT_TAB_BASIC = 1;
    private static final int SLOT_TAB_TIME = 3;
    private static final int SLOT_TAB_REWARD = 5;
    private static final int SLOT_CLOSE = 8;

    // Tab 1 - 基本資料
    private static final int SLOT_B_ID = 20;
    private static final int SLOT_B_NAME = 22;
    private static final int SLOT_B_WAND = 24;
    private static final int SLOT_B_TYPE = 30;
    private static final int SLOT_B_NOTIFY = 32;

    // Tab 2 - 時間與規則 / 計分板
    private static final int SLOT_T_MAX = 19;
    private static final int SLOT_T_CAP = 21;
    private static final int SLOT_T_BOSS = 23;
    private static final int SLOT_T_SCB_TOGGLE = 25;
    private static final int SLOT_T_WAIT_TITLE = 28;
    private static final int SLOT_T_WAIT_LINES = 30;
    private static final int SLOT_T_CAP_TITLE = 32;
    private static final int SLOT_T_CAP_LINES = 34;

    // Tab 3 - 獎勵與指令
    private static final int SLOT_R_REWARDS = 20;
    private static final int SLOT_R_POOL_INFO = 22;
    private static final int SLOT_R_COMMANDS = 24;

    // Footer
    private static final int SLOT_F_TEST = 47;
    private static final int SLOT_F_COPY = 49;
    private static final int SLOT_F_VALIDATION = 51;
    private static final int SLOT_F_SAVE = 53;

    private static final int CONTENT_FIRST = 9;
    private static final int CONTENT_LAST = 44;
    private static final int FOOTER_FIRST = 45;
    private static final int FOOTER_LAST = 53;

    private static final int TAB_BASIC = 0;
    private static final int TAB_TIME = 1;
    private static final int TAB_REWARD = 2;

    private final MessageParser parser;
    private final KothToRegisterCache cache;
    private final KothLoreBoardPreview boardPreview;
    private final WandService wandService;
    private final KothRegistry kothRegistry;

    private GuiService guiService;
    private final Map<UUID, Integer> currentTab = new HashMap<>();
    private final Map<UUID, Gui> openGuis = new HashMap<>();

    public CreateKothGui(MessageParser parser,
                         KothToRegisterCache cache,
                         WandService wandService,
                         KothRegistry kothRegistry) {
        this.parser = parser;
        this.cache = cache;
        this.wandService = wandService;
        this.kothRegistry = kothRegistry;
        this.boardPreview = new KothLoreBoardPreview(cache, parser);
    }

    public void setGuiService(GuiService guiService) {
        this.guiService = guiService;
    }

    @Override
    public void open(Player player) {
        cache.addKothToRegister(player.getUniqueId());
        currentTab.putIfAbsent(player.getUniqueId(), TAB_BASIC);
        Gui gui = buildGui(player);
        openGuis.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    @Override
    protected void registerAllUpdaters() {
        // 此 GUI 採分頁，updater 是每次切頁時動態註冊；這裡留空。
    }

    private Gui buildGui(Player player) {
        boolean editing = isEditingExisting(player);
        Gui gui = Gui.gui()
                .title(parser.parse(editing ? "<gold>編輯 KOTH" : "<gold>建立 KOTH"))
                .rows(6)
                .create();

        clearItemUpdaters();
        registerTabBar();
        registerFooter(editing);
        registerCurrentTabContent(player);

        setupAllItems(gui, player);
        fillContentBackground(gui);
        fillFooterBackground(gui);
        return gui;
    }

    private boolean isEditingExisting(Player player) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return data != null
                && data.getId() != null
                && !data.getId().equals("example")
                && kothRegistry.get(data.getId()) != null;
    }

    // ===== Tab bar =====
    private void registerTabBar() {
        registerItemUpdater(SLOT_TAB_BASIC, (p, g) -> tabButton(p, "基本資料", Material.WRITABLE_BOOK, TAB_BASIC));
        registerItemUpdater(SLOT_TAB_TIME, (p, g) -> tabButton(p, "時間與計分", Material.CLOCK, TAB_TIME));
        registerItemUpdater(SLOT_TAB_REWARD, (p, g) -> tabButton(p, "獎勵與指令", Material.CHEST, TAB_REWARD));
        registerItemUpdater(SLOT_CLOSE, (p, g) -> ItemBuilder.from(Material.BARRIER)
                .name(parser.parse("<red>關閉視窗"))
                .lore(parser.parse("<gray>會保留目前未儲存的編輯狀態"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    p.closeInventory();
                }));
    }

    private GuiItem tabButton(Player player, String label, Material mat, int tab) {
        boolean active = currentTab.getOrDefault(player.getUniqueId(), TAB_BASIC) == tab;
        ItemBuilder b = ItemBuilder.from(active ? Material.LIME_STAINED_GLASS_PANE : mat)
                .name(parser.parse((active ? "<green>▶ " : "<yellow>") + label))
                .lore(parser.parse(active ? "<dark_gray>目前分頁" : "<gray>點擊切換"));
        if (active) b.glow();
        return b.asGuiItem(event -> {
            event.setCancelled(true);
            currentTab.put(player.getUniqueId(), tab);
            refreshAll(player);
        });
    }

    // ===== Footer =====
    private void registerFooter(boolean editing) {
        registerItemUpdater(SLOT_F_TEST, this::footerTestButton);
        registerItemUpdater(SLOT_F_COPY, this::footerCopyButton);
        registerItemUpdater(SLOT_F_VALIDATION, this::footerValidation);
        registerItemUpdater(SLOT_F_SAVE, (p, g) -> footerSaveButton(p, g, editing));
    }

    private GuiItem footerTestButton(Player player, Gui gui) {
        var v = cache.validateKoth(player.getUniqueId());
        boolean ok = v.valid();
        return ItemBuilder.from(ok ? Material.FIREWORK_ROCKET : Material.GRAY_DYE)
                .name(parser.parse(ok ? "<aqua>測試此 KOTH" : "<dark_gray>測試此 KOTH"))
                .lore(parser.parse(ok
                        ? "<gray>立刻啟動一回合驗證設定"
                        : "<red>必填欄位齊全後才能測試"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if (!ok) return;
                    var tempId = cache.getKothToRegister(player.getUniqueId()).getId();
                    var koth = kothRegistry.get(tempId);
                    if (koth == null) {
                        player.sendMessage(parser.parse("<red>請先儲存 KOTH 再測試！"));
                        return;
                    }
                    try {
                        koth.start();
                        player.sendMessage(parser.parse("<green>已啟動 " + tempId));
                        player.closeInventory();
                    } catch (Throwable t) {
                        player.sendMessage(parser.parse("<red>啟動失敗：" + t.getMessage()));
                    }
                });
    }

    private GuiItem footerCopyButton(Player player, Gui gui) {
        boolean hasOthers = !kothRegistry.getAll().isEmpty();
        return ItemBuilder.from(hasOthers ? Material.WRITTEN_BOOK : Material.GRAY_DYE)
                .name(parser.parse(hasOthers ? "<yellow>複製其他 KOTH 設定" : "<dark_gray>複製其他 KOTH 設定"))
                .lore(parser.parseList(List.of(
                        "<gray>從現有 KOTH 載入欄位",
                        hasOthers ? "<dark_gray>點擊開啟列表" : "<red>目前沒有其他 KOTH 可複製"
                )))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if (!hasOthers) return;
                    player.closeInventory();
                    if (guiService != null) {
                        guiService.openEditKothListGui(player);
                    }
                });
    }

    private GuiItem footerValidation(Player player, Gui gui) {
        KothValidation.ValidationResult result = cache.validateKoth(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        if (result.valid()) {
            lore.add("<green>✓ 所有必填欄位齊全");
        } else {
            lore.add("<red>缺少 " + result.errors().size() + " 項：");
            for (String err : result.errors()) {
                lore.add("<red> - " + translateError(err));
            }
        }
        // 額外提示：捕獲時間不可大於最長時間
        var data = cache.getKothToRegister(player.getUniqueId());
        if (data != null && data.getCaptureTime() > data.getMaxTime()) {
            lore.add("<red>✗ 佔領時間 " + data.getCaptureTime()
                    + " 秒 > 最長時間 " + data.getMaxTime() + " 秒");
        }
        return ItemBuilder.from(result.valid() ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
                .name(parser.parse(result.valid() ? "<green>驗證通過" : "<red>尚未通過驗證"))
                .lore(parser.parseList(lore))
                .asGuiItem(event -> event.setCancelled(true));
    }

    private GuiItem footerSaveButton(Player player, Gui gui, boolean editing) {
        KothValidation.ValidationResult v = cache.validateKoth(player.getUniqueId());
        var data = cache.getKothToRegister(player.getUniqueId());
        boolean captureValid = data != null && data.getCaptureTime() <= data.getMaxTime();
        boolean idClash = !editing && data != null && data.getId() != null
                && kothRegistry.get(data.getId()) != null;
        boolean canSave = v.valid() && captureValid && !idClash;

        Material mat = !canSave ? Material.GRAY_DYE
                : (editing ? Material.ANVIL : Material.SLIME_BALL);
        String name = editing ? "<green>儲存變更" : "<green>建立 KOTH";

        List<String> lore = new ArrayList<>();
        lore.add(canSave ? "<gray>點擊以" + (editing ? "儲存" : "建立")
                : "<red>條件未達成，無法儲存");
        if (idClash) lore.add("<red>✗ ID 已存在");
        if (!captureValid) lore.add("<red>✗ 佔領時間超過最長活動時間");

        ItemBuilder b = ItemBuilder.from(mat).name(parser.parse(name)).lore(parser.parseList(lore));
        if (canSave) b.glow();

        return b.asGuiItem(event -> {
            event.setCancelled(true);
            if (!canSave) {
                player.sendMessage(parser.parse("<red>請先解決驗證提示中的問題！"));
                return;
            }
            player.closeInventory();
            // 編輯模式：先取消註冊舊 KOTH，讓 registrationService 重新註冊新版本
            if (editing) {
                kothRegistry.unregister(data.getId());
            }
            cache.buildKoth(player.getUniqueId());
            cache.removeKothToRegister(player.getUniqueId());
            currentTab.remove(player.getUniqueId());
            openGuis.remove(player.getUniqueId());
            player.sendMessage(parser.parse(editing
                    ? "<green>已儲存 KOTH 變更！"
                    : "<green>您已成功建立 KOTH！"));
        });
    }

    // ===== Tab content =====
    private void registerCurrentTabContent(Player player) {
        int tab = currentTab.getOrDefault(player.getUniqueId(), TAB_BASIC);
        switch (tab) {
            case TAB_BASIC -> registerBasicTab();
            case TAB_TIME -> registerTimeTab();
            case TAB_REWARD -> registerRewardTab();
        }
    }

    private void registerBasicTab() {
        registerItemUpdater(SLOT_B_ID, this::itemId);
        registerItemUpdater(SLOT_B_NAME, this::itemDisplayName);
        registerItemUpdater(SLOT_B_WAND, this::itemWand);
        registerItemUpdater(SLOT_B_TYPE, this::itemType);
        registerItemUpdater(SLOT_B_NOTIFY, this::itemNotify);
    }

    private void registerTimeTab() {
        registerItemUpdater(SLOT_T_MAX, this::itemMaxTime);
        registerItemUpdater(SLOT_T_CAP, this::itemCaptureTime);
        registerItemUpdater(SLOT_T_BOSS, this::itemBossbar);
        registerItemUpdater(SLOT_T_SCB_TOGGLE, this::itemScoreboardToggle);
        registerItemUpdater(SLOT_T_WAIT_TITLE, this::itemWaitingBoardTitle);
        registerItemUpdater(SLOT_T_WAIT_LINES, this::itemWaitingBoardLines);
        registerItemUpdater(SLOT_T_CAP_TITLE, this::itemCapturingBoardTitle);
        registerItemUpdater(SLOT_T_CAP_LINES, this::itemCapturingBoardLines);
    }

    private void registerRewardTab() {
        registerItemUpdater(SLOT_R_REWARDS, this::itemRewards);
        registerItemUpdater(SLOT_R_POOL_INFO, this::itemPoolInfo);
        registerItemUpdater(SLOT_R_COMMANDS, this::itemCommands);
    }

    // ===== 工廠：減少 boilerplate =====
    /**
     * 用 Anvil GUI 收集文字輸入，取代「關閉視窗→聊天輸入」流程。
     * validator 回傳 null = 通過；否則為錯誤訊息（會顯示給玩家後重新開啟 anvil）。
     */
    private GuiItem promptItem(Player player, Material mat, String name, String currentText,
                               boolean required, Consumer<String> setter,
                               Function<String, String> validator) {
        boolean filled = currentText != null && !currentText.isBlank() && !"example".equals(currentText);
        List<String> lore = new ArrayList<>();
        lore.add("<dark_gray>目前: <gray>" + (filled ? currentText : "尚未設定"));
        if (required) lore.add(filled ? "<green>✓ 必填" : "<red>✗ 必填");
        lore.add("<yellow>點擊以輸入 (Anvil)");
        return ItemBuilder.from(mat)
                .name(parser.parse("<yellow>" + name))
                .lore(parser.parseList(lore))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    openAnvilPrompt(player, currentText, setter, validator);
                });
    }

    private void openAnvilPrompt(Player player, String initial,
                                 Consumer<String> setter,
                                 Function<String, String> validator) {
        AnvilTextInput anvil = AnvilTextInput.get();
        if (anvil == null) {
            player.sendMessage(parser.parse("<red>Anvil 輸入系統未初始化"));
            return;
        }
        anvil.prompt(player, initial == null ? "" : initial,
                text -> {
                    String err = validator == null ? null : validator.apply(text);
                    if (err != null) {
                        player.sendMessage(parser.parse("<red>" + err));
                        openAnvilPrompt(player, text, setter, validator);
                        return;
                    }
                    setter.accept(text);
                    open(player);
                },
                () -> open(player));
    }

    private GuiItem numericItem(Player player, Material mat, String name, int value, String unit,
                                Consumer<Integer> setter) {
        return ItemBuilder.from(mat)
                .name(parser.parse("<yellow>" + name + ": <white>" + value + " " + unit))
                .lore(parser.parseList(List.of(
                        "<dark_gray>右鍵 <gray>+1 / <dark_gray>左鍵 <gray>-1",
                        "<dark_gray>Shift+右鍵 <gray>+10 / <dark_gray>Shift+左鍵 <gray>-10",
                        "<dark_gray>中鍵 <gray>+60")))
                .glow()
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    int delta = numericDelta(event);
                    if (delta != 0) {
                        setter.accept(Math.max(1, value + delta));
                        refreshActive(player);
                    }
                });
    }

    private GuiItem toggleItem(Player player, String name, boolean current,
                               String onText, String offText, Runnable toggle) {
        return ItemBuilder.from(current ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(parser.parse("<yellow>" + name))
                .lore(parser.parse("<gray>目前: " + (current ? "<green>" + onText : "<red>" + offText)))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    toggle.run();
                    refreshActive(player);
                });
    }

    // ===== Tab 1: 基本資料 =====
    private GuiItem itemId(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return promptItem(player, Material.BAMBOO_SIGN, "KOTH ID",
                data.getId(), true, data::setId,
                text -> {
                    if (text == null || text.isBlank()) return "ID 不可為空";
                    if (!text.matches("^[A-Za-z0-9_-]+$")) return "ID 只能含英數字、_、-";
                    var current = cache.getKothToRegister(player.getUniqueId()).getId();
                    if (!text.equals(current) && kothRegistry.get(text) != null) return "ID 已存在";
                    return null;
                });
    }

    private GuiItem itemDisplayName(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return promptItem(player, Material.CHERRY_SIGN, "顯示名稱",
                data.getDisplayName(), true, data::setDisplayName,
                text -> (text == null || text.isBlank()) ? "顯示名稱不可為空" : null);
    }

    private GuiItem itemWand(Player player, Gui gui) {
        var area = cache.getKothToRegister(player.getUniqueId()).getArea();
        List<String> lore = new ArrayList<>();
        if (area == null) {
            lore.add("<red>✗ 必填：尚未設定區域");
            lore.add("<gray>點擊取得設定棒");
        } else {
            lore.add("<green>✓ 已設定");
            lore.add("<light_purple>世界: <yellow>" + area.worldName());
            lore.add("<gray>角點 1: <yellow>" + area.corner1().toString());
            lore.add("<gray>角點 2: <yellow>" + area.corner2().toString());
        }
        return ItemBuilder.from(Material.BONE)
                .name(parser.parse("<yellow>KOTH 區域"))
                .lore(parser.parseList(lore))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    wandService.giveWand(player);
                    player.closeInventory();
                });
    }

    private GuiItem itemType(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        boolean capture = data.isCaptureType();
        return ItemBuilder.from(capture ? Material.DIAMOND_SWORD : Material.GOLDEN_AXE)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .name(parser.parse("<yellow>佔領類型: <white>" + (capture ? "佔領 (Capture)" : "計分 (Score)")))
                .lore(parser.parse("<gray>點擊切換"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    data.setType(capture ? KothType.SCORE : KothType.CAPTURE);
                    refreshActive(player);
                });
    }

    private GuiItem itemNotify(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        NotifyType current = data.getNotifyType();
        return ItemBuilder.from(Material.BELL)
                .name(parser.parse("<yellow>通知方式: <white>" + current.getDisplayName()))
                .lore(parser.parse("<gray>點擊切換顯示方式"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    NotifyType[] values = NotifyType.values();
                    data.setNotifyType(values[(current.ordinal() + 1) % values.length]);
                    refreshActive(player);
                });
    }

    // ===== Tab 2: 時間與計分 =====
    private GuiItem itemMaxTime(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return numericItem(player, Material.CLOCK, "最長活動時間", data.getMaxTime(), "秒",
                data::setMaxTime);
    }

    private GuiItem itemCaptureTime(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return numericItem(player, Material.COMPASS, "佔領所需時間", data.getCaptureTime(), "秒",
                data::setCaptureTime);
    }

    private GuiItem itemBossbar(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return toggleItem(player, "顯示頂部血條 (Bossbar)", data.isBossbarEnabled(),
                "啟用", "停用", () -> data.setBossbarEnabled(!data.isBossbarEnabled()));
    }

    private GuiItem itemScoreboardToggle(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return toggleItem(player, "啟用計分板", data.isScoreboardEnabled(),
                "啟用", "停用", () -> data.setScoreboardEnabled(!data.isScoreboardEnabled()));
    }

    private GuiItem itemWaitingBoardTitle(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return promptItem(player, Material.PAPER, "等待時計分板標題",
                data.getScoreboardWaitingTitle(), false,
                t -> { data.setScoreboardWaitingTitle(t); boardPreview.invalidate(player.getUniqueId()); },
                null);
    }

    private GuiItem itemWaitingBoardLines(Player player, Gui gui) {
        return ItemBuilder.from(Material.WRITABLE_BOOK)
                .name(parser.parse("<yellow>等待時計分板內容"))
                .lore(boardPreview.getWaitingLore(player.getUniqueId()))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if (guiService != null) guiService.openScoreboardLineEditor(player, false);
                });
    }

    private GuiItem itemCapturingBoardTitle(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        return promptItem(player, Material.NAME_TAG, "佔領時計分板標題",
                data.getScoreboardCapturingTitle(), false,
                t -> { data.setScoreboardCapturingTitle(t); boardPreview.invalidate(player.getUniqueId()); },
                null);
    }

    private GuiItem itemCapturingBoardLines(Player player, Gui gui) {
        return ItemBuilder.from(Material.BOOK)
                .name(parser.parse("<yellow>佔領時計分板內容"))
                .lore(boardPreview.getCapturingLore(player.getUniqueId()))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if (guiService != null) guiService.openScoreboardLineEditor(player, true);
                });
    }

    // ===== Tab 3: 獎勵與指令 =====
    private GuiItem itemRewards(Player player, Gui gui) {
        var data = cache.getKothToRegister(player.getUniqueId());
        int n = data.getPhysicalRewards().size();
        return ItemBuilder.from(Material.DIAMOND)
                .name(parser.parse("<yellow>實體獎勵清單 <white>(" + n + " 項)"))
                .lore(parser.parse("<gray>點擊管理獎勵物品"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    guiService.openAddPhysicalRewardGui(player);
                });
    }

    private GuiItem itemPoolInfo(Player player, Gui gui) {
        return ItemBuilder.from(Material.ENDER_PEARL)
                .name(parser.parse("<aqua>獎勵發放規則"))
                .lore(parser.parseList(List.of(
                        "<gray>每位贏家會從獎勵清單中",
                        "<gray>隨機抽 <yellow>1 項<gray>發放",
                        "<dark_gray>離線玩家會在下次上線時補發")))
                .asGuiItem(event -> event.setCancelled(true));
    }

    private GuiItem itemCommands(Player player, Gui gui) {
        return ItemBuilder.from(Material.COMMAND_BLOCK)
                .name(parser.parse("<yellow>指令清單"))
                .lore(parser.parse("<gray>管理開始/結束/獲勝指令"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    guiService.openCommandGui(player);
                });
    }

    // ===== Helpers =====
    private int numericDelta(InventoryClickEvent event) {
        ClickType c = event.getClick();
        if (c == ClickType.MIDDLE) return 60;
        if (c.isRightClick() && c.isShiftClick()) return 10;
        if (c.isLeftClick() && c.isShiftClick()) return -10;
        if (c.isRightClick()) return 1;
        if (c.isLeftClick()) return -1;
        return 0;
    }

    private void fillContentBackground(Gui gui) {
        GuiItem filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                .name(parser.parse(" "))
                .asGuiItem(e -> e.setCancelled(true));
        for (int slot = CONTENT_FIRST; slot <= CONTENT_LAST; slot++) {
            if (gui.getGuiItem(slot) == null) gui.setItem(slot, filler);
        }
    }

    private void fillFooterBackground(Gui gui) {
        GuiItem filler = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(parser.parse(" "))
                .asGuiItem(e -> e.setCancelled(true));
        for (int slot = FOOTER_FIRST; slot <= FOOTER_LAST; slot++) {
            if (gui.getGuiItem(slot) == null) gui.setItem(slot, filler);
        }
    }

    private void refreshAll(Player player) {
        Gui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;
        clearItemUpdaters();
        registerTabBar();
        registerFooter(isEditingExisting(player));
        registerCurrentTabContent(player);
        gui.getInventory().clear();
        setupAllItems(gui, player);
        fillContentBackground(gui);
        fillFooterBackground(gui);
        gui.update();
    }

    private void refreshActive(Player player) {
        // 同分頁內的某個欄位被改動：重新繪整個視窗（簡單可靠，不易遺漏 footer 更新）
        refreshAll(player);
    }

    private String translateError(String raw) {
        return switch (raw) {
            case "ID cannot be empty" -> "ID";
            case "Display name cannot be empty" -> "顯示名稱";
            case "Max time must be greater than 0" -> "最長活動時間 > 0";
            case "Capture time must be greater than 0" -> "佔領時間 > 0";
            case "Area must be set" -> "區域";
            case "KOTH type must be set" -> "佔領類型";
            default -> raw;
        };
    }
}
