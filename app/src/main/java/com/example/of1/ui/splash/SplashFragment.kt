package com.example.of1.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.of1.R
import com.example.of1.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSplashBinding.inflate(inflater, container, false)

        binding.lottieAnimationView.repeatCount = 0

        // Navigate to main fragment after 1200ms
        Handler(Looper.getMainLooper()).postDelayed({
            findNavController().navigate(R.id.action_splashFrag_to_mainFrag)
        }, 3500)

        return binding.root
    }
}