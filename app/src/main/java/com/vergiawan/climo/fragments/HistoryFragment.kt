package com.vergiawan.climo.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vergiawan.climo.R
import com.vergiawan.climo.adapters.HistoryAdapter
import com.vergiawan.climo.data.HistoryDataClass
import com.vergiawan.climo.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(), HistoryAdapter.OnItemClickListener {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var popupMenu: PopupMenu
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyArrayList: ArrayList<HistoryDataClass>

    private lateinit var firestore: FirebaseFirestore
    private lateinit var emptyStateStub: ViewStub
    private lateinit var alertDialog: MaterialAlertDialogBuilder
    private lateinit var idSelector: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        idSelector = arguments?.getString("ID_SELECTOR").toString()

        popupMenu = PopupMenu(requireContext(), binding.popMenuIcon)
        popupMenu.inflate(R.menu.popup_menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.hapus_riwayat -> deleteAllHistoryData()
            }
            true
        }
        binding.popMenuIcon.setOnClickListener {
            popupMenu.show()
        }

        emptyStateStub = binding.emptyState

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        historyRecyclerView = binding.recycleView
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyRecyclerView.setHasFixedSize(false)
        historyArrayList = arrayListOf()

        getHistoryData()

        binding.swiperefresh.apply {
            setOnRefreshListener {
                val fragmentManager = requireActivity().supportFragmentManager
                val bundle = Bundle().apply {
                    putString("ID_SELECTOR", idSelector)
                }
                val fragment = HistoryFragment().apply {
                    arguments = bundle
                }
                fragmentManager.beginTransaction()
                    .replace(R.id.map_layout, fragment)
                    .commit()
                isRefreshing = false
            }
        }
    }

    private fun getHistoryData() {
        val collectionRef = firestore.collection(idSelector)
            .orderBy("timeStamp", Query.Direction.DESCENDING)

        viewLifecycleOwner.lifecycleScope.launch {
            collectionRef.get().addOnSuccessListener { querySnapshot ->
                if (!isAdded || view == null || context == null) return@addOnSuccessListener

                if (querySnapshot.isEmpty) {
                    try {
                        emptyStateStub.inflate()
                    } catch (e: IllegalStateException) {
                        // Do nothing if already inflated
                    }
                    return@addOnSuccessListener
                }

                val historyArrayList = mutableListOf<HistoryDataClass>()
                for (document in querySnapshot.documents) {
                    val historyData = document.toObject(HistoryDataClass::class.java)
                    if (historyData != null) {
                        Log.i("History Data", "getHistoryData: $historyData")
                        historyArrayList.add(historyData)
                    }
                }

                context?.let {
                    historyRecyclerView.adapter =
                        HistoryAdapter(it, historyArrayList, this@HistoryFragment)
                }
            }
        }
    }

    private fun deleteAllHistoryData() {
        val collectionRef = firestore.collection(idSelector)
        viewLifecycleOwner.lifecycleScope.launch {
            collectionRef.get().addOnSuccessListener { querySnapshot ->
                if (!isAdded || view == null) return@addOnSuccessListener

                if (querySnapshot.isEmpty) return@addOnSuccessListener
                for (document in querySnapshot) {
                    collectionRef.document(document.id).delete()
                }

                val fragmentManager = requireActivity().supportFragmentManager
                val bundle = Bundle().apply {
                    putString("ID_SELECTOR", idSelector)
                }
                val newFragment = HistoryFragment().apply {
                    arguments = bundle
                }
                fragmentManager.beginTransaction()
                    .replace(R.id.map_layout, newFragment)
                    .commit()

                Toast.makeText(requireContext(), "Data riwayat dihapus", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onItemClick(position: Int, status: String, geoPoint: String) {
        val coordinates = geoPoint.split(",")
        val latitude = coordinates[0].trim().toDoubleOrNull()
        val longitude = coordinates[1].trim().toDoubleOrNull()

        if (latitude == null || longitude == null) {
            Toast.makeText(requireContext(), "Koordinat tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Buka Google Maps?")
            .setMessage("Aplikasi ingin membuka Google Maps dengan koordinat $latitude,$longitude. Apakah Anda ingin melanjutkan?")
            .setPositiveButton("Ya") { dialog, _ ->
                val uri = "geo:0,0?q=$latitude,$longitude"
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                    setPackage("com.google.android.apps.maps")
                }
                startActivity(mapIntent)
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
        alertDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
