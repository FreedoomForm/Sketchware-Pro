package pro.sketchware.creator;

import static com.google.common.truth.Truth.assertThat;

import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import mod.hey.studios.project.ProjectSettings;

@RunWith(AndroidJUnit4.class)
public class ProjectSettingsRuntimeRegressionTest {
    private static final String SC_ID = "creator_settings_npe_regression";
    private File configFile;

    @Before
    public void setUp() throws Exception {
        File projectDirectory = new File(Environment.getExternalStorageDirectory(),
                ".sketchware/data/" + SC_ID);
        if (!projectDirectory.exists()) assertThat(projectDirectory.mkdirs()).isTrue();
        configFile = new File(projectDirectory, "project_config");
        try (FileOutputStream output = new FileOutputStream(configFile)) {
            output.write("null".getBytes(StandardCharsets.UTF_8));
        }
    }

    @After
    public void tearDown() {
        if (configFile != null) configFile.delete();
        File parent = configFile == null ? null : configFile.getParentFile();
        if (parent != null) parent.delete();
    }

    @Test
    public void nullProjectConfigIsRecoveredBeforeSetValue() {
        ProjectSettings settings = new ProjectSettings(SC_ID);
        settings.setValue(ProjectSettings.SETTING_NEW_XML_COMMAND,
                ProjectSettings.SETTING_GENERIC_VALUE_TRUE);
        assertThat(settings.getValue(ProjectSettings.SETTING_NEW_XML_COMMAND, "false"))
                .isEqualTo(ProjectSettings.SETTING_GENERIC_VALUE_TRUE);
    }
}
