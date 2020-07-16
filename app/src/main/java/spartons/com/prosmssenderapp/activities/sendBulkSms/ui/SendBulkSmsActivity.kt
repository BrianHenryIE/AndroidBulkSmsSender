package spartons.com.prosmssenderapp.activities.sendBulkSms.ui

import FileUriUtils
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.FileUtils
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_send_bulk_sms.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import spartons.com.prosmssenderapp.R
import spartons.com.prosmssenderapp.activities.sendBulkSms.adapter.PreviewContactAdapter
import spartons.com.prosmssenderapp.activities.sendBulkSms.viewModel.SendBulkSmsViewModel
import spartons.com.prosmssenderapp.helper.PICK_CSV_FILE
import spartons.com.prosmssenderapp.helper.SharedPreferenceHelper
import spartons.com.prosmssenderapp.helper.SharedPreferenceHelper.Companion.BULK_SMS_PREFERRED_CARRIER_NUMBER
import spartons.com.prosmssenderapp.helper.SharedPreferenceHelper.Companion.BULK_SMS_PREFERRED_MULTIPLE_CARRIER_NUMBER_FLAG
import spartons.com.prosmssenderapp.helper.UiHelper
import spartons.com.prosmssenderapp.util.*
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.commons.io.IOUtils

/**
 * Ahsen Saeed}
 * ahsansaeed067@gmail.com}
 * 6/25/19}
 */

@SuppressLint("NewApi")
class SendBulkSmsActivity : AppCompatActivity(){

    private companion object {
        private const val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 1000
        private const val ASK_SMS_PERMISSION_REQUEST_CODE = 1001
        private const val BULK_SMS_SENT_LIMIT = 1000
        private const val ASK_READ_PHONE_STATE_PERMISSION_REQUEST_CODE = 1002
    }

    private val viewModel: SendBulkSmsViewModel by viewModel()
    private val uiHelper: UiHelper by inject()
    private val sharedPreferenceHelper: SharedPreferenceHelper by inject()
    private val previewAdapter = PreviewContactAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_bulk_sms)
        sendBulkSmsToolbar.title = getResourceString(R.string.send_bulk_sms)
        setSupportActionBar(sendBulkSmsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        /**
         * If the app has the read_phone_state permission then read all the carrier numbers used by this device. Else show a dialog why app needs this permission and simply ask for it.
         */

        if (isHasPermission(Manifest.permission.READ_PHONE_STATE))
            viewModel.handleDeviceCarrierNumbers()
        else uiHelper.showSimpleMaterialDialog(
            this, R.string.allow_following_permission,
            R.string.read_phone_state_permission_content, null,
            "Grant Permission", false, "Deny"
            , negativeTextButtonListener = { finish() }
        ) {
            askPermission(
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                ASK_READ_PHONE_STATE_PERMISSION_REQUEST_CODE
            )
        }

        /**
         *
         */

        sendBulkSmsChooseFileButton.setOnClickListener {
            if (isHasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                showFileSelectorDialog()
            else
                askPermission(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
                )
        }

        /**
         * Initializing the Contact list recycler view and setting default adapter.
         */

        sendBulkSmsAllPhoneNumberRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SendBulkSmsActivity)
            setHasFixedSize(true)
            adapter = previewAdapter
        }

        /**
         * handle events came from SendBulkSmsViewModel class.
         */

        viewModel.uiState.observe(this, Observer {
            val uiModel = it ?: return@Observer
            sendBulkSmsProgressBar.visibility = if (uiModel.showProgress) VISIBLE else GONE
            if (uiModel.contactMessageList != null && !uiModel.contactMessageList.consumed)
                uiModel.contactMessageList.consume()?.let { contactList ->
                    sendBulkSmsChooseFileButton.text =
                        getResourceString(R.string.add_another_text_file)
                    previewAdapter.submitList(contactList)
                }
            if (uiModel.showMessage != null && !uiModel.showMessage.consumed)
                uiModel.showMessage.consume()?.let { messageResource ->
                    snackbar(
                        sendBulkSmsActivityRootView, getResourceString(messageResource)
                    )
                }
            if (uiModel.noDeviceNumber) {
                val drawable = getMutedDrawable(R.drawable.red_error_vector_icon)
                drawable?.applyColor(getMutedColor(R.color.colorPrimary))
                uiHelper.showSimpleMaterialDialog(
                    this, R.string.carrier_number,
                    R.string.no_carrier_number_found_content,
                    cancelable = false, drawable = drawable
                ) { finish() }
            }
            if (uiModel.showMultipleCarrierNumber != null && !uiModel.showMultipleCarrierNumber.consumed)
                uiModel.showMultipleCarrierNumber.consume()?.let { carrierNumbers ->
                    uiHelper.showSingleSelectionListDialog(
                        this,
                        R.string.please_select_from_which_carrier_number_you_want_to_send_bulk_sms,
                        choices = carrierNumbers
                    ) { (number, checkedValue) ->
                        sharedPreferenceHelper.put(BULK_SMS_PREFERRED_CARRIER_NUMBER, number)
                        sharedPreferenceHelper.put(
                            BULK_SMS_PREFERRED_MULTIPLE_CARRIER_NUMBER_FLAG, checkedValue
                        )
                    }
                }
        })

        /**
         * handles the click listener for send bulk sms image view.
         */

        sendBulkSmsImageView.setOnClickListener {
            if (previewAdapter.itemCount == 0) {
                snackbar(
                    sendBulkSmsActivityRootView,
                    getResourceString(R.string.please_first_select_contact_list)
                )
                return@setOnClickListener
            }
            if (isHasPermission(Manifest.permission.SEND_SMS)) sendBulkSms()
            else
                askPermission(
                    arrayOf(Manifest.permission.SEND_SMS),
                    ASK_SMS_PERMISSION_REQUEST_CODE
                )
        }

    }

    private fun sendBulkSms() {
        val currentContacts = previewAdapter.currentList
        when {
            currentContacts.count() > BULK_SMS_SENT_LIMIT ->
                snackbar(
                    sendBulkSmsActivityRootView,
                    getResourceString(R.string.limit_exceeds_please_send_bulk_sms_less_than_thousand_contacts)
                )
            viewModel.checkIfWorkerIsIdle() -> {
                viewModel.sendBulkSms(
                    currentContacts.toTypedArray()
                )
                uiHelper.showSimpleMaterialDialog(
                    this, R.string.bulk_sms_sent,
                    R.string.sms_sent_successfully_content,
                    cancelable = false, icon = R.drawable.tick_image_icon
                ) { finish() }
            }
            else -> snackbar(
                sendBulkSmsActivityRootView,
                getResourceString(R.string.a_service_already_running_for_sending_bulk_sms_please_wait_until_it_finishes)
            )
        }
    }

    private fun showFileSelectorDialog() {
        showSelectContactFileDialog(this)
        //{ selectedFile ->
        //    viewModel.handleSelectedFile(selectedFile)
        //}
    }

    private fun showSelectContactFileDialog(activity: Activity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
        }

        ActivityCompat.startActivityForResult(activity, intent, PICK_CSV_FILE, null)
    }

     override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
         super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_CSV_FILE
            && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                var path = FileUriUtils(this.baseContext).getPath(uri)
                var fileName = path.substring(path.lastIndexOf("/"));

                val parcelFileDescriptor = this.baseContext.contentResolver.openFileDescriptor(uri, "r", null)

                parcelFileDescriptor?.let {
                    val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                    val file = File(this.baseContext.cacheDir, fileName)
                    val outputStream = FileOutputStream(file)
                    IOUtils.copy(inputStream, outputStream)
                    viewModel.handleSelectedFile(file)
                }
                // Perform operations on the document using its URI.
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ASK_SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                sendBulkSms()
            else
                showBottomSheetDialog(
                    R.string.sms_permission_denied,
                    R.string.sms_permission_denied_content
                ) {
                    askPermission(
                        arrayOf(Manifest.permission.SEND_SMS),
                        ASK_SMS_PERMISSION_REQUEST_CODE
                    )
                }
        } else if (requestCode == READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                showFileSelectorDialog()
            else
                showBottomSheetDialog(
                    R.string.storage_permission_denied,
                    R.string.storage_permission_denied_content
                ) {
                    askPermission(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
                    )
                }
        } else if (requestCode == ASK_READ_PHONE_STATE_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                viewModel.handleDeviceCarrierNumbers()
            else
                uiHelper.showBottomSheetDialog(
                    this,
                    R.string.phone_state_permission_denied_content,
                    R.string.phone_state_permission_denied,
                    "Ask Permission",
                    negativeTextButtonListener = { finish() }) {
                    askPermission(
                        arrayOf(Manifest.permission.READ_PHONE_STATE),
                        ASK_READ_PHONE_STATE_PERMISSION_REQUEST_CODE
                    )
                }
        }
    }

    private fun showBottomSheetDialog(
        @StringRes titleResource: Int, @StringRes contentRes: Int, closure: () -> Unit
    ) {
        uiHelper.showBottomSheetDialog(
            this, contentRes, titleResource,
            positiveButtonText = "Ask Permission"
        ) { closure.invoke() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}