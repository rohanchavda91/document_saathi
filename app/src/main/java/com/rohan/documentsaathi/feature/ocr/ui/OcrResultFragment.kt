package com.rohan.documentsaathi.feature.ocr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.rohan.documentsaathi.core.ai.SummarizationManager
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.data.repository.DocumentRepository
import com.rohan.documentsaathi.R
import com.rohan.documentsaathi.databinding.FragmentOcrResultBinding
import com.rohan.documentsaathi.feature.ocr.ui.OcrResultViewModel
import com.rohan.documentsaathi.feature.ocr.ui.OcrResultUiState
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OcrResultFragment : Fragment(){

    private var _binding: FragmentOcrResultBinding?=null
    private val binding get() = _binding!!

    private val args: OcrResultFragmentArgs by navArgs()
    private val viewModel: OcrResultViewModel by viewModels()

    @Inject
    lateinit var summarizationManager: SummarizationManager

    private var currentDocumentId: Long = -1L
    private var currentSummaryLanguage : String = "ENGLISH"

    @Inject
    lateinit var documentRepository: DocumentRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentOcrResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val extractedText = args.extractedText

        saveDocumentToDatabase(extractedText)

//        Display extracted text
        binding.tvExtractedText.text = extractedText

//        Copy to clipboard button
        binding.btnCopy.setOnClickListener {
            copyToClipboard(extractedText)
        }

//        Share button
        binding.btnShare.setOnClickListener {
            shareText(extractedText)
        }

//        Back to home Button
        binding.btnBackHome.setOnClickListener{
            findNavController().navigate(R.id.action_ocr_result_to_home)
        }

//        Summarize Button click listener
        binding.btnSummarize.setOnClickListener {
            val extractedText = args.extractedText
            val selectedLanguage = viewModel.summaryLanguages.value[binding.spinnerSummaryLanguage.selectedItemPosition].code
            
            // Navigate to the new Summarization Result screen instead of in-place summary
            val action = OcrResultFragmentDirections.actionOcrResultToSummarization(
                extractedText = extractedText,
                selectedLanguage = selectedLanguage,
                documentImagePath = args.documentImagePath
            )
            findNavController().navigate(action)
        }

//        Observe UI State Changes
        observeUiState()
        observeLanguages()
    }

    private fun observeLanguages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summaryLanguages.collect { languages ->
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    languages.map { it.displayName }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerSummaryLanguage.adapter = adapter
            }
        }
    }

    private fun saveDocumentToDatabase(extractedText: String){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val document = Document(
                    extractedText = extractedText,
                    detectedLanguage = "en"
//                    TODO: Determine language from OcrManager
                )
                val documentId = documentRepository.saveDocument(document)
                Toast.makeText(
                    requireContext(),
                    "Document saved successfully",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } catch (e: Exception){
                Toast.makeText(
                    requireContext(),
                    "Error saving document: ${e.message}",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun observeUiState(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect {
                state->
                when(state){
                    is OcrResultUiState.Loading -> {
//                        show loading animation while document is saving
                    }
                    is OcrResultUiState.DocumentSaved ->{
                        currentDocumentId = state.documentId
                        Toast.makeText(
                            requireContext(),
                            "Document is saved",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    is OcrResultUiState.SummarizationSuccess ->{
                        binding.progressSummarization.visibility = View.GONE
                        binding.btnSummarize.isEnabled = true
                        binding.tvSummary.text = state.summary
                        binding.tvSummaryLanguage.text = "Language: ${state.summaryLanguage}"
                        binding.tvSummaryLanguage.visibility = View.VISIBLE

//                        Update Document with Summary
                        if(currentDocumentId != -1L){
                            viewModel.updateDocumentWithSummary(
                                currentDocumentId,
                                state.summary,
                                state.summaryLanguage
                            )
                        }

                        Toast.makeText(
                            requireContext(),
                            "Summary generated in ${state.summaryLanguage}",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    is OcrResultUiState.SummarizationLoading -> {
                        binding.progressSummarization.visibility = View.VISIBLE
                        binding.btnSummarize.isEnabled = false
                    }
                    is OcrResultUiState.Error -> {
                        binding.progressSummarization.visibility = View.GONE
                        binding.btnSummarize.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            "Error: ${state.message}",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                    else -> {
//                        blank
                    }
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("extracted_text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(text: String){
        val shareIntent = Intent().apply{
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type="text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share extracted text"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}