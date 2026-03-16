package de.muenchen.appcenter.nimux.model

import android.os.Parcelable
import de.muenchen.appcenter.nimux.util.stringToStringSortID
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val name: String = "",
    val mail: String = "",
    val toPay: Double = 0.0,
    val stringSortID: String = stringToStringSortID(name),
    val showCredit: Boolean = true,
    val collectData: Boolean = false,
    val pin: String? = null,
    val useProductAI: Boolean? = null,
    val faceSkipsPin: Boolean = false,
    var emojiIcon: String? = null,
    var boldEnabled: Boolean? = null,
    var nameColor: NameColors? = null,
) : Parcelable

@Parcelize
enum class NameColors(val color: Int) : Parcelable {
    DEFAULT(-1),
    PURPLE(0),
    PINK(1),
    ORANGE(2),
    BLUE(3),
    CYAN(4),
}
