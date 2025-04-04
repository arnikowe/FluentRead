package com.example.fluentread.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

open class UserViewModel : ViewModel() {
    open val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
    open val db: FirebaseFirestore = FirebaseFirestore.getInstance()
}
