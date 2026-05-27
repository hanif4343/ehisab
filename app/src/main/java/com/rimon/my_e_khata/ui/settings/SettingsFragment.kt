package com.rimon.my_e_khata.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rimon.my_e_khata.databinding.FragmentSettingsBinding
import com.rimon.my_e_khata.utils.AppPreferences
import com.rimon.my_e_khata.utils.FormatUtils

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = AppPreferences.getInstance(requireContext())
        binding.tvBusinessName.text = prefs.businessName
        binding.tvLastBackup.text = if (prefs.lastBackupTime > 0)
            "Last backup: ${FormatUtils.formatDateShort(prefs.lastBackupTime)}"
        else "No backup yet"

        binding.btnOpenSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = AppPreferences.getInstance(requireContext())
        binding.tvBusinessName.text = prefs.businessName
        binding.tvLastBackup.text = if (prefs.lastBackupTime > 0)
            "Last backup: ${FormatUtils.formatDateShort(prefs.lastBackupTime)}"
        else "No backup yet"
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
