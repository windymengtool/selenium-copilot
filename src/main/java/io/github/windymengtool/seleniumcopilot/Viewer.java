package io.github.windymengtool.seleniumcopilot;

import lombok.Getter;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

@Getter
public class Viewer {
    private ChromeEngine chromeEngine;

    public Viewer(ChromeEngine chromeEngine) {
        this.chromeEngine = chromeEngine;
    }

    public void scrollIntoView(WebElement webElement) {
        if (webElement != null) {
            WebDriver driver = chromeEngine.getDriver();
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", webElement);
        }
    }
}
