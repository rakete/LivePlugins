# Live Plugins

This is my something like a dot-files repo but for Jetbrains IntelliJ IDEs. It sets up my configuration with some common external tools that I prefer to have, and some [Live Plugins](https://github.com/dkandalov/live-plugin) that I implemented:

### 1. **TabIndentOrComplete**
This implements an Action that can be used to either automatically indent, or complete if the indentation is already correct. It will also shift the indentation right in empty lines.

### 2. **FocusToolOrMainWindow**
This can be used to toggle between last focused tool and the main window by using just one key.

### 3. **ShiftTabCompleteByWord**
This is so I can use the same keybinding that I use for triggering inline suggestion for also completing the suggestion by word. 

### 4. **CutLine**
So I can Copy+Delete a line with one keybinding.

---

## Gradle Copy Tasks

The `build.gradle.kts` file includes two custom tasks (`copyToLivePlugins` and `copyExternalTools`) designed to simplify deployment of these plugins to the IntelliJ configuration directory:

### 1. **`copyToLivePlugins`**
This task ensures that all the `live-plugins` scripts are copied to each IntelliJ IDE's corresponding `live-plugins` directory within the user's configuration path. The path is platform-dependent:
- Windows: `C:\Users\<username>\AppData\Roaming\JetBrains`
- Linux: `/home/<username>/.config/JetBrains`

It iterates over all JetBrains IDE directories and places the scripts in the appropriate locations. This is useful for environments with multiple IDE versions installed.

### 2. **`copyExternalTools`**
Similar to the `copyToLivePlugins` task, this task ensures that any custom tools within the `tools` directory are copied into the `tools` directory of each IntelliJ configuration. This task is particularly helpful if you utilize external tools or scripts that must be integrated into the IDEs seamlessly.