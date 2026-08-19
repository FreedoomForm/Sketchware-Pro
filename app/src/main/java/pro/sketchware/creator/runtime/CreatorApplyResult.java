package pro.sketchware.creator.runtime;

/** Final outcome returned to both AI and human-editor callers. */
public final class CreatorApplyResult {
    private final boolean applied;
    private final boolean replayed;
    private final CreatorValidationResult validation;
    private final CreatorProjectDocument document;

    private CreatorApplyResult(boolean applied, boolean replayed, CreatorValidationResult validation,
                               CreatorProjectDocument document) {
        this.applied = applied;
        this.replayed = replayed;
        this.validation = validation;
        this.document = document;
    }

    public static CreatorApplyResult rejected(CreatorValidationResult validation, CreatorProjectDocument document) {
        return new CreatorApplyResult(false, false, validation, document);
    }

    public static CreatorApplyResult applied(CreatorProjectDocument document) {
        return new CreatorApplyResult(true, false, CreatorValidationResult.ok(), document);
    }

    public CreatorApplyResult asReplayed() {
        return new CreatorApplyResult(applied, true, validation, document);
    }

    public boolean isApplied() { return applied; }
    public boolean isReplayed() { return replayed; }
    public CreatorValidationResult getValidation() { return validation; }
    public CreatorProjectDocument getDocument() { return document; }
}
