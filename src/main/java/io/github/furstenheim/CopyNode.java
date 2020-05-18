package io.github.furstenheim;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CopyNode {
    private static final String[] VOID_ELEMENTS = {
            "AREA", "BASE", "BR", "COL", "COMMAND", "EMBED", "HR", "IMG", "INPUT",
            "KEYGEN", "LINK", "META", "PARAM", "SOURCE", "TRACK", "WBR"
    };
    private static final String[] MEANINGFUL_WHEN_BLANK_ELEMENTS = {
            "A", "TABLE", "THEAD", "TBODY", "TFOOT", "TH", "TD", "IFRAME", "SCRIPT",
            "AUDIO", "VIDEO"
            };

    private static final String[] BLOCK_ELEMENTS = {
            "ADDRESS", "ARTICLE", "ASIDE", "AUDIO", "BLOCKQUOTE", "BODY", "CANVAS",
            "CENTER", "DD", "DIR", "DIV", "DL", "DT", "FIELDSET", "FIGCAPTION", "FIGURE",
            "FOOTER", "FORM", "FRAMESET", "H1", "H2", "H3", "H4", "H5", "H6", "HEADER",
            "HGROUP", "HR", "HTML", "ISINDEX", "LI", "MAIN", "MENU", "NAV", "NOFRAMES",
            "NOSCRIPT", "OL", "OUTPUT", "P", "PRE", "SECTION", "TABLE", "TBODY", "TD",
            "TFOOT", "TH", "THEAD", "TR", "UL"
            };

    private static Set<String> VOID_ELEMENTS_SET = null;
    private static Set<String> MEANINGFUL_WHEN_BLANK_ELEMENTS_SET = null;
    private static Set<String> BLOCK_ELEMENTS_SET = null;

    Element element;
    CopyNode parent;

    public CopyNode (String input) {
        Document document = Jsoup.parse(
                // DOM parsers arrange elements in the <head> and <body>.
                // Wrapping in a custom element ensures elements are reliably arranged in
                // a single element.
                "<x-copydown id=\"copydown-root\">" + input + "</x-copydown>");
        Element root = document.getElementById("copydown-root");
        new WhitespaceCollapser().collapse(root);
        element = root;
    }

    public CopyNode (Element node, CopyNode parent) {
        element = node;
        this.parent = parent;
    }

    public boolean isCode () {
        // TODO cache in property to avoid escalating to root
        return element.nodeName().toLowerCase() == "code" || parent.isCode();
    }

    public static boolean isBlank (Element element) {
        return !isVoid(element) &&
               !isMeaningfulWhenBlank(element) &&
               // TODO check text is the same as textContent in browser
               element.text().matches("/^\\s*$/i") &&
               ! hasVoidElementsSet(element) &&
               ! hasMeaningfulWhenBlankElementsSet(element);
    }
    public FlankingWhiteSpaces flankingWhitespace () {
        String leading = "";
        String trailing = "";
        if (!element.isBlock()) {
            boolean hasLeading = element.text().matches("^\\s");
            boolean hasTrailing = element.text().matches("\\s$");
            // TODO maybe make node property and avoid recomputing
            boolean blankWithSpaces = isBlank(element) && hasLeading && hasTrailing;
            if (hasLeading && !isLeftFlankedByWhitespaces()) {
                leading = " ";
            }
            if (!blankWithSpaces && hasTrailing && !isRightFlankedByWhitespaces()) {
                trailing = " ";
            }
        }
        return new FlankingWhiteSpaces(leading, trailing);
    }

    private boolean isLeftFlankedByWhitespaces () {
        return isChildFlankedByWhitespaces(" $", element.previousElementSibling());
    }
    private boolean isRightFlankedByWhitespaces () {
        return isChildFlankedByWhitespaces("^ ", element.nextElementSibling());
    }
    private boolean isChildFlankedByWhitespaces (String regex, Element sibling) {
        if (sibling == null) {
            return false;
        }
        if (NodeUtils.isNodeType3(sibling)) {
            // TODO fix. Originally sibling.nodeValue
            return sibling.text().matches(regex);
        }
        if (NodeUtils.isNodeType1(sibling)) {
            // TODO fix. Originally textContent
            return sibling.text().matches(regex);
        }
        return false;
    }

    private static boolean hasVoidElementsSet (Element element) {
        for (String tagName: VOID_ELEMENTS_SET) {
            if (element.getElementsByTag(tagName).size() != 0) {
                return true;
            }
        }
        return false;
    }
    public static boolean isVoid (Element element) {
        return getVoidElementsSet().contains(element.tagName());
    }
    private static Set<String> getVoidElementsSet() {
        if (VOID_ELEMENTS_SET != null) {
            return VOID_ELEMENTS_SET;
        }
        VOID_ELEMENTS_SET = new HashSet<>(Arrays.asList(VOID_ELEMENTS));
        return VOID_ELEMENTS_SET;
    }

    private static boolean hasMeaningfulWhenBlankElementsSet (Element element) {
        for (String tagName: MEANINGFUL_WHEN_BLANK_ELEMENTS_SET) {
            if (element.getElementsByTag(tagName).size() != 0) {
                return true;
            }
        }
        return false;
    }
    private static boolean isMeaningfulWhenBlank (Element element) {
        return getMeaningfulWhenBlankElementsSet().contains(element.tagName());
    }
    private static Set<String> getMeaningfulWhenBlankElementsSet() {
        if (MEANINGFUL_WHEN_BLANK_ELEMENTS_SET != null) {
            return MEANINGFUL_WHEN_BLANK_ELEMENTS_SET;
        }
        MEANINGFUL_WHEN_BLANK_ELEMENTS_SET = new HashSet<>(Arrays.asList(MEANINGFUL_WHEN_BLANK_ELEMENTS));
        return MEANINGFUL_WHEN_BLANK_ELEMENTS_SET;
    }

    private boolean hasBlockElementsSet () {
        for (String tagName: BLOCK_ELEMENTS_SET) {
            if (element.getElementsByTag(tagName).size() != 0) {
                return true;
            }
        }
        return false;
    }
    public static boolean isBlock (Element element) {
        return getBlockElementsSet().contains(element.tagName());
    }
    private static Set<String> getBlockElementsSet() {
        if (BLOCK_ELEMENTS_SET != null) {
            return BLOCK_ELEMENTS_SET;
        }
        BLOCK_ELEMENTS_SET = new HashSet<>(Arrays.asList(BLOCK_ELEMENTS));
        return BLOCK_ELEMENTS_SET;
    }

    public static class FlankingWhiteSpaces {
        public String getLeading() {
            return leading;
        }

        public String getTrailing() {
            return trailing;
        }

        private final String leading;
        private final String trailing;

        public FlankingWhiteSpaces(String leading, String trailing) {
            this.leading = leading;
            this.trailing = trailing;
        }
    }
}
