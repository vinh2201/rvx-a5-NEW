# PORTING GUIDE — revanced-patches-android5 (patcher 14.2.2 class API)  ->  ReVanced patcher v22 DSL

You are one of several agents porting the OLD patch sources (YouTube 16.40.36, Android 5+) to the NEW
patcher v22 Kotlin DSL **in parallel**. Every agent owns a disjoint set of directories. Follow this guide exactly so
that cross-references between groups line up. Respond in English (the user only reads the final summary of the lead).

## 0. Paths (all absolute)
SCR = /tmp/claude-0/-home-claude/7cbb9026-938e-5723-9df1-1254581036c4/scratchpad
- OLD sources (read-only):  $SCR/src/rv5/src/main/kotlin/app/revanced/{shared,patches/youtube}   resources: $SCR/src/rv5/src/main/resources
- OLD runtime extension ("integrations", Java/Kotlin, DO NOT rewrite): $SCR/src/rv5int/app/src/main/java  (already copied into the new project)
- NEW project (write here): $SCR/out/revanced-patches-android5
  - kotlin: $PROJECT/patches/src/main/kotlin/app/revanced/...      resources: $PROJECT/patches/src/main/resources/...
  - extension (Java/Kotlin runtime half): $PROJECT/extensions/youtube-rvxa5/src/main/java/app/revanced/integrations/...
- REFERENCE (official ReVanced patches on patcher v22 — ground truth for syntax/imports!): $SCR/src/rv22/patches/src/main/kotlin/app/revanced
  (util already copied: $PROJECT/patches/src/main/kotlin/app/revanced/util/{BytecodeUtils,ResourceUtils,Utils,FreeRegisterProvider}.kt + util/resource/)
- Other (RVX lineage, patcher 21 style = functional DSL already): $SCR/src/rv6 (YouTube 17.34.36). Legacy branch of another patch group (own patcher, similar DSL, YouTube 19.16.39): $SCR/src/legacy-fork/revanced-patches-legacy-main
- Decompiled APKs (apktool, smali + res): $SCR/apk/<dir>/  -> 16.40.36 is THE TARGET. Find with `ls $SCR/apk`. smali dirs: smali, smali_classes2, ...
- Tools: lint: `python3 -I $SCR/tools/lint_kt.py $PROJECT/patches/src/main/kotlin <your dirs/files>`  (missing/unknown imports, redeclarations, leftover legacy API)
         syntax: `$SCR/tools/ktsyntax.sh <files...>` (prints Kotlin syntax errors only; ignore nothing it prints in YOUR files)

DSL vocabulary actually used by the reference (imports with frequency): $SCR/DSL_IMPORTS.txt — only use names from there (plus util/*.kt).

## 1. Build facts
- Project is Gradle plugin `app.revanced.patches` 1.0.0-dev.10 + patcher 22.0.0 (same as the reference). We CANNOT compile here, so correctness comes from
  copying idioms from the reference repo. NEVER invent an API/import: every `import app.revanced.patcher.*` you write must be an import you saw in the reference
  (`grep -rh "^import app.revanced.patcher" $REF | sort -u`). If you need something you did not see, grep the reference/util for an equivalent first.
- Do NOT use `context(...)` parameters (our syntax checker cannot parse them). Use plain extension functions on `BytecodePatchContext` instead, or pass `this`.
- Package names of patches STAY as in the old tree (`app.revanced.patches.youtube...`, `app.revanced.shared...`). Keep the old directory layout and file names where a file
  holds a patch; fingerprints of a directory are merged into ONE `Fingerprints.kt` placed in the old `fingerprints` package folder (package name unchanged).
- Extension class descriptors are UNCHANGED: `Lapp/revanced/integrations/...` (constant `INTEGRATIONS_PATH` in shared/util/integrations/Constants.kt).
- Runtime extension is ONE module: only `integrationsPatch` calls `extendWith("extensions/youtube-rvxa5.rve")`; everything else just `dependsOn(integrationsPatch)` (directly or through settingsPatch).
- Extension Java code must stay Android-5 (API 21) safe: no java.time, no java.util.function/stream/Optional/List.of/Map.of/getOrDefault/removeIf/String.join (no desugaring!).

## 2. Naming contract (CRITICAL for cross-group references)
| old | new |
|---|---|
| `class FooPatch : BytecodePatch/ResourcePatch` in file `.../FooPatch.kt` | `val fooPatch = bytecodePatch/resourcePatch(...) {}` in the SAME file (same package). Name = lowerCamel of class name |
| `object XyzFingerprint : MethodFingerprint(...)` | `internal val BytecodePatchContext.xyzMethodMatch by composingFirstMethod { ... }`  (ALWAYS composingFirstMethod + suffix `MethodMatch`, even when no indices are needed; drop the `Fingerprint` suffix). Visibility: `internal` (but `val` without internal if used from another module — never the case). |
| shared helpers `object ResourceHelper/ResourceUtils/BytecodeHelper/...` | SAME object/function names & parameter order; only context types change: `BytecodeContext`->`BytecodePatchContext`, `ResourceContext`->`ResourcePatchContext`. Callers inside `apply {}` pass `this`. |
| `PatchOptions.X` (shared/patches/options) | top-level option objects in `app.revanced.shared.patches.options` file `PatchOptions.kt`: `youTubeAppNameOption`, `youTubePackageNameOption`, `customSpeedArraysOption`, `overlayButtonsIconOption`, `darkThemeBackgroundColorOption` (see §5) |
| `ResourceMappingPatch.resourceMappings.single { it.name == "x" }.id` | `resourceIdByName("x")` (from `app.revanced.patches.shared.misc.mapping`) or typed `ResourceType.LAYOUT["x"]`. Dependency `ResourceMappingPatch::class` -> `resourceMappingPatch` |
| `SharedResourcdIdPatch` (sic) in `misc/resourceid/patch/SharedResourceIdPatch.kt` | `val sharedResourceIdPatch` (keep the exposed ids as top-level `var xxxId = -1L` in that file, same names as the old `companion` members) |
| `@Name("hide-general-ads")` | `name = "Hide general ads (A5)"` (human readable, English, sentence case, **always with the suffix ` (A5)`** so no name can clash with another bundle; take rv6's name from `$SCR/src/rv6/patches.json` when an equivalent exists and add the suffix). Internal/helper patches (no `@Patch`) get NO name |
| `@Description("...")` | `description = "..."` |
| `@Patch(false)` / `include=false` | `use = false` |
| `@DependsOn([A::class, B::class])` | `dependsOn(aPatch, bPatch)` |
| `@YouTubeCompatibility` | `compatibleWith("com.google.android.youtube"("16.40.36"))` inside the builder (no import needed) — on EVERY named patch, and on helper patches that previously had it (harmless) |

## 3. Patch skeleton (v22)
```kotlin
val hideGeneralAdsPatch = bytecodePatch(
    name = "Hide general ads (A5)",
    description = "Removes general ads.",
    use = true,               // omit when true
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))
    dependsOn(integrationsPatch, settingsPatch /* , ... */)
    apply {                   // `this` = BytecodePatchContext; old `execute(context)` body goes here
        fooMethodMatch.method.addInstructions(0, "...")
    }
}
val brandingResourcePatch = resourcePatch(name = "...", description = "...") {
    dependsOn(...)
    apply { /* `this` = ResourcePatchContext */ document("res/values/strings.xml").use { doc -> ... } }
}
```
Options: top-level `internal val xOption = stringOption(name = "...", description = "...", default = "...", required = true)`; in each patch builder call `xOption()`;
inside `apply {}` read `val x by xOption` (exact idiom: reference `shared/layout/theme/BaseThemePatch.kt`).
Pure hook patches without a name are fine: `val fooHookPatch = bytecodePatch { dependsOn(..); apply { } }`.
If an old patch is `BytecodePatch` AND needs resource edits, split it exactly as the old code did (old code already had separate resource/bytecode patch classes).

## 4. API mapping (old -> v22). Look up exact idioms in the reference, e.g. `grep -rn "<idiom>" $REF`
- `X.result ?: throw X.exception` / `X.result!!`  -> just use `xMethodMatch` (throws PatchException itself). Remove the `exception` extension use.
- `result.mutableMethod`  -> `xMethodMatch.method`;  `result.method` -> `xMethodMatch.immutableMethod`;  `result.mutableClass` -> `xMethodMatch.classDef`;  `result.classDef` -> `xMethodMatch.immutableClassDef`
- `scanResult.patternScanResult!!.startIndex` -> `xMethodMatch[0]` ; `.endIndex` -> `xMethodMatch[-1]`  (VERIFIED: old endIndex is the INCLUSIVE index of the last matched instruction, `[-1]` is the last index of the first indexed matcher)
  - indices come from the `opcodes(...)` / `instructions(...)` matcher. If a legacy fingerprint has BOTH opcodes and strings, put the strings as positional args of `composingFirstMethod("s1","s2") { opcodes(...) }` — then `match[0]` still refers to the opcode pattern (see reference `buildInitPlaybackRequestMethodMatch`).
  - `stringsScanResult!!.matches[i].index` -> `indexOfFirstInstructionOrThrow { stringReference?.string == "..." }` (util/BytecodeUtils.kt has helpers; read it) or `xMethodMatch[1, i]` ONLY if you verified in reference.
- fingerprint DSL:  `returnType = "V"` -> `returnType("V")` (prefix match, same as legacy: "L" matches any object) ; `accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL` -> `accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)` (exact set) ;
  `parameters = listOf("Z","L")` -> `parameterTypes("Z","L")` (prefix per parameter, size must equal; `listOf()` -> `parameterTypes()`) ; `opcodes = listOf(A, B, C)` (no wildcard) -> `opcodes(Opcode.A, Opcode.B, Opcode.C)` (contiguous);
  legacy WILDCARD entries (`null` = exactly one arbitrary instruction) -> use `instructions(Opcode.A(), afterAtMost(1, Opcode.B()), after(Opcode.C()))` style (`after(m)` = m is the very next instruction, `afterAtMost(n, m)` = m within n skipped instructions; `Opcode.X()` invokes the opcode into a matcher; see reference `shared/misc/privacy/Fingerprints.kt`). k consecutive nulls -> `afterAtMost(k, ...)`.
  **Indices change with wildcards**: old code did `startIndex + k` to reach later instructions; new `match[j]` is the index of the j-th MATCHER (wildcard positions have no entry). Recompute every offset by reading the old pattern carefully. When a legacy pattern is used only to locate ONE spot, prefer the simplest equivalent;
  `customFingerprint = { methodDef, classDef -> methodDef.name == "x" && classDef.type.endsWith("Foo;") }` -> `custom { name == "x" && immutableClassDef.type.endsWith("Foo;") }` (receiver is the method; `immutableClassDef` is the class). `methodDef.definingClass` stays `definingClass`.
  - **STRINGS ARE EXACT (full-literal) IN v22**; legacy `strings` were SUBSTRING matches. For EVERY string verify in the 16.40.36 smali (`grep -rn --include=*.smali 'const-string.*"yourString' $SCR/apk/<16.40.36 dir>`) whether it is the full literal. If it is only a fragment, use `instructions("fragment"(String::contains))` (reference uses `"..."(String::startsWith)` — `String::contains` / `String::endsWith` work the same way) instead of a positional string arg.
  - `definingClass("...")` in the new DSL is a full or partial type; the reference writes partial matches like `definingClass("/YouTubePlayerOverlaysLayout;")` (endsWith semantics) — use it when the old code did `classDef.type.endsWith(..)`.
  - Resource id literals: `ResourceType.LAYOUT("name")` as an instruction matcher inside `instructions(...)` (reference: `ResourceType.STRING("accessibility_play")`); old code that searched `Instruction31i.wideLiteral == id` -> `indexOfFirstLiteralInstructionOrThrow(id)` (util).
- `context.classes.forEach { classDef -> ... context.proxy(classDef).mutableClass }` -> `classDefs.forEach { classDef -> ...; classDefs.getOrReplaceMutable(classDef) }` (see util `BytecodeUtils.kt` and reference `transformInstructionsPatch`/`HideAdsPatch`); find a class: `firstClassDef("Lfoo;")`/`firstClassDefOrNull`.
- `context.findClass { it.type == "..." }!!.mutableClass` -> `firstClassDef("L...;")` (mutable) ; `context.proxy(classDef).mutableClass.methods.add(...)` -> `firstClassDef(type).methods.add(...)` (reference: `createStreamingDataMethodMatch.classDef.methods.add`)
- `method.toMethodWalker(context)` / `.nextMethod(index, true)` -> `navigate(method).to(index).stop()` (reference SeekbarColorPatch uses `navigate(it).to(i).stop()`; `.original()` for the immutable method)
- `ImmutableMethod(...).toMutable()` : `import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable` (NOTE the `app.revanced.` prefix on mutable types) ; `MutableMethod` -> `app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod`
- instruction extension imports are `app.revanced.patcher.extensions.{addInstruction,addInstructions,addInstructionsWithLabels,getInstruction,replaceInstruction,replaceInstructions,removeInstruction,removeInstructions,instructions,methodReference,fieldReference,stringReference,wideLiteral,ExternalLabel}` — NOT `InstructionExtensions.X`. `BuilderInstruction` / `MutableMethodImplementation` still from `com.android.tools.smali.dexlib2.builder...`.
- Resources: `context.xmlEditor["res/x.xml"].use { it.file }` -> `document("res/x.xml").use { doc -> ... }` ; `context["res/values-v21"].mkdirs()` -> `get("res/values-v21").mkdirs()` ; `context.copyResources(dir, ResourceGroup(...))` -> keep the ported `ResourceUtils` helper (core agent ports it; same signature) ; AndroidManifest edits via `document("AndroidManifest.xml")`.
- Never use `execute`; use `apply`. Old `finalize(context)` -> `afterDependents {}`/`finalize {}` as in reference (grep `afterDependents`).
- `throw PatchException("...")` (import `app.revanced.patcher.patch.PatchException`) replaces the old `PatchResultError`/`exception`.
- Old `context.integrations` hooks are replaced by `extendWith` (core agent). Do not add `extendWith` anywhere else.

## 5. Behaviour must stay identical
This is a mechanical port: same hooks, same smali strings, same registers. Do NOT redesign logic and do NOT "improve" anything now — a separate pass improves features. EXCEPTION: if the old code is obviously broken
for v22 semantics (e.g. relies on a removed API) adapt minimally and add a `// PORT-NOTE:` comment explaining. Keep resources (`youtube/...` folders) paths identical: copy needed resource trees from the old resources dir into `$PROJECT/patches/src/main/resources/` ONLY for resource folders your group owns (listed in your assignment); never touch another group's files. Create missing parent dirs.

## 6. Checks you MUST run before finishing
1. `python3 -I $SCR/tools/lint_kt.py $PROJECT/patches/src/main/kotlin <your dirs>` — fix every finding in your files (false positives: a name that is ambiguous between two packages/ a local variable; if you are sure, leave it and mention it).
2. `$SCR/tools/ktsyntax.sh <your files>` — must print nothing for your files.
3. Count: every old `@Patch` class and every old fingerprint in your assignment has exactly one new counterpart (write a short mapping table old-name -> new-name in your final answer).
4. For each fingerprint verify against the 16.40.36 smali that the string literals you used as exact strings exist verbatim (see §4).
5. Final answer to the lead: (a) files written, (b) mapping table of patch names (old kebab-name -> new display name), (c) anything uncertain / PORT-NOTE list, (d) every cross-group symbol you CALL (name + expected file) so the lead can verify.
