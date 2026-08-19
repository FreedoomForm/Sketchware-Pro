package pro.sketchware.creator.runtime;

/** Immutable route and root-widget association for a Creator Runtime screen. */
public final class CreatorScreen {
    private final String id;
    private final String route;
    private final String rootWidgetId;

    public CreatorScreen(String id, String route, String rootWidgetId) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("id");
        if (route == null || route.trim().isEmpty()) throw new IllegalArgumentException("route");
        if (rootWidgetId == null || rootWidgetId.trim().isEmpty()) throw new IllegalArgumentException("rootWidgetId");
        this.id = id;
        this.route = route;
        this.rootWidgetId = rootWidgetId;
    }

    public String getId() { return id; }
    public String getRoute() { return route; }
    public String getRootWidgetId() { return rootWidgetId; }
}
