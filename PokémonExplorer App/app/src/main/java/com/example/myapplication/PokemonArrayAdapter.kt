package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class PokemonArrayAdapter(
    context: Context,
    private val pokemonList: MutableList<Pokemon>
) : ArrayAdapter<Pokemon>(context, 0, pokemonList) {

    private class ViewHolder(view: View) {
        val imageViewPokemon: ImageView = view.findViewById(R.id.imageViewPokemon)
        val textViewName: TextView = view.findViewById(R.id.textViewName)
        val textViewHp: TextView = view.findViewById(R.id.textViewHp)
        val textViewAttack: TextView = view.findViewById(R.id.textViewAttack)
        val textViewDefense: TextView = view.findViewById(R.id.textViewDefense)
        val textViewSpAttack: TextView = view.findViewById(R.id.textViewSpAttack)
        val textViewSpDefense: TextView = view.findViewById(R.id.textViewSpDefense)
        val textViewSpeed: TextView = view.findViewById(R.id.textViewSpeed)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_pokemon, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val pokemon = pokemonList[position]

        holder.textViewName.text = pokemon.name.replaceFirstChar { it.uppercase() }
        holder.textViewHp.text = "HP: ${pokemon.hp}"
        holder.textViewAttack.text = "Attack: ${pokemon.attack}"
        holder.textViewDefense.text = "Defense: ${pokemon.defense}"
        holder.textViewSpAttack.text = "Special Attack: ${pokemon.specialAttack}"
        holder.textViewSpDefense.text = "Special Defense: ${pokemon.specialDefense}"
        holder.textViewSpeed.text = "Speed: ${pokemon.speed}"

        Glide.with(context)
            .load(pokemon.imageUrl)
            .into(holder.imageViewPokemon)

        return view
    }

    fun updateData(newList: List<Pokemon>) {
        pokemonList.clear()
        pokemonList.addAll(newList)
        notifyDataSetChanged()
    }
}