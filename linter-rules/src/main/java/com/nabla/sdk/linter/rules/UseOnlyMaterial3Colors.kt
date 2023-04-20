package com.nabla.sdk.linter.rules

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.android.tools.lint.detector.api.XmlScannerConstants
import org.w3c.dom.Attr
import java.util.EnumSet

internal class UseOnlyMaterial3Colors : Detector(), XmlScanner {
    override fun getApplicableAttributes(): MutableList<String> = XmlScannerConstants.ALL

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        if (
            COLOR_ATTRIBUTE_HEURISTIC_REGEX.matches(attribute.name) &&
            attribute.value.startsWith('?') && // white-list stuff like "@color/black"
            !attribute.value.contains("nabla") && // white-list stuff like "?nablaMessaging_fooColor"
            attribute.value !in MATERIAL3_COLORS_LIST
        ) {
            context.report(
                issue = USE_ONLY_MATERIAL3_COLORS,
                location = context.getLocation(attribute),
                message = DESCRIPTION,
            )
        }
    }
}

private val DESCRIPTION = """
        You should only use Material3 theme colors. Find the right color to use from this exhaustive list:
        https://github.com/material-components/material-components-android/blob/master/lib/java/com/google/android/material/color/res/values/attrs.xml
        
        More details in the guide: https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md
""".trimIndent()

private val COLOR_ATTRIBUTE_HEURISTIC_REGEX = "(.*color)|(.*tint)|(background)"
    .toRegex(RegexOption.IGNORE_CASE)

internal val USE_ONLY_MATERIAL3_COLORS = Issue.create(
    id = "DoNotUseSnakeCaseForIds",
    briefDescription = DESCRIPTION,
    explanation = DESCRIPTION,
    category = Category.CORRECTNESS,
    priority = 7,
    severity = Severity.WARNING,
    implementation = Implementation(
        UseOnlyMaterial3Colors::class.java,
        EnumSet.of(Scope.RESOURCE_FILE),
    ),
)

private val MATERIAL3_COLORS_LIST = listOf(
    "colorPrimary",
    "colorOnPrimary",
    "colorPrimaryInverse",
    "colorPrimaryContainer",
    "colorOnPrimaryContainer",
    "colorSecondary",
    "colorOnSecondary",
    "colorSecondaryContainer",
    "colorOnSecondaryContainer",
    "colorTertiary",
    "colorOnTertiary",
    "colorTertiaryContainer",
    "colorOnTertiaryContainer",
    "android:colorBackground",
    "colorOnBackground",
    "colorSurface",
    "colorOnSurface",
    "colorSurfaceVariant",
    "colorOnSurfaceVariant",
    "colorSurfaceInverse",
    "colorOnSurfaceInverse",
    "colorOutline",
    "colorError",
    "colorOnError",
    "colorErrorContainer",
    "colorOnErrorContainer",

    // pre-Material3 legacy — inherited by Material3 themes
    "android:textColorPrimary",
    "android:textColorSecondary",
    "colorControlHighlight",
).flatMap { listOf("?$it", "?attr/$it") }
