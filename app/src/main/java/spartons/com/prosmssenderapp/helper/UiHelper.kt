package spartons.com.prosmssenderapp.helper

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat.startActivityForResult
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import java.io.File


/**
 * Ahsen Saeed}
 * ahsansaeed067@gmail.com}
 * 6/25/19}
 */
const val PICK_CSV_FILE = 2

class UiHelper {

    fun showSimpleMaterialDialog(
        classingActivity: Activity,
        @StringRes title: Int, @StringRes content: Int,
        @DrawableRes icon: Int? = null,
        positiveText: String = "OK", cancelable: Boolean = true,
        negativeText: String? = null,
        drawable: Drawable? = null,
        negativeTextButtonListener: (() -> Unit)? = null,
        positiveButtonClickListener: () -> Unit
    ) {
        MaterialDialog(classingActivity).show {
            noAutoDismiss()
            if (icon != null)
                icon(icon)
            if (drawable != null)
                icon(drawable = drawable)
            title(title)
            if (negativeText != null)
                negativeButton(text = negativeText) {
                    negativeTextButtonListener?.invoke()
                    it.dismiss()
                }
            message(content)
            cancelable(cancelable)
            cornerRadius(16f)
            positiveButton(text = positiveText) {
                it.dismiss()
                positiveButtonClickListener.invoke()
            }
        }
    }

    fun showBottomSheetDialog(
        activity: Activity, @StringRes content: Int, @StringRes title: Int,
        positiveButtonText: String = "Submit", negativeButtonText: String = "Cancel",
        negativeTextButtonListener: (() -> Unit)? = null,
        positiveButtonClickListener: () -> Unit
    ) {
        MaterialDialog(activity, BottomSheet(layoutMode = LayoutMode.MATCH_PARENT)).show {
            title(title)
            cornerRadius(16f)
            noAutoDismiss()
            message(content)
            positiveButton(text = positiveButtonText) {
                it.dismiss()
                positiveButtonClickListener.invoke()
            }
            negativeButton(text = negativeButtonText) {
                negativeTextButtonListener?.invoke()
                it.dismiss()
            }
        }
    }

    fun showSingleSelectionListDialog(
        activity: Activity, @StringRes contentRes: Int, positiveText: String = "CHOOSE",
        choices: List<String> = emptyList(),
        cancelable: Boolean = false,
        singleChoiceListener: (Pair<String, Boolean>) -> Unit
    ) {
        MaterialDialog(activity).show {
            listItemsSingleChoice(items = choices, initialSelection = 0) { dialog, _, text ->
                dialog.dismiss()
                singleChoiceListener.invoke(text.toString() to dialog.isCheckPromptChecked())
            }
            checkBoxPrompt(
                text = "Always use this carrier number for Bulk SMS",
                isCheckedDefault = true
            ) {}
            cancelable(cancelable)
            message(contentRes)
            positiveButton(text = positiveText)
        }
    }
}