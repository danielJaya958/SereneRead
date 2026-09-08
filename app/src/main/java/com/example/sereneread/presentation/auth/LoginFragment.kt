package com.example.sereneread.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sereneread.data.remote.network.TokenManager
import com.example.sereneread.databinding.FragmentLoginBinding
import com.example.sereneread.domain.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // 1. Suntikkan TokenManager
    @Inject
    lateinit var tokenManager: TokenManager

    // Suntikkan ViewModel
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. CEK SESI: Jika token ada, langsung gas ke Home!
        if (!tokenManager.getToken().isNullOrEmpty()) {
            findNavController().navigate(com.example.sereneread.R.id.homeFragment)
            return // Hentikan proses di bawahnya agar tidak render UI login
        }

        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // Panggil fungsi login di ViewModel
            viewModel.login(email, password)
        }

        binding.tvGoToRegister.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    // Tampilkan indikator loading & matikan tombol
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = "Loading..."
                }
                is Resource.Success -> {
                    // KUNCI: Jangan aktifkan tombol lagi! Biarkan mati agar tidak bisa diklik dobel.
                    binding.btnLogin.text = "Memasuki aplikasi..."

                    Toast.makeText(requireContext(), "Login Berhasil!", Toast.LENGTH_SHORT).show()

                    // SABUK PENGAMAN + NAVIGASI LANGSUNG KE FRAGMENT
                    val currentDestination = findNavController().currentDestination?.id
                    if (currentDestination == com.example.sereneread.R.id.loginFragment) {
                        // Tembak langsung ke ID Home (R.id.homeFragment) alih-alih menggunakan Action
                        findNavController().navigate(com.example.sereneread.R.id.homeFragment)
                    }
                }
                is Resource.Error -> {
                    // Kalau error, BARU kita nyalakan lagi tombolnya supaya user bisa coba lagi
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Log In"

                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}