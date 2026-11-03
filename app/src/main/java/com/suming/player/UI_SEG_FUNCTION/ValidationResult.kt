package com.suming.player.UI_SEG_FUNCTION

sealed interface ValidationResult {

    data object Ok : ValidationResult
    data class Reject(val message: String) : ValidationResult

}