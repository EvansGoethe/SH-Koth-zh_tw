package dev.smartshub.shkoth.service.gui.menu.other;

import dev.smartshub.shkoth.service.gui.menu.cache.KothToRegisterCache;
import dev.smartshub.shkoth.message.MessageParser;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 計分板預覽 lore 產生器。每次計分板資料改動時呼叫 {@link #invalidate(UUID)} 清除該玩家快取。
 */
public class KothLoreBoardPreview {

    private final KothToRegisterCache kothToRegisterCache;
    private final MessageParser parser;

    private final ConcurrentHashMap<UUID, List<Component>> capturingCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Component>> waitingCache = new ConcurrentHashMap<>();

    public KothLoreBoardPreview(KothToRegisterCache kothToRegisterCache, MessageParser parser) {
        this.kothToRegisterCache = kothToRegisterCache;
        this.parser = parser;
    }

    public void invalidate(UUID uuid) {
        capturingCache.remove(uuid);
        waitingCache.remove(uuid);
    }

    public List<Component> getCapturingLore(UUID uuid) {
        return capturingCache.computeIfAbsent(uuid, this::buildCapturingLore);
    }

    public List<Component> getWaitingLore(UUID uuid) {
        return waitingCache.computeIfAbsent(uuid, this::buildWaitingLore);
    }

    private List<Component> buildCapturingLore(UUID uuid) {
        var data = kothToRegisterCache.getKothToRegister(uuid);
        List<Component> lore = new ArrayList<>();
        appendHelpLines(lore);
        if (data == null) return lore;

        String title = data.getScoreboardCapturingTitle();
        lore.add(parseForLore("標題: " + (title != null ? title : "尚未設定"), "<gold>"));
        lore.add(Component.empty());

        List<String> content = data.getScoreboardCapturingContent();
        appendContentLines(lore, content);
        return lore;
    }

    private List<Component> buildWaitingLore(UUID uuid) {
        var data = kothToRegisterCache.getKothToRegister(uuid);
        List<Component> lore = new ArrayList<>();
        appendHelpLines(lore);
        if (data == null) return lore;

        String title = data.getScoreboardWaitingTitle();
        lore.add(parseForLore("標題: " + (title != null ? title : "尚未設定"), "<gold>"));
        lore.add(Component.empty());

        List<String> content = data.getScoreboardWaitingContent();
        appendContentLines(lore, content);
        return lore;
    }

    private void appendHelpLines(List<Component> lore) {
        lore.add(parser.parse("<dark_gray>右鍵: <gray>新增內容行"));
        lore.add(parser.parse("<dark_gray>左鍵: <gray>移除最後一行"));
        lore.add(parser.parse("<dark_gray>Shift 點擊: <gray>清除全部"));
        lore.add(Component.empty());
        lore.add(parser.parse("<yellow><bold>預覽效果:"));
        lore.add(Component.empty());
    }

    private void appendContentLines(List<Component> lore, List<String> content) {
        if (content == null || content.isEmpty()) {
            lore.add(parser.parse("<dark_gray>未設定內容"));
        } else {
            lore.add(parser.parse("<aqua>內容行數 (" + content.size() + "):"));
            for (int i = 0; i < content.size(); i++) {
                lore.add(parseForLore((i + 1) + ". " + content.get(i), "<white>"));
            }
        }
    }

    private Component parseForLore(String text, String defaultColor) {
        if (text == null || text.trim().isEmpty()) {
            return parser.parse("<gray>尚未設定");
        }
        try {
            return parser.parse(defaultColor + text);
        } catch (Exception e) {
            return parser.parse("<gray>" + text);
        }
    }
}
