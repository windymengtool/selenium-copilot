package io.github.windymengtool.seleniumcopilot;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ChromeProperties {
    private String driver;
    private String userData;
    private String download;
    private List<String> crxList;
    @Builder.Default
    private Boolean useUserDataCopy = true;
    @Builder.Default
    private List<String> arguments = new ArrayList<String>() {{
        add("--no-sandbox"); // Solve the error that does not exist in the devtoolsActivePort file
        add("--disable-dev-shm-usage"); // Solve the error caused by the /dev/shm partition being too small
        add("--disable-gpu"); // If you don't use the GPU plan
        add("--verbose");
        add("--disable-infobars"); // Disable infobars
        add("--disable-popup-blocking"); // Disable popup blocking
        add("--remote-allow-origins=*"); // Allow the same source policy (necessary)
        add("--start-maximized"); // Maximum window
    }};
}
