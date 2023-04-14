package com.mertadali.artworkbook

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mertadali.artworkbook.databinding.RecyclerRowBinding

// Kullanıcıya RecyclerView da verileri göstermek için oluşturduğumuz sınıf.
class ArtAdapter(val artList: ArrayList<Art>) : RecyclerView.Adapter<ArtAdapter.ArtHolder>() {
    class ArtHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ArtHolder(binding)

    }

    override fun getItemCount(): Int {
        return artList.size

    }

    override fun onBindViewHolder(holder: ArtHolder, position: Int) {
        holder.binding.recyclerViewText.text = artList.get(position).name
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context,DetailsActivity::class.java)
            /* Main Activityden mi geliyoruz Detail Activitye yoksa Adapterdan mı bilmiyoruz Eğer Mainden geliyorsak
            yeni koyduğumuz resimi Adapterdan geliyorsak eskisini göstermeliyiz.*/
            intent.putExtra("info","old")
            intent.putExtra("id",artList.get(position).id)
            holder.itemView.context.startActivity(intent)
        }


    }

}