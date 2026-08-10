package com.sketchware.ai.tools.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sketchware.ai.tools.SketchwareTool;
import com.sketchware.ai.tools.SketchwareToolContext;
import com.sketchware.ai.tools.ToolResult;
import com.sketchware.ai.util.SketchwareApi;

import java.util.ArrayList;
import java.util.List;

/**
 * block_add - add a block to an event's logic canvas via reflection.
 */
public final class BlockAddTool implements SketchwareTool {

    @Override public String name() { return "block_add"; }
    @Override public String category() { return "block"; }
    @Override public boolean isReadOnly() { return false; }

    @Override public String description() {
        return "Add a block to an event's logic canvas. "
                + "palette_id: 0=Variable, 1=List, 2=Control, 3=Operator, 4=Math, 5=File, "
                + "6=ViewFunc, 7=ComponentFunc, -1=Strings, 8=MoreBlock. "
                + "block_spec: e.g. 'setText %s.%s', 'if %b then %m else %m'. "
                + "attach_to (optional): { parent_block_id, slot (next/subStack1/subStack2/parameter), parameter_index }.";
    }

    @Override public JsonObject jsonSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonObject eventTarget = new JsonObject();
        eventTarget.addProperty("type", "string");
        eventTarget.addProperty("description", "Event target (e.g. 'button1_onClick')");
        props.add("event_target", eventTarget);
        JsonObject paletteId = new JsonObject();
        paletteId.addProperty("type", "integer");
        props.add("palette_id", paletteId);
        JsonObject blockSpec = new JsonObject();
        blockSpec.addProperty("type", "string");
        props.add("block_spec", blockSpec);
        JsonObject attachTo = new JsonObject();
        attachTo.addProperty("type", "object");
        JsonObject attProps = new JsonObject();
        JsonObject parentId = new JsonObject();
        parentId.addProperty("type", "string");
        attProps.add("parent_block_id", parentId);
        JsonObject slot = new JsonObject();
        slot.addProperty("type", "string");
        JsonArray slotEnum = new JsonArray();
        slotEnum.add("next"); slotEnum.add("subStack1"); slotEnum.add("subStack2"); slotEnum.add("parameter");
        slot.add("enum", slotEnum);
        attProps.add("slot", slot);
        JsonObject paramIdx = new JsonObject();
        paramIdx.addProperty("type", "integer");
        attProps.add("parameter_index", paramIdx);
        attachTo.add("properties", attProps);
        props.add("attach_to", attachTo);
        schema.add("properties", props);
        JsonArray required = new JsonArray();
        required.add("event_target");
        required.add("palette_id");
        required.add("block_spec");
        schema.add("required", required);
        return schema;
    }

    @Override public ToolResult execute(JsonObject args, SketchwareToolContext ctx) throws Exception {
        String eventTarget = args.has("event_target") ? args.get("event_target").getAsString() : null;
        int paletteId = args.has("palette_id") ? args.get("palette_id").getAsInt() : 0;
        String blockSpec = args.has("block_spec") ? args.get("block_spec").getAsString() : null;
        if (eventTarget == null || blockSpec == null) {
            return ToolResult.error("event_target and block_spec are required");
        }
        String scId = ctx.getScId();
        String javaName = ctx.getCurrentJavaName();
        if (scId == null || javaName == null) return ToolResult.error("No active project/layout.");
        try {
            Object eC = SketchwareApi.invokeStatic("a.a.a.jC", "a", scId);
            Object existing = SketchwareApi.invoke(eC, "a", javaName, eventTarget);
            int maxId = 0;
            List<Object> existingList = new ArrayList<>();
            if (existing instanceof List) {
                existingList = new ArrayList<>((List<?>) existing);
                for (Object b : existingList) {
                    Object idObj = getFieldValue(b, "id");
                    if (idObj != null) {
                        try {
                            int n = Integer.parseInt(idObj.toString());
                            if (n > maxId) maxId = n;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            String newId = String.valueOf(maxId + 1);

            Class<?> blockBeanClass = Class.forName("com.besome.sketch.beans.BlockBean");
            Object block = blockBeanClass.getDeclaredConstructor().newInstance();
            setField(block, "id", newId);
            setField(block, "spec", blockSpec);
            setField(block, "type", 0);
            setField(block, "opCode", blockSpec.split(" ")[0]);
            setField(block, "color", paletteColor(paletteId));
            setField(block, "parameters", new ArrayList<String>());
            setField(block, "subStack1", -1);
            setField(block, "subStack2", -1);
            setField(block, "nextBlock", -1);

            // Attach to parent if specified.
            if (args.has("attach_to") && args.get("attach_to").isJsonObject()) {
                JsonObject attach = args.getAsJsonObject("attach_to");
                String parentId = attach.has("parent_block_id") ? attach.get("parent_block_id").getAsString() : null;
                String slot = attach.has("slot") ? attach.get("slot").getAsString() : "next";
                if (parentId != null && !parentId.isEmpty()) {
                    int newIdInt = Integer.parseInt(newId);
                    for (Object parent : existingList) {
                        Object pid = getFieldValue(parent, "id");
                        if (pid != null && parentId.equals(pid.toString())) {
                            switch (slot) {
                                case "next": setField(parent, "nextBlock", newIdInt); break;
                                case "subStack1": setField(parent, "subStack1", newIdInt); break;
                                case "subStack2": setField(parent, "subStack2", newIdInt); break;
                                case "parameter":
                                    int idx = attach.has("parameter_index") ? attach.get("parameter_index").getAsInt() : 0;
                                    Object paramsObj = getFieldValue(parent, "parameters");
                                    if (paramsObj instanceof List) {
                                        @SuppressWarnings("unchecked")
                                        List<String> params = (List<String>) paramsObj;
                                        while (params.size() <= idx) params.add("");
                                        params.set(idx, "@" + newId);
                                    }
                                    break;
                            }
                            break;
                        }
                    }
                }
            }

            existingList.add(block);
            SketchwareApi.invoke(eC, "a", javaName, eventTarget, existingList);

            return ToolResult.success("Added block id=" + newId + " spec='" + blockSpec + "' to event " + eventTarget + ".");
        } catch (Throwable t) {
            return ToolResult.error(t);
        }
    }

    private Object getFieldValue(Object obj, String name) {
        try {
            java.lang.reflect.Field f;
            try { f = obj.getClass().getDeclaredField(name); }
            catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable t) { return null; }
    }

    private void setField(Object obj, String name, Object value) {
        try {
            java.lang.reflect.Field f;
            try { f = obj.getClass().getDeclaredField(name); }
            catch (NoSuchFieldException e) { f = obj.getClass().getSuperclass().getDeclaredField(name); }
            f.setAccessible(true);
            Class<?> t = f.getType();
            if (t == int.class && value instanceof Integer) f.setInt(obj, (Integer) value);
            else if (t == boolean.class && value instanceof Boolean) f.setBoolean(obj, (Boolean) value);
            else f.set(obj, value);
        } catch (Throwable ignored) {}
    }

    private int paletteColor(int paletteId) {
        switch (paletteId) {
            case 0: return 0xffee7d16;
            case 1: return 0xffcc5b22;
            case 2: return 0xffe1a92a;
            case 3: return 0xff5cb722;
            case 4: return 0xff23b9a9;
            case 5: return 0xffa1887f;
            case 6: return 0xff4a6cd4;
            case 7: return 0xff2ca5e2;
            case 8: return 0xff8a55d7;
            default: return 0xff7c83db;
        }
    }
}
