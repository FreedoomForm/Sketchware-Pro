package pro.sketchware.creator.runtime;

import android.content.Context;

import a.a.a.ProjectBuilder;
import a.a.a.jC;
import a.a.a.yq;
import mod.hey.studios.compiler.kotlin.KotlinCompilerBridge;
import mod.jbk.build.BuildProgressReceiver;

/**
 * Concrete R3 build runner.  A Creator revision remains immutable in the queue;
 * this adapter intentionally receives it only for provenance and prepares the
 * matching legacy project before delegating to Sketchware's established compiler.
 */
public final class CreatorNativeBuildAdapter implements CreatorNativeBuildQueue.BuildRunner {
    /** Converts/synchronizes a pinned Creator document into legacy project files. */
    public interface LegacyProjectPreparer {
        void prepare(CreatorProjectDocument pinnedRevision, yq projectSession) throws Exception;
    }

    private final Context context;
    private final String scId;
    private final LegacyProjectPreparer preparer;
    private final BuildProgressReceiver progressReceiver;

    public CreatorNativeBuildAdapter(Context context, String scId,
                                    LegacyProjectPreparer preparer,
                                    BuildProgressReceiver progressReceiver) {
        if (context == null) throw new IllegalArgumentException("context");
        if (scId == null || scId.trim().isEmpty()) throw new IllegalArgumentException("scId");
        if (preparer == null) throw new IllegalArgumentException("preparer");
        this.context = context.getApplicationContext();
        this.scId = scId;
        this.preparer = preparer;
        this.progressReceiver = progressReceiver == null ? (progress, step) -> { } : progressReceiver;
    }

    @Override
    public void build(CreatorProjectDocument pinnedRevision) throws Exception {
        if (!scId.equals(pinnedRevision.getProjectId())) {
            throw new IllegalStateException("Pinned revision belongs to a different legacy project");
        }
        yq projectSession = new yq(context, scId);
        // The preparer is the only mutation boundary. It receives a revision snapshot,
        // never the live editor document, so the compiler source is traceable.
        preparer.prepare(pinnedRevision, projectSession);

        ProjectBuilder builder = new ProjectBuilder(progressReceiver, context, projectSession);
        // Keep this order aligned with DesignActivity's supported build path:
        // analyze project dependencies, generate legacy source/resources, then compile.
        var fileManager = jC.b(scId);
        var dataManager = jC.a(scId);
        var libraryManager = jC.c(scId);
        projectSession.a(libraryManager, fileManager, dataManager);
        builder.buildBuiltInLibraryInformation();
        projectSession.b(fileManager, dataManager, libraryManager, builder.getBuiltInLibraryManager());
        projectSession.f();
        projectSession.e();
        progressReceiver.onProgress("Creator Runtime revision " + pinnedRevision.getRevision() + ": compiling resources", 8);
        builder.maybeExtractAapt2();
        builder.compileResources();
        progressReceiver.onProgress("Creator Runtime revision " + pinnedRevision.getRevision() + ": compiling code", 13);
        builder.generateViewBinding();
        try {
            KotlinCompilerBridge.compileKotlinCodeIfPossible(progressReceiver, builder);
        } catch (Throwable error) {
            if (error instanceof Exception) throw (Exception) error;
            throw new IllegalStateException("Kotlin compiler bridge failed", error);
        }
        builder.compileJavaCode();
        builder.createDexFilesFromClasses();
        builder.getDexFilesReady();
        progressReceiver.onProgress("Creator Runtime revision " + pinnedRevision.getRevision() + ": packaging", 19);
        builder.buildApk();
        builder.signDebugApk();
    }
}
