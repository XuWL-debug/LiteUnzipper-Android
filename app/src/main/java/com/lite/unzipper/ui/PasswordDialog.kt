package com.lite.unzipper.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lite.unzipper.R

class PasswordDialog : DialogFragment() {
    private var callback: ((String?) -> Unit)? = null

    fun setCallback(cb: (String?) -> Unit) { callback = cb }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_password, null)
        val input = view.findViewById<android.widget.EditText>(R.id.passwordInput)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.password_required)
            .setView(view)
            .setPositiveButton(R.string.confirm) { _, _ -> callback?.invoke(input.text.toString()) }
            .setNegativeButton(R.string.password_skip) { _, _ -> callback?.invoke(null) }
            .setCancelable(false)
            .create()
    }
}
