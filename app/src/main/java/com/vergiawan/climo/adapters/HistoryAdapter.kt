package com.vergiawan.climo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vergiawan.climo.R
import com.vergiawan.climo.data.HistoryDataClass

class HistoryAdapter(
    private val context: Context,
    private val historyList: MutableList<HistoryDataClass>,
    private val onItemClickListener: OnItemClickListener?
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    // Menyimpan konteks aplikasi untuk akses resource.
    private val appContext = context.applicationContext

    /**
     * ViewHolder untuk menampung data riwayat.
     *
     * @param itemView View yang berisi elemen-elemen UI untuk menampilkan data riwayat.
     */
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val status: TextView = itemView.findViewById(R.id.tv_status_history)
        val dateTime: TextView = itemView.findViewById(R.id.date_time_history)
        val geoPoint: TextView = itemView.findViewById(R.id.geo_point_history)
    }

    /**
     * Membuat ViewHolder baru untuk menampilkan data riwayat.
     *
     * @param parent ViewGroup tempat ViewHolder baru akan ditambahkan.
     * @param viewType Tipe ViewHolder yang akan dibuat.
     * @return Instance baru dari HistoryViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.history_list_item, parent, false)
        return HistoryViewHolder(itemView)
    }

    /**
     * Mengembalikan jumlah item dalam daftar data riwayat.
     *
     * @return Jumlah item dalam historyList.
     */
    override fun getItemCount(): Int {
        return historyList.size
    }

    /**
     * Mengisi data ke ViewHolder pada posisi tertentu.
     *
     * @param holder ViewHolder yang akan diisi datanya.
     * @param position Posisi item dalam daftar data riwayat.
     */
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val currentItem = historyList[position] // Mendapatkan data riwayat pada posisi tertentu.

        // Mengatur teks pada TextView untuk menampilkan status, tanggal dan waktu, dan titik geografis.
        holder.status.text = currentItem.status
        holder.dateTime.text = currentItem.timeStamp
        holder.geoPoint.text = currentItem.geoPoint

        // Mendapatkan referensi ke RelativeLayout.
        val mainLayout = holder.itemView.findViewById<RelativeLayout>(R.id.main_layout)

        // Mendapatkan warna background berdasarkan status item.

        // Update: Mengganti baris kode yang depresiasi
        val newColor = when (currentItem.status) {
            "Severe Hypothermia" -> ContextCompat.getColor(appContext, R.color.emergency)
            "Moderate Hypothermia" -> ContextCompat.getColor(
                appContext,
                R.color.hypothermia_indicated
            )

            else -> ContextCompat.getColor(appContext, R.color.normal)
        }

        // Membuat drawable dengan warna yang diinginkan.
        val drawable = HistoryDrawableUtils.getDrawableWithColor(context, newColor)
        // Mengatur background RelativeLayout dengan drawable yang baru dibuat.
        mainLayout.background = drawable

        holder.itemView.setOnClickListener {
            val currentItem = historyList[position]
            onItemClickListener?.onItemClick(
                position,
                currentItem.status!!,
                currentItem.geoPoint!!
            )
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int, status: String, geoPoint: String)
    }

}