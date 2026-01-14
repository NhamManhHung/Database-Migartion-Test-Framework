package auto.framework;

import auto.framework.utils.ScanUtil;
import com.opencsv.CSVWriter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MainScan {
    public static void main(String[] args) throws IOException {
        WebDriver driver = new ChromeDriver();
        driver.get("https://semantic-ui.com/examples/login.html");

        ScanUtil scanner = new ScanUtil(driver);
        scanner.scanAndExportCSV("output");

        driver.quit();
    }
}


