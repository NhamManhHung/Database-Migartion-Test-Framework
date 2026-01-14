package auto.framework.utils;

import com.opencsv.CSVWriter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScanUtil {

    private WebDriver driver;

    public ScanUtil(WebDriver driver) {
        this.driver = driver;
    }

    private boolean isVisible(WebElement el) {
        try {
            if (!el.isDisplayed()) return false;
            String opacity = el.getCssValue("opacity");
            if ("0".equals(opacity)) return false;
            Point loc = el.getLocation();
            Dimension dim = el.getSize();
            if (dim.getHeight() <= 0 || dim.getWidth() <= 0) return false;
            if (loc.getX() < 0 || loc.getY() < 0) return false;
            return true;
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    private boolean hasMeaningfulText(WebElement el) {
        String text = el.getText();
        if (text == null) return false;
        return true;
    }

    private boolean hasSemanticAttributes(WebElement el) {
        List<String> attrs = Arrays.asList("id", "name", "data-testid", "role", "aria-label");
        for (String attr : attrs) {
            String val = el.getAttribute(attr);
            if (val != null && !val.isEmpty()) return true;
        }
        return false;
    }

    private boolean isInteractive(WebElement el) {
        List<String> tags = Arrays.asList("button", "input", "select", "textarea", "a");
        return tags.contains(el.getTagName().toLowerCase());
    }

    private boolean isTableContent(WebElement el) {
        List<String> tableTags = Arrays.asList("th", "td", "tr");
        return tableTags.contains(el.getTagName().toLowerCase()) && hasMeaningfulText(el);
    }

    private boolean isLayoutOnly(WebElement el) {
        List<String> layoutTags = Arrays.asList("div", "section", "article", "header", "footer", "main", "aside", "nav");
        if (!layoutTags.contains(el.getTagName().toLowerCase())) return false;
        return !(hasSemanticAttributes(el) || hasMeaningfulText(el) || hasMeaningfulChild(el));
    }

    private boolean hasMeaningfulChild(WebElement el) {
        List<WebElement> children = el.findElements(By.xpath("./*"));
        for (WebElement child : children) {
            if (hasSemanticAttributes(child) || hasMeaningfulText(child) || isInteractive(child)) return true;
        }
        return false;
    }

    private String getXPath(WebElement el) {
        String js = "function absoluteXPath(element) {" +
                "var comp, comps = [];" +
                "var parent = null;" +
                "var xpath = '';" +
                "var getPos = function(element) {" +
                "var position = 1, curNode;" +
                "if (element.nodeType == Node.ATTRIBUTE_NODE) {" +
                "return null;" +
                "}" +
                "for (curNode = element.previousSibling; curNode; curNode = curNode.previousSibling) {" +
                "if (curNode.nodeName == element.nodeName) {" +
                "++position;" +
                "}" +
                "}" +
                "return position;" +
                "};" +

                "if (element instanceof Document) {" +
                "return '/';" +
                "}" +

                "for (; element && !(element instanceof Document); element = element.nodeType == Node.ATTRIBUTE_NODE ? element.ownerElement : element.parentNode) {" +
                "comp = comps[comps.length] = {};" +
                "switch (element.nodeType) {" +
                "case Node.TEXT_NODE:" +
                "comp.name = 'text()';" +
                "break;" +
                "case Node.ATTRIBUTE_NODE:" +
                "comp.name = '@' + element.nodeName;" +
                "break;" +
                "case Node.ELEMENT_NODE:" +
                "comp.name = element.nodeName;" +
                "break;" +
                "}" +
                "comp.position = getPos(element);" +
                "}" +

                "for (var i = comps.length - 1; i >= 0; i--) {" +
                "comp = comps[i];" +
                "xpath += '/' + comp.name.toLowerCase();" +
                "if (comp.position !== null && comp.position > 1) {" +
                "xpath += '[' + comp.position + ']';" +
                "}" +
                "}" +

                "return xpath;" +
                "} return absoluteXPath(arguments[0]);";
        return (String) ((JavascriptExecutor) driver).executeScript(js, el);
    }

    private boolean isElementMeaningful(WebElement el) {
        if (!isVisible(el)) return false;
        if (isLayoutOnly(el)) return false;
        return hasSemanticAttributes(el) || isInteractive(el) || hasMeaningfulText(el) || isTableContent(el);
    }

    public void scanAndExportCSV(String csvPath) throws IOException {
        List<WebElement> elements = driver.findElements(By.xpath("//*"));
        Set<String> seenXpaths = new HashSet<>();
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvPath))) {
            // header
            writer.writeNext(new String[]{"tag", "text", "id", "name", "data-testid", "role", "aria-label", "xpath"});
            for (WebElement el : elements) {
                try {
                    if (!isElementMeaningful(el)) continue;
                    String xpath = getXPath(el);
                    if (seenXpaths.contains(xpath)) continue;
                    seenXpaths.add(xpath);
                    String[] row = new String[]{
                            el.getTagName(),
                            el.getText(),
                            el.getAttribute("id"),
                            el.getAttribute("name"),
                            el.getAttribute("data-testid"),
                            el.getAttribute("role"),
                            el.getAttribute("aria-label"),
                            xpath
                    };
                    writer.writeNext(row);
                } catch (StaleElementReferenceException ignored) {
                }
            }
        }
    }

}




