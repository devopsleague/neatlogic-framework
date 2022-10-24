/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.framework.file.core.recovery;

/**
 * 恢复协调员
 */
public class RecoveryCoordinator {

    public final static long BACKOFF_COEFFICIENT_MIN = 20;
    public final static long BACKOFF_MULTIPLIER = 4;
    static long BACKOFF_COEFFICIENT_MAX = 327680; // BACKOFF_COEFFICIENT_MIN * 4^7

    private long backOffCoefficient = BACKOFF_COEFFICIENT_MIN;

    private static long UNSET = -1;
    // 测试可以独立于系统时钟直接设置时间
    private long currentTime = UNSET;
    private long next;

    public RecoveryCoordinator() {
        next = getCurrentTime() + getBackoffCoefficient();
    }

    public RecoveryCoordinator(long currentTime) {
        this.currentTime = currentTime;
        next = getCurrentTime() + getBackoffCoefficient();
    }

    /**
     * 是不是太早了
     * @return
     */
    public boolean isTooSoon() {
        long now = getCurrentTime();
        if (now > next) {
            next = now + getBackoffCoefficient();
            return false;
        } else {
            return true;
        }
    }

    void setCurrentTime(long forcedTime) {
        currentTime = forcedTime;
    }

    private long getCurrentTime() {
        if (currentTime != UNSET) {
            return currentTime;
        }
        return System.currentTimeMillis();
    }

    private long getBackoffCoefficient() {
        long currentCoeff = backOffCoefficient;
        if (backOffCoefficient < BACKOFF_COEFFICIENT_MAX) {
            backOffCoefficient *= BACKOFF_MULTIPLIER;
        }
        return currentCoeff;
    }
}
