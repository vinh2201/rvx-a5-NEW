package app.revanced.shared.patches.options

import app.revanced.patcher.patch.stringOption

// PORT-NOTE: the old `PatchOptions` resource patch (options container) is gone in patcher v22.
// Its options are now top-level option objects. Every patch that used `PatchOptions.X` registers the option
// in its builder by calling the option (e.g. `youTubePackageNameOption()`) and reads it inside
// `apply {}` with `val x by youTubePackageNameOption`.

/*
* Custom Branding Name
*/
internal val youTubeAppNameOption = stringOption(
    name = "Application Name of YouTube",
    description = "The name of the YouTube it will show on your home screen.",
    default = "ReVanced Extended",
    required = true
)

/*
* Custom Package Name (YouTube)
*/
internal val youTubePackageNameOption = stringOption(
    name = "Package Name of YouTube",
    description = "The package name of the YouTube. (NON-ROOT user only)",
    default = "app.rvx.android.youtube",
    required = true
)

/*
* Custom Speed Values
*/
internal val customSpeedArraysOption = stringOption(
    name = "Custom Speed Values",
    description = "A list of custom video speeds. Be sure to separate them with commas (,).",
    default = "0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 5.0",
    required = true
)

/*
* Overlay Buttons Icon
*/
internal val overlayButtonsIconOption = stringOption(
    name = "Overlay button icon selection",
    description = "Choose an overlay buttons icon (old/new)",
    default = "new",
    required = true
)

/*
* Theme
*/
internal val darkThemeBackgroundColorOption = stringOption(
    name = "Background color for the dark theme",
    description = "The background color of the dark theme. Can be a hex color or a resource reference.",
    default = "@android:color/black",
    required = true
)
