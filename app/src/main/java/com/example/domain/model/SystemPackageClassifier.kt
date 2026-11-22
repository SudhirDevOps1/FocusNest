package com.example.domain.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import com.example.core.database.AppUsageEntity
import com.example.core.database.InstalledAppEntity

data class SystemEnvironmentSnapshot(
  val selfPackage: String,
  val defaultHomePackage: String?,
  val homePackages: Set<String>,
  val inputMethodPackages: Set<String>,
  val permissionControllerPackages: Set<String>,
  val systemUiPackages: Set<String> = DEFAULT_SYSTEM_INFRA_PACKAGES,
) {
  companion object {
    val DEFAULT_SYSTEM_INFRA_PACKAGES =
      setOf(
        "com.android.systemui",
        "android",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.google.android.ext.services",
      )
  }
}

/**
 * Classifies Android packages into clear wellbeing analytics & blocker categories.
 * Dynamically inspects Android's active CATEGORY_HOME launcher and enabled IMEs on-device
 * so OEM launchers (Pixel, OneUI, ColorOS/Realme, MIUI, Nova, Lawnchair, etc.) are
 * accurately recognized without relying solely on hardcoded package lists.
 */
object SystemPackageClassifier {
  private val KNOWN_HOME_LAUNCHER_FALLBACKS =
    setOf(
      "com.android.launcher",
      "com.android.launcher2",
      "com.android.launcher3",
      "com.google.android.apps.nexuslauncher",
      "com.sec.android.app.launcher",
      "com.oppo.launcher",
      "com.realme.launcher",
      "com.miui.home",
      "com.mi.android.globallauncher",
      "com.oneplus.launcher",
      "com.teslacoilsw.launcher",
      "app.lawnchair",
      "ch.deletescape.lawnchair.plah",
    )

  fun inspectEnvironment(context: Context): SystemEnvironmentSnapshot {
    val pm = context.packageManager
    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    val defaultHome =
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          pm.resolveActivity(homeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
          @Suppress("DEPRECATION")
          pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }?.activityInfo?.packageName
      }.getOrNull()?.takeIf { it != "android" && it != context.packageName }

    val homePackages =
      runCatching {
        val list =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(homeIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
          } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(homeIntent, 0)
          }
        list.mapNotNull { it.activityInfo?.packageName }.filter { it != "android" }.toSet()
      }.getOrDefault(emptySet()) + setOfNotNull(defaultHome)

    val imePackages =
      runCatching {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.enabledInputMethodList?.mapNotNull { it.packageName }?.toSet().orEmpty()
      }.getOrDefault(emptySet())

    val permControllers =
      buildSet<String> {
        add("com.google.android.permissioncontroller")
        add("com.android.permissioncontroller")
        runCatching {
          val reqPermIntent = Intent("android.content.pm.action.REQUEST_PERMISSIONS")
          val infos =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              pm.queryIntentActivities(reqPermIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_SYSTEM_ONLY.toLong()))
            } else {
              @Suppress("DEPRECATION")
              pm.queryIntentActivities(reqPermIntent, PackageManager.MATCH_SYSTEM_ONLY)
            }
          infos.mapNotNull { it.activityInfo?.packageName }.forEach { add(it) }
        }
      }

    return SystemEnvironmentSnapshot(
      selfPackage = context.packageName,
      defaultHomePackage = defaultHome,
      homePackages = homePackages,
      inputMethodPackages = imePackages,
      permissionControllerPackages = permControllers,
    )
  }

  fun classify(
    packageName: String,
    isSystemFlag: Boolean,
    env: SystemEnvironmentSnapshot,
  ): AppPackageCategory {
    return when {
      packageName == env.selfPackage -> AppPackageCategory.SELF_APP
      packageName == env.defaultHomePackage ||
        packageName in env.homePackages ||
        packageName in KNOWN_HOME_LAUNCHER_FALLBACKS -> AppPackageCategory.LAUNCHER_HOME
      packageName in env.permissionControllerPackages -> AppPackageCategory.PERMISSION_CONTROLLER
      packageName in env.inputMethodPackages -> AppPackageCategory.INPUT_METHOD
      packageName in env.systemUiPackages -> AppPackageCategory.SYSTEM_UI_INFRASTRUCTURE
      isSystemFlag -> AppPackageCategory.SYSTEM_APP
      else -> AppPackageCategory.USER_APP
    }
  }

  fun shouldExcludeFromDistractingView(
    packageName: String,
    isSystemFlag: Boolean,
    includeSystemApps: Boolean,
    env: SystemEnvironmentSnapshot,
  ): Boolean {
    val category = classify(packageName, isSystemFlag, env)
    if (category == AppPackageCategory.SELF_APP) return true
    if (includeSystemApps) {
      // Even when inspecting system apps, never mark the active home launcher as distractible/self-locked
      return category == AppPackageCategory.LAUNCHER_HOME && packageName == env.defaultHomePackage
    }
    return category.excludeFromDistractingByDefault
  }

  fun filterUsageForDisplay(
    usage: List<AppUsageEntity>,
    installedMap: Map<String, InstalledAppEntity>,
    includeSystemApps: Boolean,
    env: SystemEnvironmentSnapshot,
  ): List<AppUsageEntity> {
    if (includeSystemApps) return usage
    return usage.filter { entry ->
      val installed = installedMap[entry.packageName]
      val isSys = installed?.isSystemApp ?: false
      val cat = classify(entry.packageName, isSys, env)
      !cat.excludeFromDistractingByDefault
    }
  }

  fun filterAppsForBlockerSelection(
    apps: List<InstalledAppEntity>,
    includeSystemApps: Boolean,
    env: SystemEnvironmentSnapshot,
  ): List<InstalledAppEntity> {
    return apps.filter { app ->
      !shouldExcludeFromDistractingView(
        packageName = app.packageName,
        isSystemFlag = app.isSystemApp,
        includeSystemApps = includeSystemApps,
        env = env,
      )
    }
  }
}
