package com.tuempresa.inventariovial.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** UI input only, retained in memory across rotation; never put secrets in saved state. */
internal class LoginFormInputs : ViewModel() {
    var username by mutableStateOf("")
    var pin by mutableStateOf("")
    var url by mutableStateOf("")
    var token by mutableStateOf("")
    var rotateToken by mutableStateOf(false)

    fun clear() {
        username = ""
        pin = ""
        url = ""
        token = ""
        rotateToken = false
    }

    override fun onCleared() { clear() }
}

@Composable
internal fun rememberLoginFormInputs(lockedUsername: String?): LoginFormInputs {
    val context = LocalContext.current
    val activity = remember(context) { context.authActivity() }
    val inputs = remember(activity) {
        if (activity != null) ViewModelProvider(activity)[LoginFormInputs::class.java] else LoginFormInputs()
    }
    LaunchedEffect(lockedUsername) {
        if (lockedUsername != null) inputs.username = lockedUsername
    }
    DisposableEffect(inputs, activity) {
        onDispose {
            // A configuration change retains only this ViewModel's memory. Leaving access
            // (including successful login) clears it; process death never restores a secret.
            if (activity?.isChangingConfigurations != true) inputs.clear()
        }
    }
    return inputs
}

private fun Context.authActivity(): ComponentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        val next = current.baseContext
        if (next === current) return null
        current = next
    }
    return current as? ComponentActivity
}
