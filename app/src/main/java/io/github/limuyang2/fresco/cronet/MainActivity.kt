package io.github.limuyang2.fresco.cronet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.limuyang2.fresco.cronet.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var  viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)



        viewBinding.btnLoad.setOnClickListener {

            viewBinding.draweeView.setImageURI("https://pic.rmb.bdstatic.com/bjh/914b8c0f9814b14c5fedeec7ec6615df5813.jpeg")

        }
    }
}