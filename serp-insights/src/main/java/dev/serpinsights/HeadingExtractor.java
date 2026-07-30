package dev.serpinsights;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pulls a paper's real section/subsection headings out of its ar5iv-rendered
 * HTML. Deliberately scoped to ar5iv's "ltx_title" heading classes rather
 * than a bare "h1, h2, h3" — some arXiv IDs have no ar5iv rendering and
 * silently redirect back to arxiv.org's plain abs page, whose headings are
 * just sidebar furniture ("Bibliographic and Citation Tools", "Demos", etc.)
 * rather than real section titles; scoping this way means that fallback
 * page yields nothing instead of polluting the results.
 */
@Component
class HeadingExtractor {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 80;
    private static final int MAX_HEADINGS = 25;

    List<String> extract(Document doc) {
        Set<String> headings = new LinkedHashSet<>();
        for (Element heading : doc.select(
                "h2[class*=ltx_title_section], h3[class*=ltx_title_subsection], h4[class*=ltx_title_subsubsection]")) {
            String text = heading.text().trim().replaceAll("\\s+", " ");
            if (text.length() >= MIN_LENGTH && text.length() <= MAX_LENGTH) {
                headings.add(text);
            }
            if (headings.size() >= MAX_HEADINGS) {
                break;
            }
        }
        return List.copyOf(headings);
    }
}
