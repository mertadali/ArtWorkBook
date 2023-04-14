package com.mertadali.artworkbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.mertadali.artworkbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var artList : ArrayList<Art>
    private lateinit var artAdapter: ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)



        artList = ArrayList<Art>()

                             //RecyclerView işlemleri
        artAdapter = ArtAdapter(artList)
        binding.recylerView.layoutManager = LinearLayoutManager(this)
        binding.recylerView.adapter = artAdapter

                                 //(DATA VERİ ÇEKME)
        try {
            val database = this.openOrCreateDatabase("ArtInform", MODE_PRIVATE,null)
            val cursor = database.rawQuery("SELECT * FROM artInform",null)
            val artNameIx = cursor.getColumnIndex("artname")
            val idIx = cursor.getColumnIndex("id")

            while (cursor.moveToNext()){
                val name = cursor.getString(artNameIx)
                val id = cursor.getInt(idIx)
                val art = Art(name,id)
                // Array liste atıp recyclerView da göstereceğiz.
                artList.add(art)
            }
            cursor.close()
            // yeni veriler eklenince adaptore ekleme işlemi için
            artAdapter.notifyDataSetChanged()


        }catch (e: Exception){
            e.printStackTrace()
        }

    }



                              // (MENU)
    // Sağ üst köşede resim ekleyebilmemiz için bir menü oluşturduk. Bunu Res'e ekledik.
                             // (MENUYU MAİN ACTIVITY'E BAGLAMA)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.art_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_art_item){
            val intent = Intent(this@MainActivity,DetailsActivity::class.java)
            /* Main Activityden mi geliyoruz Detail Activitye yoksa Adapterdan mı bilmiyoruz Eğer Mainden geliyorsak
           yeni koyduğumuz resimi Adapterdan geliyorsak eskisini göstermeliyiz.*/
            intent.putExtra("info","new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}