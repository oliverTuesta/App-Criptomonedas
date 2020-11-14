package com.platzi.android.firestore.network

import com.google.firebase.firestore.FirebaseFirestore
import com.platzi.android.firestore.model.Crypto
import com.platzi.android.firestore.model.User


const val CRYPTO_COLLECTION_NAME: String = "cryptos"
const val USER_COLLECTION_NAME: String = "users"

class FirestoreService (val firebaseFirestore: FirebaseFirestore) {

    fun setDocument(data: Any, collectionName: String, id: String, callback: Callback<Void>){

        firebaseFirestore.collection(collectionName).document(id).set(data)
            .addOnSuccessListener { callback.onSuccess(null) }
            .addOnFailureListener{ exception -> callback.onFail(exception)}

    }

    fun updateUser(user: User, callback: Callback<User>?){

        firebaseFirestore.collection(USER_COLLECTION_NAME).document(user.username.toLowerCase())
            .update("cryptosList", user.cryptosList)
            .addOnSuccessListener { result ->
                if (callback != null){
                    callback.onSuccess(user)
                }
            }
                //!! ES PARA VALIDAR LOS VALORES NULOS
            .addOnFailureListener{ exception -> callback!!.onFail(exception) }
    }

    fun updateCrypto(crypto: Crypto){
        firebaseFirestore.collection(CRYPTO_COLLECTION_NAME).document(crypto.getDocumentId())
            .update("available", crypto.available)
    }

    fun getCrypto(callback: Callback<List<Crypto>>?){
        firebaseFirestore.collection(CRYPTO_COLLECTION_NAME).get()
            .addOnSuccessListener { result ->
                for (document in result){
                    //
                    val cryptoList = result.toObjects(Crypto::class.java)
                    callback!!.onSuccess(cryptoList)
                    break
                }
            }
            .addOnFailureListener{exception -> callback!!.onFail(exception)}
    }

    fun findUserById(id: String, callback: Callback<User>){
        firebaseFirestore.collection(USER_COLLECTION_NAME).document(id)
            .get()
            .addOnSuccessListener { result ->
                if (result.data != null){
                    callback.onSuccess(result.toObject(User::class.java))
                }else{
                    callback.onSuccess(null)
                }
            }
            .addOnFailureListener{exception -> callback.onFail(exception)}
    }

    fun listenForUpdate(cryptos:List<Crypto>, listener: RealtimeDataListener<Crypto>){
        val cryptoReference = firebaseFirestore.collection(CRYPTO_COLLECTION_NAME)
        for(crypto in cryptos){
            cryptoReference.document(crypto.getDocumentId()).addSnapshotListener{snapshot, e ->
                if(e != null){
                    listener.onError(e)
                }
                if (snapshot != null && snapshot.exists()){
                    listener.onDataChange(snapshot.toObject(Crypto::class.java)!!)
                }
            }
        }
    }
    fun listenForUpdate(user: User, listener: RealtimeDataListener<User>){
        val userReference = firebaseFirestore.collection(USER_COLLECTION_NAME)

        userReference.document(user.username).addSnapshotListener{snapshot, e ->
            if(e != null){
                listener.onError(e)
            }
            if (snapshot != null && snapshot.exists()){
                listener.onDataChange(snapshot.toObject(User::class.java)!!)
            }
        }
    }

}