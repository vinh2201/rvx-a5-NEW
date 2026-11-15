package app.revanced.shared.patches.theme.resource

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element

// Old name: general-theme-resource-patch (helper patch, never selectable)
val generalThemeResourcePatch = resourcePatch(
    description = "Adds the splash screen color attribute used by the theme patches.",
) {
    compatibleWith("com.google.android.youtube"("16.40.36"))

    apply {
        // edit the resource files to change the splash screen color
        val attrsPath = "res/values/attrs.xml"
        val stylesPaths: List<String> = listOf(
            "res/values/styles.xml", // Android 11 (and below)
            "res/values-v31/styles.xml", // Android 12 (and above)
        )

        document(attrsPath).use { file ->
            (file.getElementsByTagName("resources").item(0) as Element).appendChild(
                file.createElement("attr").apply {
                    setAttribute("format", "reference")
                    setAttribute("name", "splashScreenColor")
                }
            )
        }

        stylesPaths.forEachIndexed { pathIndex, stylesPath ->
            document(stylesPath).use { file ->
                val childNodes = (file.getElementsByTagName("resources").item(0) as Element).childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val nodeAttributeName = node.getAttribute("name")

                    file.createElement("item").apply {
                        setAttribute(
                            "name",
                            when (pathIndex) {
                                0 -> "splashScreenColor"
                                1 -> "android:windowSplashScreenBackground"
                                else -> "null"
                            }
                        )
                        appendChild(
                            file.createTextNode(
                                when (pathIndex) {
                                    0 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher.Dark" -> "@color/yt_black1"
                                        "Base.Theme.YouTube.Launcher.Light" -> "@color/yt_white1"
                                        else -> "null"
                                    }

                                    1 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher" -> "?attr/splashScreenColor"
                                        else -> "null"
                                    }

                                    else -> "null"
                                }
                            )
                        )
                        if (this.textContent != "null")
                            node.appendChild(this)
                    }
                }
            }
        }

        arrayOf("drawable", "drawable-sw600dp").forEach { quantumLaunchscreenPath ->
            document("res/$quantumLaunchscreenPath/quantum_launchscreen_youtube.xml").use { file ->
                val resourcesNode = file.getElementsByTagName("item").item(0) as Element

                if (resourcesNode.attributes.getNamedItem("android:drawable") != null)
                    resourcesNode.setAttribute("android:drawable", "?attr/splashScreenColor")
            }
        }
    }
}
