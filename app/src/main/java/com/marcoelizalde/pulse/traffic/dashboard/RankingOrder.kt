package com.marcoelizalde.pulse.traffic.dashboard

import java.io.File

/** What Top products ranks by. */
enum class RankingOrder(val key: String) {
    VISITORS("visitors"),
    SALES("sales");

    companion object {
        val DEFAULT = VISITORS

        /** Kept in [file] as its key. */
        fun store(file: File): SettingStore<RankingOrder> =
            FileSettingStore(file, encode = { it.key }, decode = { key -> entries.firstOrNull { it.key == key } })
    }
}
