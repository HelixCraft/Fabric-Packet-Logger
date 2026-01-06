# Task: Minecraft 1.21.5 - 1.21.8 Support Fixes

- [x] Investigate missing `dev.redstone.packagelogger.util` package
- [x] Fix `DrawUtil` placement and package declaration
- [x] Refactor UI Widgets to 1.21.5+ API (Click, KeyInput, CharInput) [/]
- [x] Fix text visibility in Lists (Alpha channel) [x]
- [x] Check for new packets in 1.21.5+ [x]
- [x] Verify build for 1.21.5 and 1.21.8
    - [x] Verify 1.21.5
    - [x] Verify 1.21.8
- [x] Merge/sync common changes across branches (like the `fabric.mod.json` sync) [x]

# 1.21.9+ Implementation
- [ ] **Planning**
    - [x] Create branch `1.21.9+`
    - [x] Analyze build errors
    - [x] Create Implementation Plan
- [x] **Core Refactoring**
    - [x] Update `PackageLoggerClient.java` (KeyBinding Category)
    - [x] Update `EntitySpawnS2CUnpacker.java` (Record Accessors)
- [x] **UI Refactoring**
    - [x] Replace `DrawContext.drawBorder` with `DrawUtil.drawBorder` in all screens
    - [x] Update `DualListSelectorWidget` input methods (Click/KeyInput/CharInput)
    - [x] Update `ColorPickerWidget` input methods
    - [x] Update `ColorSelectorWidget` input methods
    - [x] Update `ColorEditorScreen` input methods
    - [x] Update `SimpleConfigScreen` input methods
- [ ] **Verification**
    - [x] Compile for 1.21.9
    - [ ] Verify UI functionality in-game
