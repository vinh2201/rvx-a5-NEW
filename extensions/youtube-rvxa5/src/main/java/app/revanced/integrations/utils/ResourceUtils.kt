package app.revanced.integrations.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import app.revanced.integrations.utils.ReVancedUtils.getContext
import app.revanced.integrations.utils.ThemeHelper.resources

@SuppressLint("DiscouragedApi")
object ResourceUtils {
    @JvmStatic
    fun identifier(name: String, type: ResourceType) =
        identifier(name, type, getContext())

    @JvmStatic
    fun identifier(name: String, type: ResourceType, context: Context): Int =
        identifier(name, type.value, context)

    /**
     * Name of the package the resources of the app were compiled with (the package of resources.arsc).
     * It is read from a resource id of the app itself, so it does not depend on the package name of the manifest.
     */
    @Volatile
    private var compiledPackage: String? = null

    private fun findCompiledPackage(context: Context): String? {
        compiledPackage?.let { return it }

        return try {
            val info = context.applicationInfo
            val sample = if (info.icon != 0) info.icon else info.labelRes
            if (sample == 0) null else context.resources.getResourcePackageName(sample)
                .also { compiledPackage = it }
        } catch (ex: Throwable) {
            null
        }
    }

    /**
     * Looks up a resource by name. The package name of the manifest is tried first. If the installed package name
     * differs from the one the resources were compiled with (the package was renamed or got a suffix after the
     * patching), the lookup falls back to the compiled package, so settings, layouts and strings still resolve.
     */
    @JvmStatic
    fun identifier(name: String, defType: String, context: Context): Int {
        val res = context.resources

        val id = res.getIdentifier(name, defType, context.packageName)
        if (id != 0) return id

        val compiled = findCompiledPackage(context)
        if (compiled != null && compiled != context.packageName) {
            val fallback = res.getIdentifier(name, defType, compiled)
            if (fallback != 0) return fallback
        }

        return res.getIdentifier(name, defType, null)
    }

    @JvmStatic
    fun string(name: String) = identifier(name, ResourceType.STRING).let {
        if (it == 0) name else resources.getString(it)
    }

    @JvmStatic
    fun integer(name: String) = resources.getInteger(identifier(name, ResourceType.INTEGER))

    @JvmStatic
    fun anim(name: String) =
        AnimationUtils.loadAnimation(getContext(), identifier(name, ResourceType.ANIM))

    @JvmStatic
    fun <T : Class<*>, R : View> findView(clazz: T, view: View, name: String): R {
        return view.findViewById(identifier(name, ResourceType.ID)) ?: run {
            val ex = IllegalArgumentException("View with name $name not found")
            LogHelper.printException(clazz, "View not found", ex)
            throw ex
        }
    }

    @JvmStatic
    fun <T : Class<*>, R : View> findView(clazz: T, activity: Activity, name: String): R {
        return findView(clazz, activity.window.decorView, name)
    }
}

@Suppress("unused")
enum class ResourceType(internal val value: String) {
    ANIM("anim"),
    ATTR("attr"),
    COLOR("color"),
    DIMEN("dimen"),
    DRAWABLE("drawable"),
    FONT("font"),
    ID("id"),
    INTEGER("integer"),
    LAYOUT("layout"),
    MENU("menu"),
    MIPMAP("mipmap"),
    RAW("raw"),
    STRING("string"),
    STYLE("style"),
    XML("xml");
}