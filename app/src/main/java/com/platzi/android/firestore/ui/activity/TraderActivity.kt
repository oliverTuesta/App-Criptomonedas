package com.platzi.android.firestore.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.platzi.android.firestore.R
import com.platzi.android.firestore.adapter.CryptosAdapter
import com.platzi.android.firestore.adapter.CryptosAdapterListener
import com.platzi.android.firestore.model.Crypto
import com.platzi.android.firestore.model.User
import com.platzi.android.firestore.network.Callback
import com.platzi.android.firestore.network.FirestoreService
import com.platzi.android.firestore.network.RealtimeDataListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_trader.*
import java.lang.Exception


/**
 * @author Santiago Carrillo
 * 2/14/19.
 */
class TraderActivity : AppCompatActivity(), CryptosAdapterListener {

    lateinit var firestorService: FirestoreService

    private val cryptosAdapter: CryptosAdapter = CryptosAdapter(this)

    private var username: String? = null

    private var user: User? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trader)
        firestorService = FirestoreService(FirebaseFirestore.getInstance())

        username = intent.extras!![USERNAME_KEY]!!.toString()

        configureRecyclerView()
        loadCryptos()

        fab.setOnClickListener { view ->
            Snackbar.make(view, getString(R.string.generating_new_cryptos), Snackbar.LENGTH_SHORT)
                .setAction("Info", null).show()
            generateCryptoCurrenciRandom()
        }

    }

    private fun generateCryptoCurrenciRandom() {
        for(crypto in cryptosAdapter.cryptosList){
            val amount = (1..10).random()
            crypto.available+=amount
            firestorService.updateCrypto(crypto)
        }
    }

    private fun loadCryptos() {

        firestorService.getCrypto(object : Callback<List<Crypto>> {
            override fun onSuccess(cryptoList: List<Crypto>?) {
                this@TraderActivity.runOnUiThread {

                    firestorService.findUserById(username!!, object : Callback<User> {
                        override fun onSuccess(result: User?) {
                            user = result
                            if (user!!.cryptosList == null) {
                                val userCryptoList = mutableListOf<Crypto>()

                                for (crypto in cryptoList!!) {
                                    val cryptoUser = Crypto()
                                    cryptoUser.name = crypto.name
                                    cryptoUser.available = 0
                                    cryptoUser.imageUrl = crypto.imageUrl
                                    userCryptoList.add(cryptoUser)
                                }
                                user!!.cryptosList = userCryptoList
                                firestorService.updateUser(user!!, null)
                            }

                            loadUserCryptos()
                            addRealTimeDatabaseListeners(user!!, cryptoList!!)

                        }

                        override fun onFail(exception: Exception) {
                            showGeneralServerErrorMessage()
                        }

                    })
                    this@TraderActivity.runOnUiThread {
                        cryptosAdapter.cryptosList = cryptoList!!
                        cryptosAdapter.notifyDataSetChanged()

                    }
                }
            }

            override fun onFail(exception: Exception) {
                Log.e("TraverActivity", "error loading criptos", exception)
                showGeneralServerErrorMessage()
            }
        })
    }

    private fun addRealTimeDatabaseListeners(user: User, cryptosList: List<Crypto>) {

        firestorService.listenForUpdate(user, object : RealtimeDataListener<User> {
            override fun onDataChange(updateData: User) {
                this@TraderActivity.user = updateData
                loadUserCryptos()
            }

            override fun onError(exception: Exception) {
                showGeneralServerErrorMessage()
            }

        })

        firestorService.listenForUpdate(cryptosList, object : RealtimeDataListener<Crypto> {
            override fun onDataChange(updateData: Crypto) {
                var pos = 0
                for (crypto in cryptosAdapter.cryptosList) {
                    if (crypto.name.equals(updateData.name)) {
                        crypto.available = updateData.available
                        cryptosAdapter.notifyItemChanged(pos)
                    }
                    pos++
                }
            }

            override fun onError(exception: Exception) {
                showGeneralServerErrorMessage()
            }

        })

    }

    private fun loadUserCryptos() {
        runOnUiThread {
            if (user != null && user!!.cryptosList != null) {
                infoPanel.removeAllViews()
                for (crypto in user!!.cryptosList!!) {
                    addUserCryptoInfoRow(crypto)
                }
            }
        }
    }

    private fun addUserCryptoInfoRow(crypto: Crypto) {

        findViewById<TextView>(R.id.usernameTextView).text = username

        val view = LayoutInflater.from(this).inflate(R.layout.coin_info, infoPanel, false)
        view.findViewById<TextView>(R.id.coinLabel).text =
            getString(R.string.coin_info, crypto.name, crypto.available.toString())
        Picasso.get().load(crypto.imageUrl).into(view.findViewById<ImageView>(R.id.coinIcon))
        infoPanel.addView(view)
    }

    private fun configureRecyclerView() {
        recyclerView.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = cryptosAdapter

    }

    fun showGeneralServerErrorMessage() {
        Snackbar.make(
            fab,
            getString(R.string.error_while_connecting_to_the_server),
            Snackbar.LENGTH_LONG
        )
            .setAction("Info", null).show()
    }

    override fun onBuyCrypto(crypto: Crypto) {
        var flag = false
        if (crypto.available > 0) {
            for (userCrypto in user!!.cryptosList!!) {
                if (userCrypto.name == crypto.name) {
                    userCrypto.available++
                    flag = true
                    break
                }
            }
            if (!flag) {
                val cryptoUser = Crypto()
                cryptoUser.name = crypto.name
                cryptoUser.available = 1
                cryptoUser.imageUrl = crypto.imageUrl
                user!!.cryptosList = user!!.cryptosList!!.plusElement(cryptoUser)
            }

            crypto.available--

            firestorService.updateUser(user!!, null)
            firestorService.updateCrypto(crypto)
        }
    }
}