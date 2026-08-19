package com.sketchware.ai.ui.chat.sheet;

import android.content.Context;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.ToolRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pro.sketchware.R;

/**
 * A compact, browsable catalog of the tools available to the active AI agent.
 *
 * <p>The agent already receives the full tool schema, but users previously had
 * to know the hidden {@code /tools} command to discover what the assistant can
 * inspect or change. This sheet exposes the same registered catalog in grouped,
 * readable language while keeping tool invocation under the agent's permission
 * policy.
 */
public final class AiToolCatalogSheet {

    private AiToolCatalogSheet() { }

    /** Shows the tool catalog in a scrollable bottom sheet. */
    public static void show(Context context, ToolRegistry registry) {
        if (context == null || registry == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        ScrollView scroll = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(context, 20);
        content.setPadding(horizontal, dp(context, 8), horizontal, dp(context, 28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text(context, R.string.ai_tool_catalog_title, 20, Typeface.BOLD);
        content.addView(title);

        TextView description = text(context, R.string.ai_tool_catalog_description, 14, Typeface.NORMAL);
        description.setTextColor(ContextCompat.getColor(context, R.color.ai_chat_text_secondary));
        LinearLayout.LayoutParams descriptionParams = wrap();
        descriptionParams.topMargin = dp(context, 6);
        content.addView(description, descriptionParams);

        for (Map.Entry<String, List<SketchwareTool>> group : groupByCategory(registry).entrySet()) {
            TextView category = text(context,
                    displayCategory(group.getKey()) + " · " + group.getValue().size(),
                    12, Typeface.BOLD);
            category.setTextColor(ContextCompat.getColor(context, R.color.ai_chat_accent));
            LinearLayout.LayoutParams categoryParams = wrap();
            categoryParams.topMargin = dp(context, 20);
            content.addView(category, categoryParams);

            for (SketchwareTool tool : group.getValue()) {
                TextView row = text(context, tool.name() + "\n" + tool.description(), 14, Typeface.NORMAL);
                row.setTextColor(ContextCompat.getColor(context, R.color.ai_chat_text_primary));
                row.setBackgroundColor(ContextCompat.getColor(context, R.color.ai_chat_surface));
                row.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
                LinearLayout.LayoutParams rowParams = wrap();
                rowParams.topMargin = dp(context, 6);
                content.addView(row, rowParams);
            }
        }

        TextView safety = text(context, R.string.ai_tool_catalog_safety, 13, Typeface.ITALIC);
        safety.setTextColor(ContextCompat.getColor(context, R.color.ai_chat_text_secondary));
        LinearLayout.LayoutParams safetyParams = wrap();
        safetyParams.topMargin = dp(context, 20);
        content.addView(safety, safetyParams);

        dialog.setContentView(scroll);
        dialog.show();
    }

    /** Builds the concise, grouped status text used by the {@code /tools} command. */
    public static String summary(ToolRegistry registry) {
        if (registry == null) return "No AI tools are available.";
        StringBuilder summary = new StringBuilder("AI tools (")
                .append(registry.size()).append("):\n");
        for (Map.Entry<String, List<SketchwareTool>> group : groupByCategory(registry).entrySet()) {
            summary.append("• ").append(displayCategory(group.getKey()))
                    .append(": ").append(group.getValue().size()).append("\n");
        }
        summary.append("\nTap the tools button beside Attach to browse every tool. ")
                .append("Read-only tools run automatically; changes request approval unless Auto-approve is enabled.");
        return summary.toString();
    }

    static Map<String, List<SketchwareTool>> groupByCategory(ToolRegistry registry) {
        Map<String, List<SketchwareTool>> groups = new LinkedHashMap<>();
        if (registry == null) return groups;
        for (SketchwareTool tool : registry.all()) {
            String category = tool.category() == null || tool.category().trim().isEmpty()
                    ? "other" : tool.category().trim();
            List<SketchwareTool> tools = groups.get(category);
            if (tools == null) {
                tools = new ArrayList<>();
                groups.put(category, tools);
            }
            tools.add(tool);
        }
        return groups;
    }

    private static TextView text(Context context, int textRes, int sizeSp, int style) {
        TextView view = new TextView(context);
        view.setText(textRes);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setTextColor(ContextCompat.getColor(context, R.color.ai_chat_text_primary));
        return view;
    }

    private static TextView text(Context context, String value, int sizeSp, int style) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String displayCategory(String category) {
        if (category == null || category.isEmpty()) return "Other";
        return category.substring(0, 1).toUpperCase(Locale.ROOT) + category.substring(1);
    }
}
