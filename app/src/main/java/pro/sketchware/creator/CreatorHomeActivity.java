package pro.sketchware.creator;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.besome.sketch.design.DesignActivity;

import pro.sketchware.creator.runtime.CreatorLegacyProjectBridge;
import pro.sketchware.creator.runtime.CreatorProjectDocument;
import pro.sketchware.creator.runtime.CreatorRuntimeSession;

/**
 * Compatibility entry point retained for old internal intents.
 * Creator Runtime now opens the original DesignActivity main project directly;
 * this class must never render a separate Home surface.
 */
public final class CreatorHomeActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        CreatorProjectDocument document = CreatorRuntimeSession.get(this).getDocument();
        String legacyScId = CreatorLegacyProjectBridge.ensureLegacyProject(this, document);
        startActivity(new Intent(this, DesignActivity.class)
                .putExtra("sc_id", legacyScId)
                .putExtra("creator_runtime_project_id", document.getProjectId()));
        finish();
    }
}
