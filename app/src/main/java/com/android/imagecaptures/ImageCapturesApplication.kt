package com.android.imagecaptures

import android.app.Application
import com.google.firebase.auth.FirebaseUser

class ImageCapturesApplication : Application() {
    companion object {
        const val PATH_SNAPSHOTS = "imageCaptures"
        const val PROPERTY_LIKE_LIST = "likeList"

        lateinit var currentUser: FirebaseUser
    }
}