package com.sketchware.ai.tools.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

/**
 * component_add - add a built-in component instance via reflection.
 */
public final class ComponentAddTool implements SketchwareTool {

    @Override public String name() { return "component_add"; }
    @Override public String category() { return "component"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Add a built-in component instance. Component types: Intent, SharedPreferences, "
                + "FilePicker, Calendar, Vibrator, TimerTask, Dialog, MediaPlayer, SoundPool, "
                + "ObjectAnimator, Camera, Gyroscope, TextToSpeech, SpeechToText, RequestNetwork, "
                + "BluetoothConnect, LocationManager, ProgressDialog, DatePickerDialog, TimePickerDialog, "
                + "Notification, FragmentAdapter, InterstitialAd, RewardedVideoAd, Firebase, FirebaseAuth, "
                + "FirebaseStorage, FirebaseAuthPhone, FirebaseCloudMessage, FirebaseAuthGoogleLogin.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject componentType = new JsonObject();
        componentType.addProperty("type", "string");
        props.add("component_type", componentType);
        JsonObject componentName = new JsonObject();
        componentName.addProperty("type", "string");
        props.add("component_name", componentName);
        JsonObject param1 = new JsonObject();
        param1.addProperty("type", "string");
        props.add("param1", param1);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("component_type");
        required.add("component_name");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String componentType = args.has("component_type") ? args.get("component_type").getAsString() : null;
        String componentName = args.has("component_name") ? args.get("component_name").getAsString() : null;
        String param1 = args.has("param1") && !args.get("param1").isJsonNull()
                ? args.get("param1").getAsString() : null;
        if (componentType == null || componentName == null) {
            return ToolResult.error("component_type and component_name are required");
        }
        String scId = ctx.getScId();
        String javaName = ctx.getCurrentJavaName();
        if (scId == null || javaName == null) return ToolResult.error("No active project/layout.");
        try {
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            int typeInt = lookupComponentType(componentType);
            if (typeInt < 0) return ToolResult.error("Unknown component type: " + componentType);
            if (param1 != null && !param1.isEmpty()) {
                SketchwareApi.invoke(eC, "a", javaName, typeInt, componentName, param1);
            } else {
                SketchwareApi.invoke(eC, "a", javaName, typeInt, componentName);
            }
            SketchwareApi.invoke(eC, "k");
            return ToolResult.success("Added component " + componentType + " with name '" + componentName + "'.");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private int lookupComponentType(String name) {
        switch (name) {
            case "Intent": return 1;
            case "SharedPreferences": return 2;
            case "FilePicker": return 3;
            case "Calendar": return 4;
            case "Vibrator": return 5;
            case "TimerTask": return 6;
            case "Dialog": return 7;
            case "MediaPlayer": return 8;
            case "SoundPool": return 9;
            case "ObjectAnimator": return 10;
            case "Camera": return 11;
            case "Gyroscope": return 12;
            case "TextToSpeech": return 13;
            case "SpeechToText": return 14;
            case "RequestNetwork": return 15;
            case "BluetoothConnect": return 16;
            case "LocationManager": return 17;
            case "ProgressDialog": return 18;
            case "DatePickerDialog": return 19;
            case "TimePickerDialog": return 20;
            case "Notification": return 21;
            case "FragmentAdapter": return 22;
            case "InterstitialAd": return 23;
            case "RewardedVideoAd": return 24;
            case "Firebase": return 25;
            case "FirebaseAuth": return 26;
            case "FirebaseStorage": return 27;
            case "FirebaseAuthPhone": return 28;
            case "FirebaseCloudMessage": return 29;
            case "FirebaseAuthGoogleLogin": return 30;
            default: return -1;
        }
    }
}
