package com.iamconanpeter.kidsblockbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.iamconanpeter.kidsblockbuddy.ui.KidsBlockBuddyApp
import com.iamconanpeter.kidsblockbuddy.ui.theme.KidsBlockBuddyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KidsBlockBuddyTheme {
                Surface {
                    KidsBlockBuddyApp()
                }
            }
        }
    }
}
