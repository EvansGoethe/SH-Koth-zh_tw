package dev.smartshub.shkoth.gui;

import dev.smartshub.shkoth.api.gui.BaseUpdatableGui;
import dev.smartshub.shkoth.api.koth.guideline.KothType;
import dev.smartshub.shkoth.message.MessageParser;
import dev.smartshub.shkoth.service.gui.GuiService;
import dev.smartshub.shkoth.service.gui.menu.cache.KothToRegisterCache;
import dev.smartshub.shkoth.service.gui.menu.other.KothLoreBoardPreview;
import dev.smartshub.shkoth.service.gui.menu.other.WaitingToFill;
import dev.smartshub.shkoth.service.wand.WandService;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

public class CreateKothGui extends BaseUpdatableGui {

    private final MessageParser parser;
    private final KothToRegisterCache kothToRegisterCache;
    private final KothLoreBoardPreview kothLoreBoardPreview;
    private final WandService wandService;
    private GuiService guiService;

    public CreateKothGui(MessageParser parser, KothToRegisterCache kothToRegisterCache, WandService wandService) {
        this.parser = parser;
        this.kothToRegisterCache = kothToRegisterCache;
        this.wandService = wandService;
        this.kothLoreBoardPreview = new KothLoreBoardPreview(kothToRegisterCache, parser);
    }

    @Override
    public void open(Player player) {
        kothToRegisterCache.addKothToRegister(player.getUniqueId());

        Gui gui = Gui.gui()
                .title(parser.parse("<gold>建立 KOTH"))
                .rows(5)
                .create();

        registerAllUpdaters();
        setupAllItems(gui, player);
        fillEmpty(gui);

        gui.open(player);
    }

    @Override
    protected void registerAllUpdaters() {

        registerItemUpdater(10, this::createIdItem);
        registerItemUpdater(11, this::createDisplayNameItem);
        registerItemUpdater(12, this::createMaxTimeItem);
        registerItemUpdater(13, this::createCaptureTimeItem);
        registerItemUpdater(14, this::createWandItem);
        registerItemUpdater(15, this::createTypeItem);
        registerItemUpdater(19, this::createRewardsItem);
        registerItemUpdater(20, this::createCommandsItem);
        registerItemUpdater(21, this::createCapturingBoardTitleItem);
        registerItemUpdater(22, this::createCapturingBoardLinesItem);
        registerItemUpdater(23, this::createWaitingBoardTitleItem);
        registerItemUpdater(24, this::createWaitingBoardLinesItem);
        registerItemUpdater(28, this::createSoloItem);
        registerItemUpdater(29, this::createBossbarItem);
        registerItemUpdater(30, this::createDenyEnterItem);
        registerItemUpdater(31, this::createTeamItem);
        registerItemUpdater(32, this::createScoreboardItem);
        registerItemUpdater(33, this::createCreateKothItem);
        registerItemUpdater(16, this::createAuxFiller);
        registerItemUpdater(25, this::createAuxFiller);
        registerItemUpdater(34, this::createAuxFiller);
    }

    private GuiItem createAuxFiller(Player player, Gui gui) {
        return ItemBuilder.from(Material.ORANGE_STAINED_GLASS_PANE)
                .name(parser.parse(""))
                .asGuiItem(event -> event.setCancelled(true));
    }

    private GuiItem createCreateKothItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.SLIME_BALL)
                .name(parser.parse("<green>建立 KOTH"))
                .lore(parser.parse("<gray>點擊以建立 KOTH！"))
                .glow()
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    if (!kothToRegisterCache.validateKoth(player.getUniqueId()).valid()) {
                        player.sendMessage(parser.parse("<red>您現在還無法建立 KOTH！請確認所有必要欄位皆已設定。"));
                        return;
                    }
                    player.closeInventory();
                    kothToRegisterCache.buildKoth(player.getUniqueId());
                    kothToRegisterCache.removeKothToRegister(player.getUniqueId());
                    player.sendMessage(parser.parse("<green>您已成功建立 KOTH！"));
                });
    }

    private GuiItem createIdItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.BAMBOO_SIGN)
                .name(parser.parse("<yellow>KOTH ID"))
                .lore(parser.parse("<gray>點擊以設定 KOTH ID！"),
                        parser.parse("<dark_gray>目前設定: <gray>" +
                                (kothToRegisterCache.getKothToRegister(player.getUniqueId()).getId() == null ?
                                        "尚未設定" : kothToRegisterCache.getKothToRegister(player.getUniqueId()).getId())))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    kothToRegisterCache.setWaitingToFill(player.getUniqueId(), WaitingToFill.ID);
                    player.sendMessage(parser.parse("<green>請在聊天欄輸入 KOTH ID，或輸入 <red>'cancel'<green> 取消返回:"));
                });
    }

    private GuiItem createDisplayNameItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.CHERRY_SIGN)
                .name(parser.parse("<yellow>KOTH 顯示名稱"))
                .lore(parser.parse("<gray>點擊以設定 KOTH 顯示名稱 (支援 MiniMessage 格式)！"),
                        parser.parse("<dark_gray>目前設定: <gray>" +
                                (kothToRegisterCache.getKothToRegister(player.getUniqueId()).getId() == null ?
                                        "尚未設定" : kothToRegisterCache.getKothToRegister(player.getUniqueId()).getDisplayName())))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    kothToRegisterCache.setWaitingToFill(player.getUniqueId(), WaitingToFill.DISPLAYNAME);
                    player.sendMessage(parser.parse("<green>請在聊天欄輸入 KOTH 顯示名稱，或輸入 <red>'cancel'<green> 取消返回:"));
                });
    }

    private GuiItem createMaxTimeItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.CLOCK)
                .name(parser.parse("<yellow>KOTH 最長活動時間"))
                .lore(parser.parse("<gray>Koth 運作時間: " + kothToRegisterCache.getKothToRegister(player.getUniqueId()).getMaxTime() + " 秒"),
                        parser.parse("<dark_gray>右鍵點擊: <gray>增加 1 秒"),
                        parser.parse("<dark_gray>左鍵點擊: <gray>減少 1 秒"),
                        parser.parse("<dark_gray>Shift + 右鍵: <gray>增加 10 秒"),
                        parser.parse("<dark_gray>Shift + 左鍵: <gray>減少 10 秒"))
                .glow()
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    int change = getNumericChange(event);
                    if (change != 0) {
                        var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                        kothData.setMaxTime(Math.max(1, kothData.getMaxTime() + change));
                        updateItem(gui, player, event.getSlot());
                    }
                });
    }

    private GuiItem createCaptureTimeItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.COMPASS)
                .name(parser.parse("<yellow>KOTH 佔領所需時間"))
                .lore(parser.parse("<gray>Koth 佔領時間: " + kothToRegisterCache.getKothToRegister(player.getUniqueId()).getCaptureTime() + " 秒"),
                        parser.parse("<dark_gray>右鍵點擊: <gray>增加 1 秒"),
                        parser.parse("<dark_gray>左鍵點擊: <gray>減少 1 秒"),
                        parser.parse("<dark_gray>Shift + 右鍵: <gray>增加 10 秒"),
                        parser.parse("<dark_gray>Shift + 左鍵: <gray>減少 10 秒"))
                .glow()
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    int change = getNumericChange(event);
                    if (change != 0) {
                        var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                        kothData.setCaptureTime(Math.max(1, kothData.getCaptureTime() + change));
                        updateItem(gui, player, event.getSlot());
                    }
                });
    }

    private GuiItem createWandItem(Player player, Gui gui) {
        var area = kothToRegisterCache.getKothToRegister(player.getUniqueId()).getArea();
        List<String> lore = new ArrayList<>();
        if (area == null) {
            lore.add("<gray>區域尚未設定！(請使用設定棒進行設定)");
        } else {
            lore.add("<green>設定正確！");
            lore.add("<light_purple>世界: <yellow>" + area.worldName());
            lore.add("<gray>角點 1: <yellow>" + area.corner1().toString());
            lore.add("<gray>角點 2: <yellow>" + area.corner2().toString());
        }
        return ItemBuilder.from(Material.BONE)
                .name(parser.parse("<yellow>KOTH 設定棒"))
                .lore(parser.parseList(lore))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    wandService.giveWand(player);
                    player.closeInventory();
                });
    }

    private GuiItem createTypeItem(Player player, Gui gui) {
        Material mat = kothToRegisterCache.getKothToRegister(player.getUniqueId()).getType() == KothType.CAPTURE ?
                Material.DIAMOND_SWORD : Material.GOLDEN_AXE;
        return ItemBuilder.from(mat)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .name(parser.parse("<yellow>佔領類型"))
                .lore(parser.parse("<gray>目前設定: " +
                        (kothToRegisterCache.getKothToRegister(player.getUniqueId()).isCaptureType() ? "佔領 (Capture)" : "計分 (Score)")))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                    kothData.setType(kothData.isCaptureType() ? KothType.SCORE : KothType.CAPTURE);
                    updateItem(gui, player, event.getSlot());
                });
    }

    private GuiItem createRewardsItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.DIAMOND)
                .name(parser.parse("<yellow>Koth 獎勵"))
                .lore(parser.parse("<gray>打開選單以管理實體獎勵"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    guiService.openAddPhysicalRewardGui(player);
                });
    }

    private GuiItem createCommandsItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.COMMAND_BLOCK)
                .name(parser.parse("<yellow>Koth 指令"))
                .lore(parser.parse("<gray>打開選單以管理指令"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    guiService.openCommandGui(player);
                });
    }

    private GuiItem createCapturingBoardTitleItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.NAME_TAG)
                .name(parser.parse("<yellow>佔領時計分板標題"))
                .lore(parser.parse("<gray>點擊以設定！"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    kothToRegisterCache.setWaitingToFill(player.getUniqueId(), WaitingToFill.BOARD_CAPTURING_TITLE);
                    player.sendMessage(parser.parse("<green>請在聊天欄輸入計分板標題，或輸入 <red>'cancel'<green> 取消返回:"));
                });
    }

    private GuiItem createCapturingBoardLinesItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.BOOK)
                .name(parser.parse("<yellow>佔領時計分板內容"))
                .lore(kothLoreBoardPreview.getCapturingLore(player.getUniqueId()))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    handleBoardLinesClick(event, player, gui, true, event.getSlot());
                });
    }

    private GuiItem createWaitingBoardTitleItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.PAPER)
                .name(parser.parse("<yellow>等待時計分板標題"))
                .lore(parser.parse("<gray>點擊以設定！"))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    player.closeInventory();
                    kothToRegisterCache.setWaitingToFill(player.getUniqueId(), WaitingToFill.BOARD_WAITING_TITLE);
                    player.sendMessage(parser.parse("<green>請在聊天欄輸入計分板標題，或輸入 <red>'cancel'<green> 取消返回:"));
                });
    }

    private GuiItem createWaitingBoardLinesItem(Player player, Gui gui) {
        return ItemBuilder.from(Material.WRITABLE_BOOK)
                .name(parser.parse("<yellow>等待時計分板內容"))
                .lore(kothLoreBoardPreview.getWaitingLore(player.getUniqueId()))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    handleBoardLinesClick(event, player, gui, false, event.getSlot());
                });
    }

    private GuiItem createToggleItem(Player player, Gui gui, String name,
                                     boolean currentValue, String enabledText, String disabledText, Runnable toggleAction) {
        Material mat = currentValue ? Material.LIME_DYE : Material.GRAY_DYE;
        return ItemBuilder.from(mat)
                .name(parser.parse("<yellow>" + name))
                .lore(parser.parse("<gray>Current: " + (currentValue ? enabledText : disabledText)))
                .asGuiItem(event -> {
                    event.setCancelled(true);
                    toggleAction.run();
                    updateItem(gui, player, event.getSlot());
                });
    }

    private GuiItem createSoloItem(Player player, Gui gui) {
        return createToggleItem(player, gui, "啟用/停用單人模式",
                kothToRegisterCache.getKothToRegister(player.getUniqueId()).isSolo(),
                "單人模式 (Solo)", "隊伍模式 (Team)", () -> {
                    var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                    kothData.setSolo(!kothData.isSolo());
                });
    }

    private GuiItem createBossbarItem(Player player, Gui gui) {
        return createToggleItem(player, gui, "啟用/停用血條 (Bossbar)",
                kothToRegisterCache.getKothToRegister(player.getUniqueId()).isBossbarEnabled(),
                "已啟用", "已停用", () -> {
                    var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                    kothData.setBossbarEnabled(!kothData.isBossbarEnabled());
                });
    }

    private GuiItem createDenyEnterItem(Player player, Gui gui) {
        return createToggleItem(player, gui, "阻擋非隊伍玩家進入",
                kothToRegisterCache.getKothToRegister(player.getUniqueId()).isDenyEnterWithoutTeam(),
                "已啟用", "已停用", () -> {
                    var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                    kothData.setDenyEnterWithoutTeam(!kothData.isDenyEnterWithoutTeam());
                });
    }

    private GuiItem createTeamItem(Player player, Gui gui) {
        return createToggleItem(player, gui, "進入時自動建立隊伍",
                kothToRegisterCache.getKothToRegister(player.getUniqueId()).isCreateTeamIfNotExistsOnEnter(),
                "已啟用", "已停用", () -> {
                    var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                    kothData.setCreateTeamIfNotExistsOnEnter(!kothData.isCreateTeamIfNotExistsOnEnter());
                });
    }

    private GuiItem createScoreboardItem(Player player, Gui gui) {
        return createToggleItem(player, gui, "啟用/停用計分板 (Scoreboard)",
                kothToRegisterCache.getKothToRegister(player.getUniqueId()).isScoreboardEnabled(),
                "已啟用", "已停用", () -> {
                    var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
                    kothData.setScoreboardEnabled(!kothData.isScoreboardEnabled());
                });
    }

    private int getNumericChange(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getClick().isRightClick() && event.getClick().isShiftClick()) return 10;
        if (event.getClick().isLeftClick() && event.getClick().isShiftClick()) return -10;
        if (event.getClick().isRightClick()) return 1;
        if (event.getClick().isLeftClick()) return -1;
        return 0;
    }

    private void handleBoardLinesClick(org.bukkit.event.inventory.InventoryClickEvent event, Player player, Gui gui, boolean isCapturing, int slot) {
        if (event.getClick().isRightClick()) {
            player.closeInventory();
            kothToRegisterCache.setWaitingToFill(player.getUniqueId(),
                    isCapturing ? WaitingToFill.BOARD_CAPTURING_LINE : WaitingToFill.BOARD_WAITING_LINE);
            player.sendMessage(parser.parse("<green>請在聊天欄輸入內容行，或輸入 <red>'cancel'<green> 取消返回:"));
        } else if (event.getClick().isLeftClick()) {
            var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
            if (isCapturing) kothData.removeLastCapturingLine();
            else kothData.removeLastWaitingLine();
            updateItem(gui, player, slot);
        } else if (event.getClick().isShiftClick()) {
            var kothData = kothToRegisterCache.getKothToRegister(player.getUniqueId());
            if (isCapturing) kothData.clearCapturingLines();
            else kothData.clearWaitingLines();
            updateItem(gui, player, slot);
        }
    }

    public void setGuiService(GuiService guiService) {
        this.guiService = guiService;
    }
}