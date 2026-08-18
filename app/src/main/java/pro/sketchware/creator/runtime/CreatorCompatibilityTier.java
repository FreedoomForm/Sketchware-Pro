package pro.sketchware.creator.runtime;

/** Execution tier assigned during Sketchware-to-Creator Runtime migration. */
public enum CreatorCompatibilityTier {
    R1_RUNTIME_NATIVE,
    R2_RUNTIME_PLUGIN,
    R3_NATIVE_FALLBACK,
    R0_UNSUPPORTED
}
