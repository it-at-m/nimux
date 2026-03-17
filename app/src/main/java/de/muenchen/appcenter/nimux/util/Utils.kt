package de.muenchen.appcenter.nimux.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.CountDownTimer
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.ActionMode
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import de.muenchen.appcenter.nimux.R
import de.muenchen.appcenter.nimux.databinding.MultiOrderOverviewRvLayoutBinding
import de.muenchen.appcenter.nimux.databinding.MultiOrderProductRvLayoutBinding
import de.muenchen.appcenter.nimux.model.MultiOrderProductListWithProduct
import de.muenchen.appcenter.nimux.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.round


const val collection_users = "users"
const val collection_suggest_users = "suggest_users"
const val collection_products = "products"
val collection_stats = "stats"
val collection_stats_total = "items"
val collection_donate = "donateItem"
val collection_userLogs = "userLogs"
val collection_productLogs = "productLogs"
val collection_help = "helpCollection"
val collection_loginlogoutlogs = "LoginLogoutLogs"
val collection_passwordManage = "ManagePWCollection"

const val coll_stats_main_doc = "document"

const val product_icon_none = 0
const val product_icon_cup = 1
const val product_icon_bottle = 2
const val product_icon_water = 3
const val product_icon_fastfood = 4
const val product_icon_fridge = 5
const val product_icon_cookie = 6
const val product_icon_egg = 7
const val product_icon_pizza = 8
const val product_icon_tea = 9
const val product_icon_icecream = 10
const val product_icon_can = 11

const val userlog_description_add_user = "Benutzer hinzugefügt"
const val userlog_description_deleted = "Benutzer gelöscht"
const val userlog_description_add_balance = "Guthaben hinzugefügt"
const val userlog_description_add_balance_paypal =
    "Guthaben durch PayPal aufgeladen. Rechnungs-ID: %s, Transaktionscode: %s"
const val userlog_description_payed = "Gekauft: "
const val userlog_description_payed_with_donation = "(Spende aktiv) Gekauft: "
const val userlog_description_dono_started = "Spende gestartet bis "
const val userlog_description_update =
    "Benutzer bearbeitet. Guthaben sichtbar: %b; Statistiken auswerten: %b; PIN benötigt: %b"
const val userlog_mail_update =
    "Benutzer Mail geändert durch PayPal Anfrage."

const val productlog_description_add_product = "Produkt hinzugefügt"
const val productlog_description_delete_product = "Produkt gelöscht"
const val productlog_description_add_stock = "Bestand aufgefüllt"
const val productlog_description_update =
    "Produkt bearbeitet. Preis: %s; Icon Nr. %s; aktueller Bestand: %s; Auffüllmenge: %s"

//SettingsFragment aka global settings pref keys
const val standbyBoolPrefKey = "PREFERENCE_STANDBY_KEY"
const val scanFaceStandbyPrefKey = "PREFERENCE_FACE_SCAN_KEY"
const val faceRecognitionPrefKey = "PREFERENCE_FRAGMENT_SCAN_FACES_KEY"
const val scanProductPrefKey = "PREFERENCE_SCAN_PRODUCTS"
const val checkInvoicesPrefKey = "PREFERENCE_CHECK_INVOICES"

//User specific pref
const val userPrefsScanProductsPreferenceKey = "scan_products_pref"
const val userPrefsFaceReplacePinPreferenceKey = "facial_scan_replaces_pin_pref"

fun hideKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    // Check if no view has focus
    val currentFocusedView = activity.currentFocus
    currentFocusedView?.let {
        inputMethodManager.hideSoftInputFromWindow(
            currentFocusedView.windowToken, 0
        )
    }
    currentFocusedView?.clearFocus()
}

fun showKeyboard(activity: Activity) {
    val inputMethodManager =
        activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    // Check if no view has focus
    val currentFocusedView = activity.currentFocus
    currentFocusedView?.let {
        inputMethodManager.showSoftInput(
            currentFocusedView, InputMethodManager.SHOW_IMPLICIT
        )
    }
}


fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}


//Coroutine Firestore Helper Methods
@ExperimentalCoroutinesApi
suspend inline fun <reified T : Any> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}

fun showNetworkHint(
    context: Context,
    listener: DialogInterface.OnClickListener,
    dismisslistener: DialogInterface.OnDismissListener,
) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.network_alert_title)
        .setMessage(R.string.network_alert_message)
        .setPositiveButton(R.string.ok, null)
        .setNegativeButton(R.string.retry, listener)
        .show()
        .setOnDismissListener(dismisslistener)
}

class LogInLogOutLog(
    val login: Boolean = false,
) {
    val timestamp = Calendar.getInstance().time
    val accountEmail = FirebaseAuth.getInstance().currentUser!!.email
    val device = Build.MODEL
}

fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}

fun isEmailValid(email: String?): Boolean {
    val EMAIL_PATTERN: String =
        "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
    val pattern: Pattern = Pattern.compile(EMAIL_PATTERN)
    val matcher: Matcher = pattern.matcher(email)
    return matcher.matches()
}

data class PeakWeekStatDocument(
    val productName: String = "",
    var producticon: Int = 0,

    var highestAmount: Int = 0,

    var highestWeek: Int = 0,
    var startOfHighestWeek: String = "",
    var endOfHighestWeek: String = "",
    var highestYear: Int = 0,

    var currentAmount: Int = 0,

    var currentWeek: Int = 0,
    var startOfCurrentWeek: String = "",
    var endOfCurrentWeek: String = "",
    var currentYear: Int = 0,

    var lastAmount: Int = 0,

    var lastWeek: Int = 0,
    var lastWeekYear: Int = 0,
)

data class FeedbackDoc(
    val title: String = "",
    val description: String = "",
    val name: String = "",
    val occurrence: String = "",
) {
    val timestamp = Calendar.getInstance().time
}

class RangeValidator(private val minDate: Long, private val maxDate: Long) :
    CalendarConstraints.DateValidator {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun isValid(date: Long): Boolean {
        return !(minDate > date || maxDate < date)

    }

    companion object CREATOR : Parcelable.Creator<RangeValidator> {
        override fun createFromParcel(parcel: Parcel): RangeValidator {
            return RangeValidator(parcel)
        }

        override fun newArray(size: Int): Array<RangeValidator?> {
            return arrayOfNulls(size)
        }
    }
}

fun getChartColors(context: Context, num: Int): IntArray {
    val colorArray: ArrayList<Int> = ArrayList()
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_lemon))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_champagne))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_baby))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_lavender))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_mauve))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_blue))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_light_blue))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_electric))
    colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_mint))
    if (num.mod(10) != 0)
        colorArray.add(ContextCompat.getColor(context, R.color.chart_pastel_apple))
    return colorArray.toIntArray()
}

fun showEnterUserPin(user: User, context: Context, view: View, pinTrueAction: () -> Unit) {
    var falseEnterAmount = context.getSharedPreferences("BruteForceProtect", Context.MODE_PRIVATE)
        .getInt(user.stringSortID + "falseAttempts", 0)
    val remainingTimeAmount =
        context.getSharedPreferences("BruteForceProtect", Context.MODE_PRIVATE)
            .getLong(user.stringSortID + "endTime", 0L)

    val res = context.resources
    val builder = MaterialAlertDialogBuilder(context)

    val v = LayoutInflater.from(context).inflate(R.layout.pin_enter_dialog_layout, null)
    val inputView = v.findViewById<TextInputLayout>(R.id.pin_enter_dialog_edittext)
    val bruteForceHintView = v.findViewById<TextView>(R.id.brute_force_warn)

    builder.setTitle(res.getString(R.string.enter_pin_alert_title))
        .setView(v)
        .setPositiveButton(res.getString(R.string.confirm_pin), null)
        .setNegativeButton(res.getString(R.string.cancel), null)

    val dialog = builder.show()
    val activity = context.findActivity()
    val window = dialog.window
    if (window != null && activity != null) {
        window.callback = UserInteractionAwareCallback(window.callback, activity)
    }

    val pButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
    pButton.setOnClickListener {
        if ((inputView.editText?.text.toString().toIntOrNull() != null) &&
            (md5(inputView.editText?.text.toString()) == user.pin.toString())
        ) {
            inputView.error = null
            dialog.dismiss()
            clearBruteForceData(context, user.stringSortID)
            pinTrueAction()
        } else {
            falseEnterAmount++
            Timber.d("BruteForceLog $falseEnterAmount")
            checkBruteForce(
                falseEnterAmount,
                context,
                inputView,
                bruteForceHintView,
                pButton,
                res,
                0L,
                user.stringSortID
            )
            inputView.error = res.getString(R.string.wrong_pin_input)
        }
    }


    v.postDelayed({
        val remainingSecs = remainingTimeAmount - Calendar.getInstance().timeInMillis
        if (remainingSecs > 0) checkBruteForce(
            falseEnterAmount,
            context,
            inputView,
            bruteForceHintView,
            pButton,
            res,
            remainingSecs.div(1000),
            user.stringSortID
        )

    }, 200)

    inputView?.editText?.requestFocus()
    inputView?.editText?.setOnKeyListener { _, i, keyEvent ->
        if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) {
            if ((inputView.editText?.text.toString().toIntOrNull() != null) &&
                (md5(inputView.editText?.text.toString()) == user.pin.toString())
            ) {
                inputView.error = null
                dialog.dismiss()
                clearBruteForceData(context, user.stringSortID)
                pinTrueAction()
            } else {
                falseEnterAmount++
                Timber.d("BruteForceLog 2 $falseEnterAmount")
                checkBruteForce(
                    falseEnterAmount,
                    context,
                    inputView,
                    bruteForceHintView,
                    pButton,
                    res,
                    0L,
                    user.stringSortID
                )
                inputView.error = res.getString(R.string.wrong_pin_input)
            }
        }
        false
    }
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun clearBruteForceData(context: Context, userId: String) {
    context.getSharedPreferences("BruteForceProtect", Context.MODE_PRIVATE).edit().apply {
        putLong(userId + "endTime", 0L)
        putInt(userId + "falseAttempts", 0)
        commit()
    }
}

fun checkBruteForce(
    falseEnterAmount: Int,
    context: Context,
    inputView: TextInputLayout,
    bruteForceHintView: TextView,
    pButton: Button,
    res: Resources,
    remainingTime: Long = 0L,
    userId: String,
) {
    val timer: CountDownTimer
    var bruteForceLockOutTime = 0

    Timber.d("BruteForceLog 3 ${falseEnterAmount.div(3)}")

    if (remainingTime == 0L) {
        when (falseEnterAmount.div(3)) {
            0 -> {}
            1 -> {
                bruteForceLockOutTime = 30
            }

            2 -> {
                bruteForceLockOutTime = 60
            }

            3 -> {
                bruteForceLockOutTime = 120
            }

            4 -> {
                bruteForceLockOutTime = 300
            }

            5 -> {
                bruteForceLockOutTime = 900
            }

            else -> {
                bruteForceLockOutTime = 1800
            }
        }
        val cal = Calendar.getInstance()
        cal.add(Calendar.SECOND, bruteForceLockOutTime)
        context.getSharedPreferences("BruteForceProtect", Context.MODE_PRIVATE).edit().apply {
            putLong(userId + "endTime", cal.timeInMillis)
            putInt(userId + "falseAttempts", falseEnterAmount)
            commit()
        }
    } else {
        bruteForceLockOutTime = remainingTime.toInt()
        Timber.d("BruteForceLog 4 $remainingTime")
    }
    if (bruteForceLockOutTime != 0) {
        timer = object : CountDownTimer((bruteForceLockOutTime).times(1000).toLong(), 1000) {
            override fun onTick(p0: Long) {
                hideKeyboard(context as Activity)
                inputView.editText?.isEnabled = false
                pButton.isEnabled = false
                bruteForceHintView.visibility = View.VISIBLE
                bruteForceHintView.text =
                    if (p0 < 120000) res.getString(
                        R.string.pin_brute_force_string_seconds,
                        p0.div(1000)
                    ) else res.getString(
                        R.string.pin_brute_force_string_minutes,
                        p0.div(1000).div(60)
                    )
            }

            override fun onFinish() {
                inputView.editText?.isEnabled = true
                pButton.isEnabled = true
                bruteForceHintView.visibility = View.GONE
                showKeyboard(context as Activity)
            }
        }.start()
    }
}

class UserInteractionAwareCallback(private val originalCallback: Window.Callback,
                                   private val activity: Activity?
) :
    Window.Callback {
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return originalCallback.dispatchKeyEvent(event)
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        return originalCallback.dispatchKeyShortcutEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> activity?.onUserInteraction()
            else -> {}
        }
        return originalCallback.dispatchTouchEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
        return originalCallback.dispatchTrackballEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        return originalCallback.dispatchGenericMotionEvent(event)
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        return originalCallback.dispatchPopulateAccessibilityEvent(event)
    }

    override fun onCreatePanelView(p0: Int): View? {
        return originalCallback.onCreatePanelView(p0)
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        return originalCallback.onCreatePanelMenu(featureId, menu)
    }

    override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
        return originalCallback.onPreparePanel(featureId, view, menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return originalCallback.onMenuOpened(featureId, menu)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        return originalCallback.onMenuItemSelected(featureId, item)
    }

    override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
        originalCallback.onWindowAttributesChanged(attrs)
    }

    override fun onContentChanged() {
        originalCallback.onContentChanged()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        originalCallback.onWindowFocusChanged(hasFocus)
    }

    override fun onAttachedToWindow() {
        originalCallback.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        originalCallback.onDetachedFromWindow()
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        originalCallback.onPanelClosed(featureId, menu)
    }

    override fun onSearchRequested(): Boolean {
        return originalCallback.onSearchRequested()
    }

    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return originalCallback.onSearchRequested(searchEvent)
    }

    override fun onWindowStartingActionMode(p0: ActionMode.Callback?): ActionMode? {
        return p0 as ActionMode
    }

    override fun onWindowStartingActionMode(p0: ActionMode.Callback?, p1: Int): ActionMode? {
        return p0 as ActionMode
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        originalCallback.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        originalCallback.onActionModeFinished(mode)
    }
}

class MultiOrderProductAdapter(
    private val onPlusClicked: (item: MultiOrderProductListWithProduct) -> Unit,
    private val onMinClicked: (item: MultiOrderProductListWithProduct) -> Unit,
) : ListAdapter<MultiOrderProductListWithProduct, MultiOrderProductAdapter.MultiOrderProductViewHolder>(
    DiffCallback
) {

    class MultiOrderProductViewHolder(
        private var binding: MultiOrderProductRvLayoutBinding,
        private val listeners: PlusMinusListeners,
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        fun bind(item: MultiOrderProductListWithProduct) {
            binding.product = item.product
            binding.multiOrder = item.multiOrderProductList
            binding.executePendingBindings()
            binding.productIcon.setImageResource(getProductIcon(item.product.productIcon))
            binding.plusIconButton.setOnClickListener(this)
            binding.minusIconButton.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            when (p0) {
                binding.plusIconButton -> listeners.plus(this.layoutPosition)
                binding.minusIconButton -> listeners.minus(this.layoutPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiOrderProductViewHolder {
        val viewHolder = MultiOrderProductViewHolder(
            MultiOrderProductRvLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            object : PlusMinusListeners {
                override fun plus(pos: Int) {
                    onPlusClicked(getItem(pos))
                }

                override fun minus(pos: Int) {
                    onMinClicked(getItem(pos))
                }
            })
        return viewHolder
    }

    interface PlusMinusListeners {
        fun plus(pos: Int)
        fun minus(pos: Int)
    }

    override fun onBindViewHolder(holder: MultiOrderProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback =
            object : DiffUtil.ItemCallback<MultiOrderProductListWithProduct>() {
                override fun areItemsTheSame(
                    oldItem: MultiOrderProductListWithProduct,
                    newItem: MultiOrderProductListWithProduct,
                ): Boolean {
                    return oldItem.product.stringSortID == newItem.product.stringSortID
                }

                override fun areContentsTheSame(
                    oldItem: MultiOrderProductListWithProduct,
                    newItem: MultiOrderProductListWithProduct,
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}

class MultiOrderOverviewAdapter :
    ListAdapter<MultiOrderProductListWithProduct, MultiOrderOverviewAdapter.MultiOrderOverviewViewHolder>(
        DiffCallback
    ) {
    class MultiOrderOverviewViewHolder(private val binding: MultiOrderOverviewRvLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: MultiOrderProductListWithProduct) {
            binding.item = item
            binding.productCostSum.text = String.format(
                "%.2f",
                item.multiOrderProductList.amount.toDouble() * item.multiOrderProductList.price
            ) + " €"
            binding.executePendingBindings()
        }
    }


    companion object {
        private val DiffCallback =
            object : DiffUtil.ItemCallback<MultiOrderProductListWithProduct>() {
                override fun areItemsTheSame(
                    oldItem: MultiOrderProductListWithProduct,
                    newItem: MultiOrderProductListWithProduct,
                ): Boolean {
                    return oldItem.product.stringSortID == newItem.product.stringSortID
                }

                override fun areContentsTheSame(
                    oldItem: MultiOrderProductListWithProduct,
                    newItem: MultiOrderProductListWithProduct,
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MultiOrderOverviewViewHolder {
        return MultiOrderOverviewViewHolder(
            MultiOrderOverviewRvLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MultiOrderOverviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

fun Bitmap.flip(): Bitmap {
    val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, width / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun getFirstEmoji(string: String): String {
    var newString = ""
    for (c in string) {
        val type = Character.getType(c).toByte()
        Timber.d("EmojiText Type: $type")
        if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
            Timber.d("EmojiText ADDED")
            newString += c
        }
    }
    Timber.d("EmojiText: GetFirstEmoji: $newString")
    return newString
}

fun stringToStringSortID(string: String): String {
    return string.replace("[^A-Za-z0-9 ]".toRegex(), "").lowercase(Locale.getDefault()).trim()
}

fun getProductIcon(productIcon: Int): Int {
    return when (productIcon) {
        product_icon_cup -> R.drawable.ic_round_free_breakfast_24
        product_icon_bottle -> R.drawable.ic_bottle_wine
        product_icon_water -> R.drawable.ic_round_local_drink_24
        product_icon_fastfood -> R.drawable.ic_round_fastfood_24
        product_icon_fridge -> R.drawable.ic_round_kitchen_24
        product_icon_cookie -> R.drawable.ic_round_cookie_24
        product_icon_pizza -> R.drawable.ic_baseline_local_pizza_24
        product_icon_tea -> R.drawable.ic_baseline_emoji_food_beverage_24
        product_icon_egg -> R.drawable.ic_baseline_egg_24
        product_icon_icecream -> R.drawable.ic_baseline_icecream_24
        product_icon_can -> R.drawable.ic_coke_can
        else -> 0
    }
}