package dev.smartshub.shkoth.api.koth.guideline;

import java.util.EnumSet;
import java.util.Set;

/**
 * KOTH 通知可用的個別頻道。未來 NotifyService 可依此 EnumSet 過濾。
 * 與舊有 {@link NotifyType} 的對應關係見 {@link NotifyType#channels()}。
 */
public enum NotifyChannel {
    TITLE,
    ACTIONBAR,
    CHAT,
    BOSSBAR;

    public static final Set<NotifyChannel> ALL_CHANNELS = EnumSet.allOf(NotifyChannel.class);
}
