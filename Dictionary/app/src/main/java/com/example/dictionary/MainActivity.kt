package com.example.dictionary

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dictionary.adapter.WordAdapter
import com.example.dictionary.model.DictionaryWord
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL

class MainActivity : AppCompatActivity() {
    lateinit var etSearch : EditText
    lateinit var btSearch : Button
    lateinit var rvMain : RecyclerView
    lateinit var wordAdapter: WordAdapter
    var list = ArrayList<DictionaryWord>()
    var search =""
     val progressDialog by lazy { ProgressDialog(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkInternet()

        etSearch = findViewById(R.id.etSearch)
        btSearch = findViewById(R.id.btSearch)
        rvMain = findViewById(R.id.rvMain)

        wordAdapter = WordAdapter(this , list)
        rvMain.adapter = wordAdapter
        rvMain.layoutManager = LinearLayoutManager(this)

        btSearch.setOnClickListener {
             search = etSearch.text.toString()

            if (search.isNotEmpty()){
                requestApi()
            }else {
                Toast.makeText(this , "Enter a Search name" , Toast.LENGTH_SHORT).show()
            }
            etSearch.text.clear()
        }
    }

    private fun requestApi() {

        CoroutineScope(Dispatchers.IO).launch {

            withContext(Dispatchers.Main) {

                progressDialog.setMessage("Please wait")
                progressDialog.show()

            }
                val data = async {

                    fetchData()

                }.await()

                if (data.isNotEmpty()) {

                    updateData(data)
                }
        }
    }

    private suspend fun fetchData():String{

        var response=""
        try {
            response = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$search").readText(Charsets.UTF_8)

        }catch (e:Exception)
        {

            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(applicationContext , "word is invalid" , Toast.LENGTH_SHORT).show()

            }
            println("Error $e")

        }
        return response

    }

    private suspend fun updateData(data:String) {
        withContext(Dispatchers.Main) {
            progressDialog.dismiss()
            val jsonArray = JSONArray(data)
            val jsonObject = jsonArray.getJSONObject(0)
            val word = jsonObject.getString("word")
            val meaning = jsonObject.getJSONArray("meanings")
            val definition = meaning.getJSONObject(0).getJSONArray("definitions").getJSONObject(0).getString("definition")
            var synonyms = meaning.getJSONObject(0).getJSONArray("definitions").getJSONObject(0).getJSONArray("synonyms")

            // Log.d("Array" , "$synonyms")
            for ( i in 0 until  synonyms.length()){
                Log.d("Array" , "${synonyms[i].toString()}")
            }

            list.add(DictionaryWord(word,definition))

           // rvMain.adapter!!.notifyDataSetChanged()

            wordAdapter.update(list)


        }

    }

    private fun checkInternet(){
        if(!connectedToInternet()){
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Internet Connection Not Found")
                .setPositiveButton("RETRY"){_, _ -> checkInternet()}
                .show()
        }
    }

    private fun connectedToInternet(): Boolean{
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

}