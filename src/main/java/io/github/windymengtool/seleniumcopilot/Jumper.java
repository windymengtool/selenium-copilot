package io.github.windymengtool.seleniumcopilot;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
public class Jumper {
    private ChromeEngine chromeEngine;
    private String logPrefix = getDefaultLogPrefix();
    private Boolean throwEx = true;
    private String url;
    private Long maxMs;
    private Consumer<Jumper> consumerBeforeJump;
    private Consumer<Jumper> consumerAfterJump = getDefaultAfterJumpConsumer();
    private Consumer<Jumper> consumerExceptionally;
    private Consumer<Jumper> consumerIntervalLog = getDefaultIntervalLogConsumer();
    //
    private Boolean jumping = false;
    private TimeInterval timer;

    /**
     * the only constructor
     */
    public Jumper(ChromeEngine chromeEngine) {
        this.chromeEngine = chromeEngine;
    }

    public Jumper logPrefix(String logPrefix) {
        this.logPrefix = logPrefix;
        return this;
    }

    public Jumper throwEx(Boolean throwEx) {
        this.throwEx = throwEx;
        return this;
    }

    public Jumper url(String url) {
        this.url = url;
        return this;
    }

    public Jumper maxTimeoutMs(Long maxTimeoutMs) {
        this.maxMs = maxTimeoutMs;
        return this;
    }

    public Jumper consumerBeforeJump(Consumer<Jumper> consumerBeforeJump) {
        this.consumerBeforeJump = consumerBeforeJump;
        return this;
    }

    public Jumper consumerAfterJump(Consumer<Jumper> consumerAfterJump) {
        this.consumerAfterJump = consumerAfterJump;
        return this;
    }

    public Jumper consumerExceptionally(Consumer<Jumper> consumerExceptionally) {
        this.consumerExceptionally = consumerExceptionally;
        return this;
    }

    public Jumper consumerIntervalLog(Consumer<Jumper> consumerIntervalLog) {
        this.consumerIntervalLog = consumerIntervalLog;
        return this;
    }

    public void jump() {
        if (jumping) throw new RuntimeException("Jumper is jumping");
        try {
            // 1.0
            jumping = true;
            this.timer = DateUtil.timer();
            formatVerify();
            // 1.1
            Optional.ofNullable(consumerBeforeJump).ifPresent(f -> f.accept(this));
            // 1.2
            CompletableFuture.runAsync(() -> {
                while (jumping) {
                    Optional.ofNullable(consumerIntervalLog).ifPresent(f -> f.accept(this));
                    ThreadUtil.safeSleep(1000L);
                }
            });
            // 1.3
            this.maxMs = this.maxMs == null || this.maxMs < 10000 ? 10000 : this.maxMs;
            WebDriver driver = this.chromeEngine.getDriver();
            driver.manage().timeouts().pageLoadTimeout(Duration.ofMillis(this.maxMs));
            driver.get(this.url);
            Optional.ofNullable(this.consumerAfterJump).ifPresent(f -> f.accept(this));
        } catch (Exception e) {
            // 1.4
            Assistant.errPrintFlush(StrUtil.format("[{}]-jump failed @ {}ms", this.logPrefix, getPrettyMs(this)));
            Optional.ofNullable(this.consumerExceptionally).ifPresent(f -> f.accept(this));
            if (BooleanUtil.isTrue(this.throwEx)) throw new RuntimeException(e);
        } finally {
            jumping = false;
            this.timer = null;
        }
    }

    private void formatVerify() {
        if (!Validator.isUrl(this.url)) {
            throw new RuntimeException(StrUtil.format("[{}]-url is not valid which is [{}]", this.logPrefix, this.url));
        }
    }

    private String getDefaultLogPrefix() {
        return StrUtil.format("{}-{}", getClass().getSimpleName().toUpperCase(), Assistant.nanoIdUpperCase());
    }

    private Consumer<Jumper> getDefaultIntervalLogConsumer() {
        return jumper -> {
            System.out.println(StrUtil.format("[{}]-jumping[{}]-[{}]", jumper.getLogPrefix(), getPrettyMs(jumper), getPrettyUrl(jumper)));
            ThreadUtil.safeSleep(500L);
        };
    }

    private Consumer<Jumper> getDefaultAfterJumpConsumer() {
        return jumper -> {
            System.out.println(StrUtil.format("[{}]-jumped done [{}]-[{}]", jumper.getLogPrefix(), getPrettyMs(jumper), getPrettyUrl(jumper)));
        };
    }

    private String getPrettyMs(Jumper jumper) {
        String ms = Convert.toStr(jumper.timer.intervalMs());
        String msStr = StrUtil.fillBefore(ms, '0', 6);
        return msStr;
    }

    private static String getPrettyUrl(Jumper jumper) {
        int maxLen = 500;
        return jumper.getUrl().length() > maxLen ? jumper.getUrl().substring(0, maxLen) + "..." : jumper.getUrl();
    }
}
