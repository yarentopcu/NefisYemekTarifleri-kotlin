package com.yt.nefisyemektarifleri.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.yt.nefisyemektarifleri.databinding.FragmentTarifBinding
import com.yt.nefisyemektarifleri.model.Tarif
import com.yt.nefisyemektarifleri.roomdb.TarifDAO
import com.yt.nefisyemektarifleri.roomdb.TarifDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.IOException

private lateinit var permissionLauncher:ActivityResultLauncher<String>
private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
private var secilenGorsel: Uri?=null
private var secilenBitmap: Bitmap?=  null
private val mDisposable = CompositeDisposable()
private var secilenTarif : Tarif? = null
private  lateinit var  db: TarifDatabase
private lateinit var tarifDao: TarifDAO

class TarifFragment : Fragment() {
    private var _binding: FragmentTarifBinding?= null
    private val binding get()=_binding!!





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db= Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler").build()
        tarifDao = db.tarifDao()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentTarifBinding.inflate(inflater,container,false)
        val view=binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageView.setOnClickListener{gorselSec(it)}
        binding.kaydetButton.setOnClickListener{kaydet(it)}
        binding.silButton.setOnClickListener{sil(it)}

        arguments?.let{
            val bilgi=TarifFragmentArgs.fromBundle(it).bilgi
            if(bilgi=="yeni"){
                secilenTarif = null
                binding.silButton.isEnabled=false
                binding.kaydetButton.isEnabled=true
                binding.isimText.setText("")
                binding.malzemeText.setText("")

            }
            else{
                binding.silButton.isEnabled=true
                binding.kaydetButton.isEnabled=false
                val id= TarifFragmentArgs.fromBundle(it).id

                mDisposable.add(
                    tarifDao.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponse)
                )


            }
        }


    }
    private fun handleResponse(tarif:Tarif){
        secilenTarif =tarif
        binding.isimText.setText(tarif.isim)
        binding.malzemeText.setText(tarif.malzeme)
        val byteDizisi=tarif.gorsel
        val bitmap= BitmapFactory.decodeByteArray(byteDizisi,0,byteDizisi.size)
        binding.imageView.setImageBitmap(bitmap)


    }

    fun kaydet(view:View){
        val isim =binding.isimText.text.toString()
        val malzeme= binding.malzemeText.text.toString()

        if(secilenBitmap!= null){

            val kucukBitmap= kucukBitmapOlustur(secilenBitmap!! , 300)
            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi= outputStream.toByteArray()

            val tarif = Tarif(isim=isim,malzeme=malzeme, gorsel = byteDizisi)

            mDisposable.add(
                tarifDao.insert(tarif)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponseForInsert)  )


        }


    }
    private fun handleResponseForInsert(){
        val action=TarifFragmentDirections.actionTarifFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)

    }
    fun sil(view: View){
        if(secilenTarif!= null){
            mDisposable.add(
                tarifDao.delete(tarif=secilenTarif!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )

        }



    }
    fun gorselSec(view: View){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_MEDIA_IMAGES)){
                    Snackbar.make(view,"yemek resmi yüklemek için galeriye erişim gereklidir!",Snackbar.LENGTH_INDEFINITE).setAction(
                        "izin ver",
                        View.OnClickListener{
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)

                        }
                    ).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)

                }
        }else{
            val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)

        }

        }else{
            if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view,"yemek resmi yüklemek için galeriye erişim gereklidir!",Snackbar.LENGTH_INDEFINITE).setAction(
                        "izin ver",
                        View.OnClickListener{
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                        }
                    ).show()
                }else{
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                }
            }else{
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }

        }



    }
    private fun registerLauncher(){
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
            if(result.resultCode==AppCompatActivity.RESULT_OK){
                val intentFromResult=result.data
                if(intentFromResult!= null){
                    secilenGorsel=intentFromResult.data
                    try{
                        if(Build.VERSION.SDK_INT>=28){
                            val source=ImageDecoder.createSource(requireActivity().contentResolver,secilenGorsel!!)
                            secilenBitmap=ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(secilenBitmap)
                        }else{
                            secilenBitmap= MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,secilenGorsel)
                            binding.imageView.setImageBitmap((secilenBitmap))
                        }
                    }catch (e:IOException){
                        println(e.localizedMessage)
                    }





                }

            }
        }
        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
            result->
            if(result){
                val intentToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{
                Toast.makeText(requireContext(),"izin verilmedi",Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun kucukBitmapOlustur( kullanicininSectigiBitmap : Bitmap, maximumBoyut : Int ): Bitmap{
        var width=kullanicininSectigiBitmap.width
        var height = kullanicininSectigiBitmap.height

        val bitmapOrani : Double = width.toDouble() / height.toDouble()

        if(bitmapOrani>1){
            width=maximumBoyut
            val kisaltilmisYukseklik =width/bitmapOrani
            height=kisaltilmisYukseklik.toInt()
        }else{
            height=maximumBoyut
            val kisaltilmisGenislik = height * bitmapOrani
            width=kisaltilmisGenislik.toInt()

        }

        return Bitmap.createScaledBitmap(kullanicininSectigiBitmap,width,height,true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
        mDisposable.clear()
    }


}