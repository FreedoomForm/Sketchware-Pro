package com.sketchware.ai.tools.build;

import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.tools.UniversalTool;
import com.sketchware.ai.util.SketchwareApi;

/**
 * export_action — universal tool for build operations.
 *
 * <p>Replaces 4 stubs: export_action:export_signed_apk, export_action:export_aab, export_action:export_source_zip, export_action:keystore_create
 *
 * <p><b>AUTO-GENERATED</b> by /home/z/my-project/scripts/gen_universal_tools_pt2.py.
 * Hand-edit is allowed; re-running the generator will overwrite this file.
 */
public final class ExportActionTool extends UniversalTool {

    public ExportActionTool() {
        super("export_action",
                "Export project: build signed APK, build AAB, export source zip, create keystore.",
                "build", false, false,
"export_signed_apk",
                "export_aab",
                "export_source_zip",
                "keystore_create");
    }

    @Override protected void addExtraProperties(JsonObject props) {
        JsonObject p_file_path = new JsonObject();
        p_file_path.addProperty("type", "string");
        p_file_path.addProperty("description", "Output file path.");
        props.add("file_path", p_file_path);
        JsonObject p_keystore_path = new JsonObject();
        p_keystore_path.addProperty("type", "string");
        p_keystore_path.addProperty("description", "(export_signed_apk/aab) Keystore path.");
        props.add("keystore_path", p_keystore_path);
        JsonObject p_keystore_password = new JsonObject();
        p_keystore_password.addProperty("type", "string");
        p_keystore_password.addProperty("description", "(export_signed_apk/aab/keystore_create) Keystore password.");
        props.add("keystore_password", p_keystore_password);
        JsonObject p_key_alias = new JsonObject();
        p_key_alias.addProperty("type", "string");
        p_key_alias.addProperty("description", "(export_signed_apk/aab) Key alias.");
        props.add("key_alias", p_key_alias);
        JsonObject p_key_password = new JsonObject();
        p_key_password.addProperty("type", "string");
        p_key_password.addProperty("description", "(export_signed_apk/aab) Key password.");
        props.add("key_password", p_key_password);
        JsonObject p_keystore_name = new JsonObject();
        p_keystore_name.addProperty("type", "string");
        p_keystore_name.addProperty("description", "(keystore_create) New keystore name.");
        props.add("keystore_name", p_keystore_name);
        JsonObject p_organization = new JsonObject();
        p_organization.addProperty("type", "string");
        p_organization.addProperty("description", "(keystore_create) Organization name.");
        props.add("organization", p_organization);
    }

    @Override
    protected ToolResult dispatch(String action, JsonObject args, SketchwareToolContext ctx) throws Exception {
        switch (action) {
            case "export_signed_apk": {
                String ksPath = optString(args, "keystore_path");
                                String ksPass = optString(args, "keystore_password");
                                String alias = optString(args, "key_alias");
                                String keyPass = optString(args, "key_password");
                                if (ksPath == null || ksPass == null || alias == null || keyPass == null)
                                    return err("keystore_path, keystore_password, key_alias, key_password required");
                                try {
                                    Object exp = SketchwareApi.invokeStatic("mod.jbk.export.ApkExporter", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(exp, "exportSignedApk", ctx.getContext(), ksPath, ksPass, alias, keyPass);
                                    return ok("Started signed APK export.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "export_aab": {
                String ksPath = optString(args, "keystore_path");
                                String ksPass = optString(args, "keystore_password");
                                String alias = optString(args, "key_alias");
                                String keyPass = optString(args, "key_password");
                                if (ksPath == null || ksPass == null || alias == null || keyPass == null)
                                    return err("keystore_path, keystore_password, key_alias, key_password required");
                                try {
                                    Object exp = SketchwareApi.invokeStatic("mod.jbk.export.AabExporter", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(exp, "exportAab", ctx.getContext(), ksPath, ksPass, alias, keyPass);
                                    return ok("Started AAB export.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "export_source_zip": {
                String path = optString(args, "file_path");
                                if (path == null) return err("file_path is required");
                                try {
                                    Object exp = SketchwareApi.invokeStatic("mod.jbk.export.SourceExporter", "getInstance", ctx.getScId());
                                    SketchwareApi.invoke(exp, "exportZip", path);
                                    return ok("Exported source ZIP to " + path + ".");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            case "keystore_create": {
                String name = optString(args, "keystore_name");
                                String ksPass = optString(args, "keystore_password");
                                String org = optString(args, "organization", "SketchwareAI");
                                if (name == null || ksPass == null) return err("keystore_name and keystore_password required");
                                try {
                                    Object ksm = SketchwareApi.invokeStatic("mod.jbk.export.KeystoreManager", "getInstance");
                                    SketchwareApi.invoke(ksm, "createKeystore", name, ksPass, org);
                                    return ok("Created keystore '" + name + "'.");
                                } catch (Throwable t) { return ToolResult.error(t); }
            }
            default:
                return err("Unknown action: " + action);
        }
    }

}
