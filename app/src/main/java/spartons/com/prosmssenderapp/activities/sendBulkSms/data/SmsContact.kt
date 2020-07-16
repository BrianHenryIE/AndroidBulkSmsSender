package spartons.com.prosmssenderapp.activities.sendBulkSms.data

import kotlinx.serialization.Serializable


/**
 * Ahsen Saeed}
 * ahsansaeed067@gmail.com}
 * 10/30/19}
 */

fun toSmsContactMessage(contact:String, message: String) =
    SmsContactMessage(contact, message)

@Serializable
data class SmsContactMessage(
    val contactNumber: String,
    val message: String,
    var isSent: Boolean = false
)
