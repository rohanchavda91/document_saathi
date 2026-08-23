package com.rohan.documentsaathi.feature.home.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rohan.documentsaathi.R
import com.rohan.documentsaathi.data.db.entity.Document
import com.rohan.documentsaathi.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var documentAdapter: DocumentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

//        Navigate to scanner
        binding.fabScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanner)
        }
    }

    private fun setupRecyclerView() {
        documentAdapter = DocumentAdapter(
            documents = emptyList(),
            onItemClick = { document ->
                val action = HomeFragmentDirections.actionHomeToDocumentDetail(document.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { document ->
                showDeleteConfirmation(document)
            }
        )
        binding.documentsRecyclerView.apply {
            adapter = documentAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showDeleteConfirmation(document: Document) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Document?")
            .setMessage("Are you sure you want to delete this document? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDocument(document)
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.documents.collect { documents ->
                    documentAdapter.updateDocuments(documents)
                    binding.emptyStateLayout.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state is HomeUiState.Loading) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun onDestroyView(){
        super.onDestroyView()
        _binding=null
    }
}