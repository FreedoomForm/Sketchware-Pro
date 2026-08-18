package pro.sketchware.creator.runtime;

import java.util.Map;

/** Validates requested operations before they can create a revision. */
public final class CreatorOperationValidator {
    private CreatorOperationValidator() { }

    public static CreatorValidationResult validate(CreatorProjectDocument document,
                                                    CreatorProjectOperation operation) {
        if (document == null || operation == null) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.INVALID_PAYLOAD,
                    "document and operation are required");
        }
        if (!document.getProjectId().equals(operation.getProjectId())) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.PROJECT_MISMATCH,
                    "operation projectId does not match the active document");
        }
        if (document.getRevision() != operation.getBaseRevision()) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.STALE_REVISION,
                    "operation baseRevision does not match the current revision");
        }
        Map<String, Object> payload = operation.getPayload();
        switch (operation.getType()) {
            case SCREEN_CREATE:
                return validateScreenCreate(document, payload);
            case WIDGET_ADD:
                return validateWidgetAdd(document, payload);
            case WIDGET_SET_PROPERTY:
                return validatePropertySet(document, payload);
            case ENTRY_CONTROL_UPDATE:
                return validateEntryControlUpdate(payload);
            case REVISION_RESTORE:
                return payload.get("targetRevision") instanceof Number
                        ? CreatorValidationResult.ok()
                        : invalid("targetRevision must be a number");
            default:
                return CreatorValidationResult.error(CreatorValidationResult.Code.UNKNOWN_OPERATION,
                        "unsupported operation type");
        }
    }

    private static CreatorValidationResult validateScreenCreate(CreatorProjectDocument document,
                                                                  Map<String, Object> payload) {
        String screenId = string(payload, "screenId");
        String route = string(payload, "route");
        String rootWidgetId = string(payload, "rootWidgetId");
        if (screenId == null || route == null || rootWidgetId == null) return invalid("screenId, route and rootWidgetId are required");
        if (!route.startsWith("/")) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.INVALID_ROUTE,
                    "screen route must start with '/'");
        }
        if (document.getScreens().containsKey(screenId) || document.getWidgets().containsKey(rootWidgetId)) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.DUPLICATE_ID,
                    "screenId or rootWidgetId already exists");
        }
        for (CreatorScreen screen : document.getScreens().values()) {
            if (route.equals(screen.getRoute())) {
                return CreatorValidationResult.error(CreatorValidationResult.Code.DUPLICATE_ID,
                        "screen route already exists");
            }
        }
        return CreatorValidationResult.ok();
    }

    private static CreatorValidationResult validateWidgetAdd(CreatorProjectDocument document,
                                                               Map<String, Object> payload) {
        String widgetId = string(payload, "widgetId");
        String type = string(payload, "widgetType");
        String parentId = string(payload, "parentId");
        if (widgetId == null || type == null || parentId == null) return invalid("widgetId, widgetType and parentId are required");
        if (document.getWidgets().containsKey(widgetId)) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.DUPLICATE_ID,
                    "widgetId already exists");
        }
        if (!document.getWidgets().containsKey(parentId)) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.MISSING_REFERENCE,
                    "parentId does not exist");
        }
        return CreatorValidationResult.ok();
    }

    private static CreatorValidationResult validatePropertySet(CreatorProjectDocument document,
                                                                 Map<String, Object> payload) {
        String widgetId = string(payload, "widgetId");
        String property = string(payload, "property");
        if (widgetId == null || property == null || !payload.containsKey("value")) {
            return invalid("widgetId, property and value are required");
        }
        if (!document.getWidgets().containsKey(widgetId)) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.MISSING_REFERENCE,
                    "widgetId does not exist");
        }
        return CreatorValidationResult.ok();
    }

    private static CreatorValidationResult validateEntryControlUpdate(Map<String, Object> payload) {
        if (payload.containsKey("recoveryEnabled") || payload.containsKey("shakeRecoveryEnabled")) {
            return CreatorValidationResult.error(CreatorValidationResult.Code.SAFETY_VIOLATION,
                    "project operations cannot disable host recovery");
        }
        Object label = payload.get("label");
        if (label != null && (!(label instanceof String) || ((String) label).trim().isEmpty())) {
            return invalid("entry-control label must be a non-empty string");
        }
        Object placement = payload.get("placement");
        if (placement != null && (!(placement instanceof String)
                || !CreatorEntryControl.isSupportedPlacement((String) placement))) {
            return invalid("entry-control placement is unsupported");
        }
        Object visible = payload.get("visible");
        if (visible != null && !(visible instanceof Boolean)) return invalid("entry-control visible must be boolean");
        return CreatorValidationResult.ok();
    }

    private static CreatorValidationResult invalid(String message) {
        return CreatorValidationResult.error(CreatorValidationResult.Code.INVALID_PAYLOAD, message);
    }

    private static String string(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) return null;
        return (String) value;
    }
}
