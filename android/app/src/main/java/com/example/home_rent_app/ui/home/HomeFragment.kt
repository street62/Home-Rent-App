package com.example.home_rent_app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.home_rent_app.databinding.FragmentHomeBinding
import com.example.home_rent_app.ui.HomeActivity

class HomeFragment : Fragment() {

    private val binding: FragmentHomeBinding by lazy {
        FragmentHomeBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        goWantHomeActivity()
    }

    private fun goWantHomeActivity() {
        binding.tempButton.setOnClickListener {
            val activity = activity as HomeActivity
            activity.goWantHomeActivity()
        }
    }
}
