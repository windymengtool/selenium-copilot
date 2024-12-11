package io.github.windymengtool.seleniumcopilot;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Selenium Chrome Engine
 */
@Getter
// @Setter // 禁用
public class ChromeEngine {
    private ChromeProperties chromeProperties;
    private ChromeOptions chromeOptions;
    private WebDriver driver;

    public ChromeEngine(ChromeProperties chromeProperties) {
        this.chromeProperties = chromeProperties;
        this.chromeOptions = buildChromeOptions(chromeProperties);
        try {
            this.driver = new ChromeDriver(chromeOptions);
        } catch (Exception e) {
            if (e instanceof SessionNotCreatedException) Assistant.errPrintWrapper("Please check if there is already a Google Chrome browser running, please close it and try again\r\n请检查是否已经有Google Chrome浏览器运行，请关闭后重试");
            throw new RuntimeException(e);
        }
    }

    private ChromeOptions buildChromeOptions(ChromeProperties chromeProperties) {
        ChromeOptions chromeOptions = new ChromeOptions();
        System.setProperty("webdriver.chrome.driver", chromeProperties.getDriver());// system environment variable: specify the driving path
        // arguments
        Set<String> arguments = new HashSet<>();
        //
        if (CollectionUtil.isNotEmpty(chromeProperties.getArguments())) chromeProperties.getArguments().forEach(arguments::add);
        if (BooleanUtil.isTrue(chromeProperties.getUseUserDataCopy())) {
            arguments.add(StrUtil.format("--user-data-dir={}", copyUserData(chromeProperties.getUserData())));
        }
        // crx
        List<File> crxSetList = checkCrx(chromeProperties.getCrxList());
        //
        chromeOptions.addArguments(new ArrayList<>(arguments));
        if (CollectionUtil.isNotEmpty(crxSetList)) chromeOptions.addExtensions(crxSetList);
        return chromeOptions;
    }

    private String copyUserData(String userData) {
        Assert.isTrue(StrUtil.isNotBlank(userData) && FileUtil.exist(userData) && FileUtil.isDirectory(userData), "`userData` must be a valid directory");
        String path = FileUtil.getAbsolutePath(FileUtil.file(FileUtil.getParent(userData, 1), "user_data_copy"));
        if (!(FileUtil.exist(path) && FileUtil.isDirectory(path))) {
            FileUtil.mkdir(path);
            // copy the file in full, if the google browser is not turned off here, an error will be reported
            try {
                FileUtil.copyContent(FileUtil.file(userData), FileUtil.file(path), true);
            } catch (IORuntimeException e) {
                if (StrUtil.contains(e.getMessage(), "另一个程序正在使用此文件，进程无法访问")) {
                    Assistant.errPrintWrapper("User data directory copy failed, please close Google Chrome and try again\r\n用户数据目录复制失败，请关闭Google Chrome后重试");
                }
                throw new RuntimeException(e);
            }
        }
        return path;
    }

    private List<File> checkCrx(List<String> crxList) {
        List<String> list = crxList.stream().filter(f -> {
            boolean exist = FileUtil.exist(f);
            boolean isCrx = StrUtil.isNotBlank(FileUtil.extName(f)) && StrUtil.equalsIgnoreCase(FileUtil.extName(f), "crx");
            return exist && isCrx;
        }).collect(Collectors.toList());
        return ListUtil.toList(list.stream().map(FileUtil::file).collect(Collectors.toList()));
    }

    public Jumper jumper() {
        return new Jumper(this);
    }

    public Finder finder() {
        return new Finder(this);
    }

    public Viewer viewer() {
        return new Viewer(this);
    }

    public Waiter waiter() {
        return new Waiter(this);
    }
}
