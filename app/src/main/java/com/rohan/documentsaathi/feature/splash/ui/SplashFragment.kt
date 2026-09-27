package com.rohan.documentsaathi.feature.splash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rohan.documentsaathi.R
import com.rohan.documentsaathi.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUnlock.setOnClickListener {
            checkAndAuthenticate()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            checkAndAuthenticate()
        }
    }

    private fun checkAndAuthenticate() {
        val biometricManager = BiometricManager.from(requireContext())
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                promptBiometricAuthentication(authenticators)
            }
            else -> {
                // If no lockscreen/biometric security is enrolled on device, proceed directly
                navigateToHome()
            }
        }
    }

    private fun promptBiometricAuthentication(authenticators: Int) {
        binding.progressIndicator.visibility = View.VISIBLE
        binding.btnUnlock.visibility = View.GONE

        val executor = ContextCompat.getMainExecutor(requireContext())
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                navigateToHome()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                binding.progressIndicator.visibility = View.GONE
                binding.btnUnlock.visibility = View.VISIBLE
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(requireContext(), errString, Toast.LENGTH_SHORT).show()
                }
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_app_title))
            .setSubtitle(getString(R.string.unlock_app_subtitle))
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToHome() {
        try {
            findNavController().navigate(R.id.action_splash_to_home)
        } catch (_: Exception) {
            // Already navigated
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
