package org.safeplan.app

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import org.safeplan.compose.SafeplanApp

class MainActivity : ComponentActivity() {
    private var onPhoneContactPicked: ((String, String) -> Unit)? = null

    // ACTION_PICK gives temporary access to the selected phone row; no broad
    // READ_CONTACTS permission is requested.
    private val phoneContactPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = onPhoneContactPicked
        onPhoneContactPicked = null
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        try {
            contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                    val number = if (numberIndex >= 0) cursor.getString(numberIndex).orEmpty() else ""
                    callback?.invoke(name, number)
                }
            }
        } catch (_: SecurityException) {
            // No contact data is saved if the system denies access.
        } catch (_: IllegalArgumentException) {
            // Ignore invalid or unavailable provider data.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme {
                SafeplanApp(
                    repository = EncryptedLocalRepository(applicationContext),
                    pickPhoneContact = { onSelected ->
                        onPhoneContactPicked = onSelected
                        phoneContactPicker.launch(
                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        )
                    }
                )
            }
        }
    }
}
