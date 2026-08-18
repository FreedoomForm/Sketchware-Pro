package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Host-side permission state machine. Android UI adapters call {@link #resolve}
 * after a user decision; plugins cannot mark a capability as granted directly.
 */
public final class CreatorRuntimePermissionBridge {
    public enum Outcome { GRANTED, DENIED, REQUEST_REQUIRED, UNSUPPORTED, NO_HOST }
    private final Set<CreatorRuntimeCapability> supported;
    private final Set<CreatorRuntimeCapability> granted = EnumSet.noneOf(CreatorRuntimeCapability.class);

    public CreatorRuntimePermissionBridge(Set<CreatorRuntimeCapability> supported) {
        this.supported = supported == null ? Collections.<CreatorRuntimeCapability>emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(supported));
    }

    public Outcome check(CreatorRuntimeCapability capability, boolean hasHost) {
        if (capability == null || !supported.contains(capability)) return Outcome.UNSUPPORTED;
        if (!hasHost) return Outcome.NO_HOST;
        return granted.contains(capability) ? Outcome.GRANTED : Outcome.REQUEST_REQUIRED;
    }

    public Outcome resolve(CreatorRuntimeCapability capability, boolean userGranted) {
        if (capability == null || !supported.contains(capability)) return Outcome.UNSUPPORTED;
        if (userGranted) {
            granted.add(capability);
            return Outcome.GRANTED;
        }
        granted.remove(capability);
        return Outcome.DENIED;
    }
}
