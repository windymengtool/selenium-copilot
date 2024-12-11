package io.github.windymengtool.seleniumcopilot;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Getter
public class Finder {
    private ChromeEngine chromeEngine;
    private String logPrefix = getDefaultLogPrefix();
    private String javaScript;
    private Long maxMs;
    private Long intervalMs;
    /**
     * 判断查找成功的标准:至少找到多少个元素,最小值为1
     */
    private Integer atLeast = 1;
    /**
     * 达到至少条件时是否立即返回,默认为true;如果为false,则会等待maxMs时间
     */
    private Boolean immediateReturnWhenAtLeast = true;
    private boolean throwEx = true;
    private Consumer<Finder> consumerBeforeLoop = getDefaultBeforeLoopConsumer();
    private Consumer<Finder> consumerIntervalLog = getDefaultIntervalLogConsumer();
    private Consumer<Finder> consumerAfterLoop = getDefaultAfterLoopConsumer();
    private Consumer<Finder> consumerExceptionally;
    //
    private Boolean finding = false;
    private By locator;
    private TimeInterval timer;
    private ConditionEnum finalConditionEnum;
    //
    List<WebElement> resultElements;


    /**
     * the only constructor
     */
    public Finder(ChromeEngine chromeEngine) {
        this.chromeEngine = chromeEngine;
    }

    public Finder maxMs(Long maxMs) {
        this.maxMs = maxMs;
        return this;
    }

    public Finder intervalMs(Long intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    public Finder atLeast(Integer atLeast) {
        this.atLeast = atLeast;
        return this;
    }

    public Finder immediateReturnWhenAtLeast(Boolean immediateReturnWhenAtLeast) {
        this.immediateReturnWhenAtLeast = immediateReturnWhenAtLeast;
        return this;
    }

    public Finder consumerBeforeLoop(Consumer<Finder> consumerBeforeLoop) {
        this.consumerBeforeLoop = consumerBeforeLoop;
        return this;
    }

    public Finder consumerIntervalLog(Consumer<Finder> consumerIntervalLog) {
        this.consumerIntervalLog = consumerIntervalLog;
        return this;
    }

    public Finder consumerAfterLoop(Consumer<Finder> consumerAfterLoop) {
        this.consumerAfterLoop = consumerAfterLoop;
        return this;
    }

    public Finder consumerExceptionally(Consumer<Finder> consumerExceptionally) {
        this.consumerExceptionally = consumerExceptionally;
        return this;
    }

    public Finder throwEx(Boolean throwEx) {
        this.throwEx = throwEx;
        return this;
    }

    public Finder byLocator(By locator) {
        this.locator = locator;
        this.finalConditionEnum = ConditionEnum.LOCATOR;
        return this;
    }

    public Finder byJavaScript(String javaScript) {
        this.javaScript = javaScript;
        this.finalConditionEnum = ConditionEnum.JS;
        return this;
    }

    private String getDefaultLogPrefix() {
        return StrUtil.format("{}-{}", getClass().getSimpleName().toUpperCase(), Assistant.nanoIdUpperCase());
    }

    private Consumer<Finder> getDefaultBeforeLoopConsumer() {
        return finder -> {
            String target = null;
            if (finalConditionEnum.equals(ConditionEnum.LOCATOR)) {
                target = locator.toString();
            } else if (finalConditionEnum.equals(ConditionEnum.JS)) {
                int maxLen = 100;
                target = javaScript != null && javaScript.length() > maxLen ? javaScript.substring(0, maxLen) + "......" : javaScript;
            } else {
                throw new RuntimeException(StrUtil.format("[{}]-not supported condition type:[{}]", finder.getLogPrefix(), finalConditionEnum));
            }
            System.out.println(StrUtil.format("[{}]-元素开始查找:[{}]", finder.getLogPrefix(), Optional.ofNullable(target).orElse("undefined")));
        };
    }

    private Consumer<Finder> getDefaultIntervalLogConsumer() {
        return finder -> {
            System.out.println(StrUtil.format("[{}]-元素查找中...", finder.getLogPrefix()));
            ThreadUtil.safeSleep(500L);
        };
    }

    private Consumer<Finder> getDefaultAfterLoopConsumer() {
        return finder -> {
            String count = Optional.ofNullable(finder).map(Finder::getResultElements).map(List::size).map(Convert::toStr).orElse("N/A");
            String duration = Optional.ofNullable(finder).map(Finder::getTimer).map(TimeInterval::intervalMs).map(DateUtil::formatBetween).orElse("N/A");
            System.out.println(StrUtil.format("[{}]-元素查找结束,共找到[{}]个元素,耗时[{}]", finder.getLogPrefix(), count, duration));
        };
    }

    public WebElement findOne() {
        return loop(FindTypeEnum.ONE).get(0);
    }

    public List<WebElement> findList() {
        return loop(FindTypeEnum.LIST);
    }

    private List<WebElement> loop(FindTypeEnum findTypeEnum) {
        // System.out.println("===================================loop ing ======================================");
        if (finding) {
            throw new RuntimeException(StrUtil.format("[{}]-cannot call repeatedly:[{}]", this.logPrefix, Thread.currentThread().getStackTrace()[2].getMethodName()));
        }
        List<WebElement> resultElements = new ArrayList<>();
        maxMs = maxMs == null || maxMs < 0 ? 10 * 1000L : maxMs;
        intervalMs = intervalMs == null || intervalMs < 0 ? 1000L : intervalMs;
        atLeast = atLeast == null || atLeast < 1 ? 1 : atLeast;
        try {
            // 1.1
            finding = true;
            Assert.isTrue(intervalMs < maxMs, "intervalMs must be less than maxMs");
            Assert.notNull(finalConditionEnum, "not specified search condition type");
            Assert.notNull(immediateReturnWhenAtLeast, "not specified immediateReturnWhenAtLeast");
            Assert.isTrue(Arrays.stream(ConditionEnum.values()).anyMatch(f -> f.equals(finalConditionEnum)), "not supported condition type");
            Assert.isTrue(Arrays.stream(FindTypeEnum.values()).anyMatch(f -> f.equals(findTypeEnum)), "not supported find type");
            //
            // 1.1.1 优化JS脚本
            if (finalConditionEnum == ConditionEnum.JS) {
                this.javaScript = StrUtil.addPrefixIfNot(this.javaScript, "return ");
            }
            //
            this.timer = DateUtil.timer();
            Optional.ofNullable(consumerBeforeLoop).ifPresent(f -> f.accept(this));
            // 1.2
            // 1.3 在规定的最大时间被查找元素,如果设置了`达到至少条件时立即返回`,那么在找到至少条件时立即返回,否则等待maxMs时间结束
            WebDriver driver = chromeEngine.getDriver();
            while (NumberUtil.compare(timer.intervalMs(), maxMs) <= 0) {
                Optional.ofNullable(consumerIntervalLog).ifPresent(f -> f.accept(this));
                //
                if (finalConditionEnum.equals(ConditionEnum.LOCATOR)) {
                    if (findTypeEnum.equals(FindTypeEnum.ONE)) {
                        resultElements = Arrays.asList(driver.findElement(locator));
                    } else if (findTypeEnum.equals(FindTypeEnum.LIST)) {
                        resultElements = driver.findElements(locator);
                    }
                } else if (finalConditionEnum.equals(ConditionEnum.JS)) {
                    JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
                    if (findTypeEnum.equals(FindTypeEnum.ONE)) {
                        WebElement webElement = (WebElement) jsExecutor.executeScript(this.javaScript);
                        resultElements = Arrays.asList(webElement);
                    } else if (findTypeEnum.equals(FindTypeEnum.LIST)) {
                        resultElements = (List<WebElement>) jsExecutor.executeScript(this.javaScript);
                    }
                }
                // 1.3.1 是否设置了立即返回
                if (BooleanUtil.isTrue(immediateReturnWhenAtLeast) && resultElements.size() >= atLeast) {
                    break;
                } else {
                    ThreadUtil.safeSleep(intervalMs);
                }
            }
            this.resultElements = resultElements;
            Optional.ofNullable(consumerAfterLoop).ifPresent(f -> f.accept(this));
            return resultElements;
        } catch (Exception e) {
            Assistant.errPrintFlush(StrUtil.format("[{}]-loop failed @ {}ms", this.logPrefix, this.timer.intervalMs()));
            Optional.ofNullable(this.consumerExceptionally).ifPresent(f -> f.accept(this));
            if (BooleanUtil.isTrue(this.throwEx)) throw new RuntimeException(e);
        } finally {
            finding = false;
            timer = null;
            // System.out.println("===================================loop out ======================================");
        }
        return new ArrayList<>();
    }

    @AllArgsConstructor
    public enum ConditionEnum {
        LOCATOR(1, "通过定位器", "by locator"),
        JS(2, "通过JS脚本", "by js script"),
        ;
        private Integer code;
        private String zhLabel;
        private String enLabel;
    }

    @AllArgsConstructor
    public enum FindTypeEnum {
        ONE(1, "查找一个元素", "find one element"),
        LIST(2, "查找多个元素", "find list elements"),
        ;
        private Integer code;
        private String zhLabel;
        private String enLabel;
    }

}