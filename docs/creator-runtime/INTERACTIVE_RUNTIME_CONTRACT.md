# Interactive Runtime Contract v1

Creator Runtime v1 adds visible behavior without introducing hidden source code. A screen interaction selects an event binding. The binding contains an ordered list of typed blocks. Blocks either plan typed project operations or emit a clearly labelled ephemeral effect.

| Element | Contract |
|---|---|
| Event | `targetWidgetId + eventName`, initially `click` for buttons and `change` for inputs/toggles. |
| State | Explicit project variables with stable ID, primitive value, and revisioned updates. |
| Block | A serializable type plus declarative payload; no Java/Kotlin source string. |
| Operation block | Plans a normal `ProjectOperation`, then the shared validator/reducer/session applies it. |
| Effect block | Emits a message or navigation request to the renderer; it cannot mutate Project IR. |
| Trace | Every event dispatch logs its binding ID, block count, resulting revision(s), and redacted effect metadata. |

The initial supported block vocabulary is deliberately small: `set_widget_property`, `set_state`, `show_message`, and `navigate`. Unsupported legacy logic is reported as native fallback rather than silently translated.
