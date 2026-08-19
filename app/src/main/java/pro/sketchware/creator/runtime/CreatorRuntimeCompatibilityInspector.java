package pro.sketchware.creator.runtime;

/** Builds a transparent compatibility report for the active Project IR document. */
public final class CreatorRuntimeCompatibilityInspector {
    private CreatorRuntimeCompatibilityInspector() { }

    public static CreatorCompatibilityReport inspect(CreatorProjectDocument document) {
        CreatorCompatibilityReport report = new CreatorCompatibilityReport();
        if (document == null) {
            report.add("document", "project", CreatorCompatibilityTier.R0_UNSUPPORTED,
                    "No project document is available for inspection.");
            return report;
        }
        report.add(document.getEntryScreenId() == null ? "entry" : document.getEntryScreenId(), "screen",
                CreatorCompatibilityTier.R1_RUNTIME_NATIVE, "Entry screen renders in Creator Runtime.");
        report.add("entry_control", "host control", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                "Visible entry control is runtime-native; shake recovery remains host-owned.");
        for (CreatorWidget widget : document.getWidgets().values()) {
            CreatorCompatibilityTier tier = CreatorRuntimeWidgetCatalog.isRuntimeNative(widget.getType())
                    ? CreatorCompatibilityTier.R1_RUNTIME_NATIVE : CreatorCompatibilityTier.R0_UNSUPPORTED;
            String message = tier == CreatorCompatibilityTier.R1_RUNTIME_NATIVE
                    ? "Renders directly in the live runtime."
                    : "No Creator Runtime renderer is registered; execution is blocked rather than falling back.";
            report.add(widget.getId(), widget.getType(), tier, message);
        }
        return report;
    }
}
