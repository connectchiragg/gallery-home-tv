package com.chirag.home;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
final class WatchRule {
    static final long ATTACH_WINDOW_MS = 180000;
    static final int ATTRIBUTE = 1;
    static final int BLANK = 0;
    static final long DISMISS_GRACE_MS = 30000;
    static final long GAP_MS = 1800000;
    static final int HOLD = 2;
    static final long MOVED_MS = 15000;
    static final long SESSION_MS = 14400000;
    private static final Pattern SE = Pattern.compile("\\bS(\\d{1,2})(?: E(\\d{1,4}))?\\b");
    private static final Pattern LEFT = Pattern.compile("(?:(\\d+) h ?)?(?:(\\d+) min )?left");

    private WatchRule() {
    }

    static final class Launch {
        long attachedTs;
        long duration;
        long lastTs;
        long openedTs;

        Launch(long j, long j2, long j3, long j4) {
            this.openedTs = j;
            this.attachedTs = j2;
            this.lastTs = j3;
            this.duration = j4;
        }
    }

    static boolean plausible(long j) {
        return j >= 60000 && j <= 43200000;
    }

    static boolean plausible(long j, long j2) {
        return plausible(j) && j2 >= 0 && j2 <= j;
    }

    static int playback(Launch launch, boolean z, long j, long j2) {
        if (launch == null || launch.openedTs <= 0 || launch.attachedTs < 0) {
            return BLANK;
        }
        if (j2 < launch.openedTs) {
            return BLANK;
        }
        if (launch.attachedTs != 0) {
            if ((plausible(j) && launch.duration > 0 && Math.abs(j - launch.duration) > Math.max(5000L, launch.duration / 50)) || j2 - launch.lastTs > GAP_MS || j2 - launch.attachedTs > SESSION_MS) {
                launch.attachedTs = -1L;
                return BLANK;
            }
            if (!z) {
                return HOLD;
            }
        } else {
            if (j2 - launch.openedTs > ATTACH_WINDOW_MS) {
                launch.attachedTs = -1L;
                return BLANK;
            }
            if (!z) {
                return HOLD;
            }
            launch.attachedTs = j2;
        }
        launch.lastTs = j2;
        if (plausible(j)) {
            launch.duration = j;
        }
        return ATTRIBUTE;
    }

    static boolean live(Launch launch, long j) {
        if (launch == null || launch.openedTs <= 0 || launch.attachedTs < 0 || j < launch.openedTs) {
            return false;
        }
        if (launch.attachedTs == 0) {
            if (j - launch.openedTs > ATTACH_WINDOW_MS) {
                return false;
            }
        } else if (j - launch.lastTs > GAP_MS) {
            return false;
        }
        return true;
    }

    static boolean revived(long j, long j2, boolean z, long j3, long j4, long j5) {
        if (j <= 0 || !z || j5 - j < DISMISS_GRACE_MS) {
            return false;
        }
        return !(j4 > 0 && j3 >= 0) || Math.abs(j3 - Math.max(0L, j2)) >= MOVED_MS;
    }

    private static int minutesLeft(String str) {
        Matcher matcher = LEFT.matcher(str);
        if (!matcher.find()) {
            return -1;
        }
        if (matcher.group(ATTRIBUTE) == null && matcher.group(HOLD) == null) {
            return -1;
        }
        String strGroup = matcher.group(ATTRIBUTE);
        int i = BLANK;
        int i2 = strGroup == null ? BLANK : Integer.parseInt(matcher.group(ATTRIBUTE)) * 60;
        if (matcher.group(HOLD) != null) {
            i = Integer.parseInt(matcher.group(HOLD));
        }
        return i2 + i;
    }

    static boolean progressed(String str, String str2) {
        if (str == null || str2 == null) {
            return false;
        }
        Matcher matcher = SE.matcher(str);
        Matcher matcher2 = SE.matcher(str2);
        boolean zFind = matcher.find();
        boolean zFind2 = matcher2.find();
        if (zFind && zFind2 && !matcher.group(BLANK).equals(matcher2.group(BLANK))) {
            return true;
        }
        if (zFind != zFind2) {
            return false;
        }
        int iMinutesLeft = minutesLeft(str);
        int iMinutesLeft2 = minutesLeft(str2);
        if (iMinutesLeft < 0 || iMinutesLeft2 < 0 || iMinutesLeft2 >= iMinutesLeft) {
            return false;
        }
        return true;
    }

    static boolean hidesBlank(long j, long j2) {
        return j >= j2;
    }
}
