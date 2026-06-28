package dev.smartshub.shkoth.service.gui.menu.other;

import dev.smartshub.shkoth.service.gui.menu.cache.KothToRegisterCache;
import dev.smartshub.shkoth.message.MessageParser;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KothLoreBoardPreview {

    private final KothToRegisterCache kothToRegisterCache;
    private final MessageParser parser;

    public KothLoreBoardPreview(KothToRegisterCache kothToRegisterCache, MessageParser parser) {
        this.kothToRegisterCache = kothToRegisterCache;
        this.parser = parser;
    }

    public List<Component> getCapturingLore(UUID uuid) {
        var data = kothToRegisterCache.getKothToRegister(uuid);

        List<Component> lore = new ArrayList<>();

        lore.add(parser.parse("<dark_gray>右鍵點擊: <gray>新增內容行"));
        lore.add(parser.parse("<dark_gray>左鍵點擊: <gray>移除最後一行"));
        lore.add(parser.parse("<dark_gray>Shift 點擊: <gray>清除所有內容"));
        lore.add(Component.empty());
        lore.add(parser.parse("<yellow><bold>預覽效果:"));
        lore.add(Component.empty());

        String title = data.getScoreboardCapturingTitle();
        lore.add(parseForLore("標題: " + (title != null ? title : "尚未設定"), "<gold>"));

        lore.add(Component.empty());

        List<String> content = data.getScoreboardCapturingContent();
        if (content == null || content.isEmpty()) {
            lore.add(parser.parse("<dark_gray>未設定內容"));
        } else {
            lore.add(parser.parse("<aqua>內容行數 (" + content.size() + "):"));
            for (int i = 0; i < content.size(); i++) {
                String line = content.get(i);
                lore.add(parseForLore((i + 1) + ". " + line, "<white>"));
            }
        }

        return lore;
    }

    public List<Component> getWaitingLore(UUID uuid) {
        var data = kothToRegisterCache.getKothToRegister(uuid);

        List<Component> lore = new ArrayList<>();

        lore.add(parser.parse("<dark_gray>右鍵點擊: <gray>新增內容行"));
        lore.add(parser.parse("<dark_gray>左鍵點擊: <gray>移除最後一行"));
        lore.add(parser.parse("<dark_gray>Shift 點擊: <gray>清除所有內容"));
        lore.add(Component.empty());
        lore.add(parser.parse("<yellow><bold>預覽效果:"));
        lore.add(Component.empty());

        String title = data.getScoreboardWaitingTitle();
        lore.add(parseForLore("標題: " + (title != null ? title : "尚未設定"), "<gold>"));

        lore.add(Component.empty());

        List<String> content = data.getScoreboardWaitingContent();
        if (content == null || content.isEmpty()) {
            lore.add(parser.parse("<dark_gray>未設定內容"));
        } else {
            lore.add(parser.parse("<aqua>內容行數 (" + content.size() + "):"));
            for (int i = 0; i < content.size(); i++) {
                String line = content.get(i);
                lore.add(parseForLore((i + 1) + ". " + line, "<white>"));
            }
        }

        return lore;
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