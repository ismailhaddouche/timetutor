package com.haddouche.timetutor.ui.alumno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.haddouche.timetutor.R


import android.widget.RadioButton
import android.widget.RadioGroup
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context

class ConfiguracionAlumnoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_configuracion_alumno, container, false)
        // --- Selector de tema claro/oscuro ---
        val radioGroupTema = view.findViewById<RadioGroup>(R.id.radioGroupTema)
        val radioClaro = view.findViewById<RadioButton>(R.id.radioClaro)
        val radioOscuro = view.findViewById<RadioButton>(R.id.radioOscuro)
        val radioSistema = view.findViewById<RadioButton>(R.id.radioSistema)
        val prefs = requireContext().getSharedPreferences("tema_app", Context.MODE_PRIVATE)
        when (prefs.getString("tema", "sistema")) {
            "claro" -> radioClaro.isChecked = true
            "oscuro" -> radioOscuro.isChecked = true
            else -> radioSistema.isChecked = true
        }
        radioGroupTema.setOnCheckedChangeListener { _, checkedId ->
            val editor = prefs.edit()
            when (checkedId) {
                R.id.radioClaro -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    editor.putString("tema", "claro")
                }
                R.id.radioOscuro -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    editor.putString("tema", "oscuro")
                }
                R.id.radioSistema -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    editor.putString("tema", "sistema")
                }
            }
            editor.apply()
            requireActivity().recreate()
        }
        return view
    }
}
