package dev.smartshub.shkoth.api.koth.guideline;

import java.util.EnumSet;
import java.util.Set;

/**
 * 通知顯示模式預設值。實際內部以 {@link NotifyChannel} 的集合處理，
 * 此 enum 僅為設定檔（notify-type）的便利別名。
 */
public enum NotifyType {
    TITLE("Title (標題)", EnumSet.of(NotifyChannel.TITLE, NotifyChannel.ACTIONBAR)),
    CHAT("Chat (聊天訊息)", EnumSet.of(NotifyChannel.CHAT)),
    BOSSBAR("Bossbar (頂部血條)", EnumSet.of(NotifyChannel.BOSSBAR)),
    ALL("All (全部顯示)", EnumSet.allOf(NotifyChannel.class));

    private final String displayName;
    private final Set<NotifyChannel> channels;

    NotifyType(String displayName, Set<NotifyChannel> channels) {
        this.displayName = displayName;
        this.channels = channels;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 回傳此 NotifyType 對應的頻道集合。NotifyService 可依此判斷是否要送出某個頻道。
     */
    public Set<NotifyChannel> channels() {
        return EnumSet.copyOf(channels);
    }

    public boolean includes(NotifyChannel channel) {
        return channels.contains(channel);
    }
}
