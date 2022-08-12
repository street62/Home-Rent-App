package com.example.home_rent_app.ui.loginprofile

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import coil.load
import com.example.home_rent_app.R
import com.example.home_rent_app.databinding.FragmentLoginProfileBinding

class LoginProfileFragment : Fragment() {

    lateinit var binding: FragmentLoginProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_login_profile, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivLoginProfile.setOnClickListener {
            if (isAllPermissionGranted()) {
                selectGallery()
            } else {
                requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
            }
        }
    }

    private fun selectGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        // intent 의 data 와 type 을 동시에 설정하는 메서드
        intent.setDataAndType(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "image/*"
        )
        imageResult.launch(intent)
    }

    private val imageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                imageUri?.let {
                    binding.ivLoginProfile.load(it)
                }
            }
        }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(activity as Activity, REQUIRED_PERMISSIONS, REQ_GALLERY)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val deniedList = result.filter {
                !it.value
            }.map {
                it.key
            }

            when {
                deniedList.isNotEmpty() -> {
                    val map = deniedList.groupBy { permission ->
                        if (shouldShowRequestPermissionRationale(permission)) {
                            "DENIED"
                        } else {
                            "EXPLAINED"
                        }
                    }
                    map["DENIED"]?.let {
                        // 한번 거절했을 경우 재요청
                        Toast.makeText(
                            requireContext(),
                            "앨범에 접근하려면 권한이 필요합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        requestPermissions()
                    }
                    map["EXPLAINED"]?.let {
                        // 두번 거절했을 경우
                        Toast.makeText(
                            requireContext(),
                            "설정에서 미디어 접근 권한을 허용해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    // 권한 중 하나라도 허용되지 않았다면 false 를 반환하는 함수
    private fun isAllPermissionGranted(): Boolean = REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQ_GALLERY = 1
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
