
package gas.trulala.servergo

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import android.net.Uri
import android.content.Intent
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var fileYangDipilih: File? = null
    private var modePotong: Boolean = false
    private var folderSaatIni: File? = null
    private var server: FileServer? = null
    private var userCount: Int = 0
    private val REQUEST_CODE_FILE = 100
    private val REQUEST_CODE_FOLDER = 101
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val liner = findViewById<LinearLayout>(R.id.liner)
        val grid = findViewById<GridLayout>(R.id.grid)
        val kembali = findViewById<ImageView>(R.id.kembali)
        val folderBaru = findViewById<ImageView>(R.id.folderBaru)
        val fileBaru = findViewById<ImageView>(R.id.fileBaru)
        val tekanLama = findViewById<LinearLayout>(R.id.lama)
        val hapus = findViewById<ImageView>(R.id.hapus)
        val potong = findViewById<ImageView>(R.id.potong)
        val salin = findViewById<ImageView>(R.id.salin)
        val paste = findViewById<ImageView>(R.id.tempel)
        
        val pengguna = findViewById<TextView>(R.id.pengguna)
        val alamat = findViewById<TextView>(R.id.alamat)
        val mulaiHost = findViewById<Switch>(R.id.mulaiServer)
        
        mulaiHost.setOnCheckedChangeListener { _, isChecked ->
    if (isChecked) {
        if (server == null) {
            try {
                server = FileServer()
                server?.start()
                val ip = getPublicIp() 
                alamat.text = "http://$ip:8080"
            } catch (e: Exception) {
                alamat.text = "Gagal memulai server"
                server = null
            }
        }
    } else {
        server?.stop()
        server = null
        alamat.text = "Server dimatikan"
    }
}
        
        folderSaatIni = filesDir
        
        val member = File(filesDir, "member")
        val logika = File(filesDir, "logika")

        if (!member.exists()) { member.mkdirs() }
        if (!logika.exists()) { logika.mkdirs() }


        findViewById<ImageView>(R.id.nav).setOnClickListener {
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE}

        fun tampilkanFile(directory: File) {
            folderSaatIni = directory
            grid.removeAllViews()

            kembali.visibility = if (directory != filesDir) View.VISIBLE else View.GONE

            directory.listFiles()?.forEach { file ->
                val item = LayoutInflater.from(this).inflate(R.layout.item_vertical, grid, false)
                val gambar = item.findViewById<ImageView>(R.id.gambar)
                val nama = item.findViewById<TextView>(R.id.nama)

                nama.text = file.name
                gambar.setImageResource(if (file.isDirectory) R.drawable.folder else R.drawable.file)

                item.setOnClickListener {
    if (file.isDirectory) { 
        tampilkanFile(file) 
    } else { 
        val intent = Intent(this, Editor::class.java)
        intent.putExtra("file_path", file.absolutePath)
        startActivity(intent)
    }
}

                item.setOnLongClickListener {
                    tekanLama.visibility = View.VISIBLE
                    fileYangDipilih = file
                    true
                }

                grid.addView(item)
            }
        }

        tampilkanFile(filesDir)

        kembali.setOnClickListener {
            folderSaatIni?.parentFile?.let { parent ->
                tampilkanFile(parent)
            }
        }

        folderBaru.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.item_tulis, null)
            val tulis = view.findViewById<EditText>(R.id.tulis)

            AlertDialog.Builder(this)
                .setTitle("Buat Folder Baru")
                .setView(view)
                .setPositiveButton("Buat") { _, _ ->
                    val namaFolder = tulis.text.toString()
                    if (namaFolder.isNotEmpty()) {
                        val folderBaru = File(folderSaatIni, namaFolder)
                        if (!folderBaru.exists()) folderBaru.mkdirs()
                        tampilkanFile(folderSaatIni!!)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        fileBaru.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.item_tulis, null)
            val tulis = view.findViewById<EditText>(R.id.tulis)

            AlertDialog.Builder(this)
                .setTitle("Buat File Baru")
                .setView(view)
                .setPositiveButton("Buat") { _, _ ->
                    val namaFile = tulis.text.toString()
                    if (namaFile.isNotEmpty()) {
                        val fileBaru = File(folderSaatIni, namaFile)
                        if (!fileBaru.exists()) fileBaru.createNewFile()
                        tampilkanFile(folderSaatIni!!)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        hapus.setOnClickListener {
            fileYangDipilih?.let {
                if (it.deleteRecursively()) {
                    Toast.makeText(this, "${it.name} dihapus", Toast.LENGTH_SHORT).show()
                    tampilkanFile(folderSaatIni!!)
                }
            }
            tekanLama.visibility = View.GONE
        }

        potong.setOnClickListener {
            fileYangDipilih?.let {
                modePotong = true
                paste.visibility = View.VISIBLE
            }
        }

        salin.setOnClickListener {
            fileYangDipilih?.let {
                modePotong = false
                paste.visibility = View.VISIBLE
            }
        }

        paste.setOnClickListener {
            fileYangDipilih?.let { file ->
                val targetFile = File(folderSaatIni, file.name)

                if (modePotong) {
                    if (file.renameTo(targetFile)) {
                        Toast.makeText(this, "Dipindahkan ke ${targetFile.path}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    file.copyTo(targetFile, overwrite = true)
                    Toast.makeText(this, "Disalin ke ${targetFile.path}", Toast.LENGTH_SHORT).show()
                }

                tampilkanFile(folderSaatIni!!)
            }

            paste.visibility = View.GONE
            tekanLama.visibility = View.GONE
        }
        
        findViewById<ImageView>(R.id.rename).setOnClickListener {
        fileYangDipilih?.let { file ->
        val editText = EditText(this).apply {
            setText(file.name)
        }

        AlertDialog.Builder(this)
            .setTitle("Ubah Nama")
            .setView(editText)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameFile(file, newName)
                } else {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    } ?: Toast.makeText(this, "Pilih file terlebih dahulu", Toast.LENGTH_SHORT).show()
}

    

findViewById<ImageView>(R.id.tfFile).setOnClickListener {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*" 
    }
    startActivityForResult(intent, REQUEST_CODE_FILE)
}

findViewById<ImageView>(R.id.tfFolder).setOnClickListener {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    startActivityForResult(intent, REQUEST_CODE_FOLDER)
}
    
    }

    inner class FileServer : NanoHTTPD(8080) {
    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/" -> serveHtml("index.html") 
            "/list-files" -> serveJsonFiles() 
            else -> serveFile(session.uri) 
        }
    }

    private fun serveHtml(fileName: String): Response {
        return try {
            val inputStream = assets.open(fileName) 
            val html = inputStream.bufferedReader().use { it.readText() }
            newFixedLengthResponse(Response.Status.OK, "text/html", html)
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File tidak ditemukan")
        }
    }

    private fun serveJsonFiles(): Response {
        val files = filesDir.listFiles()?.map { it.name } ?: emptyList()
        val json = """{"userCount": $userCount, "files": ${files.joinToString(",", "[", "]") { "\"$it\"" }}}"""
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun serveFile(uri: String): Response {
        val fileName = uri.removePrefix("/file/")
        val file = File(filesDir, fileName)
        return if (file.exists()) {
            newFixedLengthResponse(Response.Status.OK, "text/plain", file.readText())
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File tidak ditemukan")
        }
    }
}
    
    fun getPublicIp(): String {
    return try {
        URL("https://api64.ipify.org").readText()
    } catch (e: Exception) {
        "0.0.0.0"
    }
}

    private fun getLocalIpAddress(): String {
    return try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is InetAddress }?.hostAddress ?: "0.0.0.0" }
    catch (ex: Exception) {
        ex.printStackTrace()
        "0.0.0.0" }}

    private fun renameFile(file: File, newName: String) {
    val newFile = File(file.parent, newName)
    
    if (newFile.exists()) {
        Toast.makeText(this, "Nama sudah digunakan", Toast.LENGTH_SHORT).show()
    } else {
        if (file.renameTo(newFile)) {
            Toast.makeText(this, "Berhasil diubah", Toast.LENGTH_SHORT).show()
            tampilkanFile(file.parentFile ?: filesDir)
        } else {
            Toast.makeText(this, "Gagal mengubah nama", Toast.LENGTH_SHORT).show()
        }
    }
}

 private fun tampilkanFile(directory: File) {
    folderSaatIni = directory
    val grid = findViewById<GridLayout>(R.id.grid)
    grid.removeAllViews()

    directory.listFiles()?.let { files ->
        files.forEach { file ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_vertical, grid, false)
            val gambar = item.findViewById<ImageView>(R.id.gambar)
            val nama = item.findViewById<TextView>(R.id.nama)

            nama.text = file.name
            gambar.setImageResource(if (file.isDirectory) R.drawable.folder else R.drawable.file)

            item.setOnClickListener {
    if (file.isDirectory) {
        tampilkanFile(file)  
    } else {
        val intent = Intent(this, Editor::class.java).apply {
            putExtra("file_path", file.absolutePath)  
        }
        startActivity(intent)
    }
}

            item.setOnLongClickListener {
                showOptionsDialog(file)
                true
            }

            grid.addView(item)
        }
    } ?: Toast.makeText(this, "Folder kosong atau tidak dapat diakses", Toast.LENGTH_SHORT).show()
}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == RESULT_OK && data != null) {
        when (requestCode) {
            REQUEST_CODE_FILE -> {
                val uri = data.data
                uri?.let { copyFileToInternalStorage(it) }
            }

            REQUEST_CODE_FOLDER -> {
                val uri = data.data
                uri?.let { copyFolderToInternalStorage(it) }
            }
        }
    }
}

    private fun copyFileToInternalStorage(uri: Uri) {
    try {
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = uri.lastPathSegment?.substringAfterLast("/")
        val outputFile = File(filesDir, fileName ?: "file_terpilih")

        inputStream?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }}

        Toast.makeText(this, "File disalin: ${outputFile.name}", Toast.LENGTH_SHORT).show()
        
        tampilkanFile(filesDir)

    } catch (e: Exception) {
        Toast.makeText(this, "Gagal menyalin file", Toast.LENGTH_SHORT).show()
    }}
    
    private fun copyFolderToInternalStorage(uri: Uri) {
    Toast.makeText(this, "Folder dipilih: $uri", Toast.LENGTH_SHORT).show()}

private fun showOptionsDialog(file: File) {
    AlertDialog.Builder(this)
        .setTitle("Pilihan")
        .setItems(arrayOf("Hapus", "Rename")) { _, which ->
            when (which) {
                0 -> {
                    if (file.delete()) {
                        Toast.makeText(this, "File dihapus", Toast.LENGTH_SHORT).show()
                        tampilkanFile(file.parentFile ?: filesDir)}
                    else { Toast.makeText(this, "Gagal menghapus file", Toast.LENGTH_SHORT).show() } }
                1 -> showRenameDialog(file)
            }}
        .setNegativeButton("Batal", null)
        .show()}
    
    private fun showRenameDialog(file: File) {
    val editText = EditText(this).apply {
        setText(file.name) }

    AlertDialog.Builder(this)
        .setTitle("Ubah Nama")
        .setView(editText)
        .setPositiveButton("Simpan") { _, _ ->
            val newName = editText.text.toString().trim()
            if (newName.isNotEmpty()) {
                renameFile(file, newName)
            }
            else { Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show() }
        }
        .setNegativeButton("Batal", null)
        .show()
}
    
}
