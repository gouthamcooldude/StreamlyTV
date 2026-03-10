package com.streamlytv.worker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager

object ScanStatusManager {

    fun getScanWorkInfo(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(MetadataScanWorker.TAG)
    }

    fun isScanning(workInfos: List<WorkInfo>): Boolean {
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }

    fun lastScanResult(workInfos: List<WorkInfo>): ScanSummary? {
        val last = workInfos.lastOrNull { it.state == WorkInfo.State.SUCCEEDED } ?: return null
        return ScanSummary(
            itemsScanned = last.outputData.getInt(MetadataScanWorker.KEY_ITEMS_SCANNED, 0),
            found51 = last.outputData.getInt(MetadataScanWorker.KEY_ITEMS_FOUND_51, 0),
            found4K = last.outputData.getInt(MetadataScanWorker.KEY_ITEMS_FOUND_4K, 0)
        )
    }

    data class ScanSummary(
        val itemsScanned: Int,
        val found51: Int,
        val found4K: Int
    ) {
        override fun toString(): String {
            val parts = mutableListOf("Scanned $itemsScanned items")
            if (found4K > 0) parts.add("$found4K in 4K")
            if (found51 > 0) parts.add("$found51 with 5.1")
            return parts.joinToString(" · ")
        }
    }
}
