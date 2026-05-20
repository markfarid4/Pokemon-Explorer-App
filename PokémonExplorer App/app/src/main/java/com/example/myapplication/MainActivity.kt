package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var editTextType: TextInputEditText
    private lateinit var listViewPokemon: ListView
    private lateinit var fabSearch: FloatingActionButton
    private lateinit var adapter: PokemonArrayAdapter

    private val searchChannel = Channel<String>(Channel.CONFLATED)
    private val pokemonList = mutableListOf<Pokemon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        editTextType = findViewById(R.id.editTextType)
        listViewPokemon = findViewById(R.id.listViewPokemon)
        fabSearch = findViewById(R.id.fabSearch)

        adapter = PokemonArrayAdapter(this, pokemonList)
        listViewPokemon.adapter = adapter

        fabSearch.setOnClickListener {
            hideKeyboard()

            val type = editTextType.text.toString().trim().lowercase()

            if (type.isEmpty()) {
                showSnackbar(getString(R.string.error_empty))
            } else {
                lifecycleScope.launch {
                    searchChannel.send(type)
                }
            }
        }

        lifecycleScope.launch {
            searchChannel
                .consumeAsFlow()
                .debounce(300)
                .distinctUntilChanged()
                .collect { type ->
                    fetchAndDisplayPokemon(type)
                }
        }
    }

    private fun fetchAndDisplayPokemon(type: String) {
        lifecycleScope.launch {
            try {
                val pokemonData = withContext(Dispatchers.IO) {
                    fetchPokemonByType(type)
                }
                adapter.updateData(pokemonData)
            } catch (e: Exception) {
                showSnackbar(e.message ?: getString(R.string.error_fetch))
            }
        }
    }

    private fun fetchPokemonByType(type: String): List<Pokemon> {
        val typeUrl = URL("https://pokeapi.co/api/v2/type/$type")
        val typeConnection = typeUrl.openConnection() as HttpURLConnection
        typeConnection.requestMethod = "GET"
        typeConnection.connectTimeout = 10000
        typeConnection.readTimeout = 10000

        if (typeConnection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception(getString(R.string.error_invalid))
        }

        val typeResponse = typeConnection.inputStream.bufferedReader().use { it.readText() }
        val typeJson = JSONObject(typeResponse)
        val pokemonArray = typeJson.getJSONArray("pokemon")

        val result = mutableListOf<Pokemon>()
        val limit = minOf(20, pokemonArray.length())

        for (i in 0 until limit) {
            val pokemonEntry = pokemonArray.getJSONObject(i).getJSONObject("pokemon")
            val pokemonName = pokemonEntry.getString("name")
            result.add(fetchPokemonDetails(pokemonName))
        }

        return result
    }

    private fun fetchPokemonDetails(name: String): Pokemon {
        val pokemonUrl = URL("https://pokeapi.co/api/v2/pokemon/$name")
        val pokemonConnection = pokemonUrl.openConnection() as HttpURLConnection
        pokemonConnection.requestMethod = "GET"
        pokemonConnection.connectTimeout = 10000
        pokemonConnection.readTimeout = 10000

        if (pokemonConnection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception(getString(R.string.error_fetch))
        }

        val response = pokemonConnection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)

        val sprites = json.getJSONObject("sprites")
        val imageUrl = sprites.getString("front_default")

        val statsArray = json.getJSONArray("stats")

        var hp = 0
        var attack = 0
        var defense = 0
        var specialAttack = 0
        var specialDefense = 0
        var speed = 0

        for (i in 0 until statsArray.length()) {
            val statObject = statsArray.getJSONObject(i)
            val baseStat = statObject.getInt("base_stat")
            val statName = statObject.getJSONObject("stat").getString("name")

            when (statName) {
                "hp" -> hp = baseStat
                "attack" -> attack = baseStat
                "defense" -> defense = baseStat
                "special-attack" -> specialAttack = baseStat
                "special-defense" -> specialDefense = baseStat
                "speed" -> speed = baseStat
            }
        }

        return Pokemon(
            name = name,
            imageUrl = imageUrl,
            hp = hp,
            attack = attack,
            defense = defense,
            specialAttack = specialAttack,
            specialDefense = specialDefense,
            speed = speed
        )
    }

    private fun hideKeyboard() {
        val view = currentFocus ?: View(this)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
}