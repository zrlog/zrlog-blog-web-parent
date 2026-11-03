package com.zrlog.test.web.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zrlog.blog.web.util.OutlineUtil;
import com.zrlog.common.vo.Outline;
import java.util.List;
import org.junit.Test;

public class OutlineUtilTest {

    @Test
    public void shouldExtractNestedOutline() {
        String html = "<h1>Title</h1><p>x</p><h2>Section</h2><h3>Child</h3><h2>Section2</h2>";
        List<Outline> outlines = OutlineUtil.extractOutline(html);
        assertEquals(1, outlines.size());
        assertEquals("Title", outlines.get(0).getText());
        assertEquals(2, outlines.get(0).getChildren().size());
        assertEquals("Section", outlines.get(0).getChildren().get(0).getText());
        assertEquals("Child", outlines.get(0).getChildren().get(0).getChildren().get(0).getText());
    }

    @Test
    public void shouldExtractFlatOutlineWhenHeadingLevelsDoNotNest() {
        String html = "<h2>Section</h2><h2>Next</h2><h1>Top</h1>";

        List<Outline> outlines = OutlineUtil.extractOutline(html);

        assertEquals(3, outlines.size());
        assertEquals("Section", outlines.get(0).getText());
        assertEquals("Next", outlines.get(1).getText());
        assertEquals("Top", outlines.get(2).getText());
    }

    @Test
    public void shouldReturnEmptyOutlineWhenHtmlHasNoHeading() {
        assertTrue(OutlineUtil.extractOutline("<p>content</p>").isEmpty());
    }

    @Test
    public void shouldBuildTocHtml() {
        Outline root = new Outline();
        root.setText("Root");
        Outline child = new Outline();
        child.setText("Child");
        root.getChildren().add(child);

        String toc = OutlineUtil.buildTocHtml(List.of(root), "");
        assertTrue(toc.startsWith("<ul>"));
        assertTrue(toc.contains("<a href='#Root'>Root</a>"));
        assertTrue(toc.contains("<a href='#Child'>Child</a>"));
    }

    @Test
    public void shouldBuildEmptyTocHtml() {
        assertEquals("<ul></ul>", OutlineUtil.buildTocHtml(List.of(), ""));
    }
}
