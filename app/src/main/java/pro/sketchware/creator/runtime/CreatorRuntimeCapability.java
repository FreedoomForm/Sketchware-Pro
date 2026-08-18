package pro.sketchware.creator.runtime;

/** Capabilities exposed to Creator projects; none imply a permission grant. */
public enum CreatorRuntimeCapability {
    CAMERA("android.permission.CAMERA"),
    FINE_LOCATION("android.permission.ACCESS_FINE_LOCATION"),
    RECORD_AUDIO("android.permission.RECORD_AUDIO"),
    READ_MEDIA("android.permission.READ_MEDIA_IMAGES"),
    NOTIFICATIONS("android.permission.POST_NOTIFICATIONS");

    private final String permission;
    CreatorRuntimeCapability(String permission) { this.permission = permission; }
    public String getPermission() { return permission; }
}
