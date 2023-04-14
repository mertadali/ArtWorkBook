package com.mertadali.artworkbook

import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.mertadali.artworkbook.databinding.ActivityDetailsBinding
import java.io.ByteArrayOutputStream


class DetailsActivity : AppCompatActivity() {

                                  //(INITIALIZE)

    private lateinit var binding:ActivityDetailsBinding
    //(REGISTER)
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>   //ActivityResultLauncher bir sınıftır. Bu sınıf bize veri alıp onunla işlem yapmamıza yarar.
    private lateinit var permissionLauncher: ActivityResultLauncher<String>        //READ_EXTERNAL_STORAGE gibi String tipte bir ifade alacağız.
    var selectedBitmap : Bitmap? = null
    private lateinit var database : SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

         database = this.openOrCreateDatabase("ArtInform", MODE_PRIVATE,null)


        // Launcher sınıfından türettiğimiz  activityResultLauncher'ı ve  permissionLauncherı initiliaze etmek için ve app çökmemesi için çağırdık.
        registerLauncher()


        val intent = intent
        val info = intent.getStringExtra("info")  //Eğer info new gelirse yeni bir şey kaydediyoruz yani Mainden geliyoruz demektir.
        if (info.equals("new")){
            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearText.setText("")
            binding.button.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.icon1)
        }else{            //Old gelirse  eskisini gösteriyor.
            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)
            val cursor = database.rawQuery("SELECT * FROM artInform WHERE id =? ", arrayOf(selectedId.toString()))
            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)

            }
            cursor.close()



        }


    }
    fun saveButtonClicked(view: View){
        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearText.text.toString()
        if (selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            // görseli direkt sqlite kaydedemiyoruz önce byte dizisine  çevirip öyle kaydedebiliriz.
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val convertByteArray = outputStream.toByteArray()

            // (DATABASE)

            try {
                //val database = this.openOrCreateDatabase("ArtInform", MODE_PRIVATE,null)   - (Oncreate altında oluşturduk.)
                database.execSQL("CREATE TABLE IF NOT EXISTS artInform(id INTEGER PRIMARY KEY,artname VARCHAR, artistname VARCHAR,year VARCHAR, image BLOB)")  //BLOB veri kaydetmeye yarayan değişken.

                /*(INSERT INTO) Komutunu bildiğimiz yoldan yapamıyoruz çünkü bilgilerimiz değişkene atandı.Bu sebeple değişkenlere
                bağlamaya yarayan bir method var "Statement".Burada index 1 den başlar. SQL de hemen çalıştırılmaz önce bağlama işlemi yapılır. */
                val sqlString = "INSERT INTO artInform (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,convertByteArray)
                statement.execute()


            }catch (e: Exception){
                e.printStackTrace()
            }
            // Veri kaydetme işlemi bittikten sonra MainActivitye dönmek istersem arkada Details Activity açık kalacak bunu kapatıp Ana aktivitemize dönmek için.
            val intent = Intent(this@DetailsActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)     // Arkadaki uygulamaları kapatır.
            startActivity(intent)



        }


    }


    /* Save butonuna tıklandığında ne olacağını yazdığımız kısım. Ancak sqlite da 1 mb aşan bir room oluşturamıyoruz. Görselleri
     bir cloud sunucuda tutmak daha mantıklı ancak bazen kullanıcının interneti olmayabilir ve bu şekilde uygulama silindiğinde resimler silinmeyecek.
      bu yüzden bir bitmap kullanacağız bu şekilde görseli obje olarak tutacak ve var olan büyüklüğünü sınırlandıracağız.*/

    // Kullanıcının yatay mı dikey mi kullanacağını bilmediğimiz bir fonksiyon yazacağız.

    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int) : Bitmap{
        var width = image.width
        var height = image.height
        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if (bitmapRatio > 1){
            // landscape- Yatay bir görseldir.
            width = maximumSize
            val changedHeight = width / bitmapRatio
            height = changedHeight.toInt()
        }else{
            // portrait - Dikey bir görseldir.
            height = maximumSize
            val changedWidth = height * bitmapRatio
            width = changedWidth.toInt()



        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }


    //(IMAGEVİEW)

    /*Manifest.xml kısmına uses-permission ekledik. Galeriye ulaşmak istediğimiz
        için Dangerous bir manifeste sahip o halde izinleri kontrol edeceğiz.*/

    fun selectImage(view: View){


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        //  (Android 33+ -> READ_MEDIA_IMAGES)
            //(EĞER İZİN VERİLMEDİYSE)


            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){

              //- 2021 Sonrası çıkan bir yenilikle android kullanıcıya izin alma mantığını sorması için kullanacağımız ifade
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_MEDIA_IMAGES)){
                    //rationale
                    // Indefinite -  Belirsiz süre anlamında.
                    Snackbar.make(view,"Permission needs for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", View.OnClickListener {
                        //request permission
                        permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)

                    }).show()

                }else{
                    // request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }




            }else{
              // (İzin verildiyse medyadan bir resim alacağız.)
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }



        }else{
            // (Android 32- ->READ_EXTERNAL_STORAGE)
            //(EĞER İZİN VERİLMEDİYSE)

            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //- 2021 Sonrası çıkan bir yenilikle android kullanıcıya izin alma mantığını sorması için kullanacağımız ifade
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)){

                    //rationale
                    // Indefinite -  Belirsiz süre anlamında.
                    Snackbar.make(view,"Permission needs for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                        View.OnClickListener {
                        //request permission
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)

                    }).show()

                }else{
                    // request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }




            }else{
               // (İzin verildiyse medyadan bir resim alacağız.)
                val intentToGalery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGalery)

            }



        }



    }
    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode== RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null){
                    val imageUri = intentFromResult.data
                   // binding.imageView.setImageURI(imageUri)
                    // (HATALI İŞLEMDE APP ÇÖKMEMESİ İÇİN)
                    // URİ veriye çevireceğiz

                    try {
                            // Seçilen görsel ImageViewda gösterme işlemi.
                            if (Build.VERSION.SDK_INT >= 28){
                                val source = ImageDecoder.createSource(this@DetailsActivity.contentResolver, imageUri!!)//Bu ifade min 28sdk de çalışıyor o halde sdk kontrol etmemiz lazım.
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }else{
                                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver,imageUri)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }


                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                }
            }

        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result){ //permission granted
                val intentToGalery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGalery)

            }else{   // permission denied
                Toast.makeText(this@DetailsActivity,"Permission needed!!",Toast.LENGTH_LONG).show()
            }
        }


    }
}
