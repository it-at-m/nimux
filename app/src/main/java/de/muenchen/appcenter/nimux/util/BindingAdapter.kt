package de.muenchen.appcenter.nimux.util

import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.InverseBindingMethod
import androidx.databinding.InverseBindingMethods
import com.google.android.material.chip.ChipGroup
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.model.NameColors
import timber.log.Timber


@InverseBindingMethods(
    InverseBindingMethod(
        type = ChipGroup::class,
        attribute = "android:checkedButton",
        method = "getCheckedChipId"
    )
)
class ChipGroupBindingAdapter {
    companion object {
        @JvmStatic
        @BindingAdapter("android:checkedButton")
        fun setCheckedChip(view: ChipGroup?, id: Int) {
            if (id != view?.checkedChipId)
                view?.check(id)
            else
                if (id != 0)
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            Timber.d("adapter new id: $id")
        }

        @JvmStatic
        @BindingAdapter(
            value = ["android:onCheckedChanged", "android:checkedButtonAttrChanged"],
            requireAll = false
        )
        fun setChipsListeners(
            view: ChipGroup?, listener: ChipGroup.OnCheckedChangeListener?,
            attrChange: InverseBindingListener?,
        ) {
            if (attrChange == null) {
                view?.setOnCheckedChangeListener(listener)
            } else {
                view?.setOnCheckedChangeListener { group, checkedId ->
                    attrChange.onChange()
                    listener?.onCheckedChanged(group, checkedId)
                }
            }
        }
    }
}

object TextViewBindingAdapter {
    @JvmStatic
    @BindingAdapter("app:isBold")
    fun setBold(view: TextView, isBold: Boolean?) {
        if (isBold != null && isBold) {
            view.typeface = Typeface.DEFAULT_BOLD
        } else {
            view.typeface = Typeface.DEFAULT
        }
    }

    @JvmStatic
    @BindingAdapter("app:premiumColor")
    fun setColor(view: TextView, nameColors: NameColors?) {
        val textColor = TypedValue()
        val theme = view.context.theme
        when (nameColors) {
            NameColors.DEFAULT -> {
                theme.resolveAttribute(R.attr.colorOnBackground, textColor, true)
            }

            NameColors.PURPLE -> {
                theme.resolveAttribute(R.attr.colorAmethyst, textColor, true)
            }

            NameColors.PINK -> {
                theme.resolveAttribute(R.attr.colorMagenta, textColor, true)
            }

            NameColors.ORANGE -> {
                theme.resolveAttribute(R.attr.colorOrange, textColor, true)
            }

            NameColors.BLUE -> {
                theme.resolveAttribute(R.attr.colorCapri, textColor, true)
            }

            NameColors.CYAN -> {
                theme.resolveAttribute(R.attr.colorSeagreen, textColor, true)
            }

            else -> {
                theme.resolveAttribute(R.attr.colorOnBackground, textColor, true)
            }
        }
        view.setTextColor(textColor.data)
    }
}

