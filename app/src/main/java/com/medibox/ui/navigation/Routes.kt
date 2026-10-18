package com.medibox.ui.navigation

object Routes {
    const val HOME = "home"
    const val MEDICINE_LIST = "medicine_list"
    const val SCAN = "scan"
    const val OCR_CONFIRM = "ocr_confirm"
    const val OCR_CONFIRM_WITH_CANDIDATES = "ocr_confirm?name={name}&specification={specification}&manufacturer={manufacturer}&batchNo={batchNo}&productionDate={productionDate}&expiryDate={expiryDate}"
    const val DETAIL = "detail"
    const val DETAIL_WITH_ID = "detail/{id}"
    const val EXPIRING = "expiring"
    const val INVENTORY = "inventory"
    const val PROFILE = "profile"
}