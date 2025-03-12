package gas.trulala.servergo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.File
import android.widget.*

class Editor : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editor)

        findViewById<TextView>(R.id.keluar).setOnClickListener { finish() }
        val tulis = findViewById<EditText>(R.id.tulis)
        val simpan = findViewById<ImageView>(R.id.simpan)

        val filePath = intent.getStringExtra("file_path")
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists()) {
                tulis.setText(file.readText()) 
            }
        }

        simpan.setOnClickListener {
            filePath?.let {
                val file = File(it)
                file.writeText(tulis.text.toString())
                Toast.makeText(this, "File disimpan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}