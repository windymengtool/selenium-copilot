package io.github.windymengtool.seleniumcopilot;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Getter
public class Waiter {
    private ChromeEngine chromeEngine;
    private String logPrefix = getDefaultLogPrefix();
    private Long maxMs;
    private Long intervalMs;
    private Consumer<Waiter> consumerBeforeWait = getDefaultBeforeWaitConsumer();
    private Consumer<Waiter> consumerAfterWait = getDefaultAfterWaitConsumer();

    public Waiter(ChromeEngine chromeEngine) {
        this.chromeEngine = chromeEngine;
    }

    public Waiter maxMs(Long maxMs) {
        this.maxMs = maxMs;
        return this;
    }

    public Waiter intervalMs(Long intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    public Waiter consumerBeforeWait(Consumer<Waiter> consumerBeforeWait) {
        this.consumerBeforeWait = consumerBeforeWait;
        return this;
    }

    public Waiter consumerAfterWait(Consumer<Waiter> consumerAfterWait) {
        this.consumerAfterWait = consumerAfterWait;
        return this;
    }

    public boolean until(BooleanSupplier booleanSupplier) {
        maxMs = maxMs == null || maxMs < 10000 ? 10000 : maxMs;
        intervalMs = intervalMs == null || intervalMs < 1000 ? 1000 : intervalMs;
        Assert.isTrue(maxMs > intervalMs, "maxMs must be greater than intervalMs");
        boolean result = false;
        TimeInterval timer = DateUtil.timer();
        Optional.ofNullable(consumerBeforeWait).ifPresent(consumer -> consumer.accept(this));
        do {
            try {
                boolean asBoolean = booleanSupplier.getAsBoolean(); // 如果想打印异常日志,用户需要自己在booleanSupplier中打印
                if (asBoolean) {
                    result = true;
                    break;
                }
                ThreadUtil.safeSleep(intervalMs);
            } catch (Exception e) {
                // ignore
            }
        } while (NumberUtil.compare(timer.intervalMs(), maxMs) <= 0);
        Optional.ofNullable(consumerAfterWait).ifPresent(consumer -> consumer.accept(this));
        return result;
    }

    private String getDefaultLogPrefix() {
        return StrUtil.format("{}-{}", getClass().getSimpleName().toUpperCase(), Assistant.nanoIdUpperCase());
    }

    private Consumer<Waiter> getDefaultBeforeWaitConsumer() {
        return waiter -> {
            System.out.println(StrUtil.format("[{}]-等待开始...", waiter.getLogPrefix()));
        };
    }

    private Consumer<Waiter> getDefaultAfterWaitConsumer() {
        return waiter -> {
            System.out.println(StrUtil.format("[{}]-等待结束", waiter.getLogPrefix()));
        };
    }
}